package li.cil.oc2.common.blockentity.network.vxlan;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.vxlan.TunnelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A network hub that bridges the local block-entity bus to a VXLAN tunnel.
 *
 * <p>Outbound frames written by adjacent devices are flooded to all neighboring
 * interfaces (except the origin), including the tunnel interface registered with
 * {@link TunnelManager}, which encapsulates and sends them over UDP. Inbound
 * frames arrive asynchronously on the tunnel socket thread into
 * {@link #packetQueue} and are injected into the local network during
 * {@link #serverTick()}, once per game tick.
 */
@EventBusSubscriber(modid = API.MOD_ID)
public final class VxlanBlockEntity extends ModBlockEntity
        implements NetworkInterface, TickableBlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Per-hop cost subtracted from the time-to-live when flooding frames to neighbors. */
    private static final int TTL_COST = 1;

    /** The VXLAN header carries a 24-bit VNI; identifiers must stay inside that range. */
    private static final int VNI_LIMIT = 1 << 24;

    private static final Random VTI_RANDOM = new Random();

    /**
     * Virtual tunnel identifier; selects the inbound VXLAN frames addressed to this hub.
     * Randomized per placed hub instead of a shared constant so two hubs cannot silently
     * steal each other's tunnel registration ({@code tunnels} maps VNI to one interface);
     * values restored from NBT are range-validated (todo.md §39 С2).
     */
    private int vti = VTI_RANDOM.nextInt(VNI_LIMIT);
    private int frameCount;
    private long lastGameTime;

    /**
     * Inbound frames delivered by the tunnel socket thread; capacity is configurable
     * ({@code vxlanPacketQueueCapacity}) and frames beyond it are dropped by the producer.
     * {@link ArrayBlockingQueue} is thread-safe, so no external lock is needed (todo.md §38 Ш5).
     */
    private final Queue<byte[]> packetQueue =
            new ArrayBlockingQueue<>(Config.vxlanPacketQueueCapacity);

    private final AdjacentBlockInterfaces adjacentInterfaces = new AdjacentBlockInterfaces(this);

    public VxlanBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.VXLAN_HUB.get(), pos, state);
    }

    public void handleNeighborChanged() {
        adjacentInterfaces.handleNeighborChanged();
    }

    private static final byte[] NO_FRAME = new byte[0];

    @Override
    @Nullable
    public byte[] readEthernetFrame() {
        return NO_FRAME;
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, final byte[] frame, final int timeToLive) {
        if (level == null) {
            return;
        }

        // Per-tick flood limiter: only the first hubEthernetFramesPerTick frames of a
        // tick are forwarded, so a flooded external network cannot stall the server.
        final long gameTime = level.getGameTime();
        if (gameTime > lastGameTime) {
            lastGameTime = gameTime;
            frameCount = 1;
        } else if (frameCount > Config.hubEthernetFramesPerTick) {
            return;
        } else {
            frameCount++;
        }

        adjacentInterfaces
                .getAll()
                .forEach(
                        adjacentInterface -> {
                            if (!adjacentInterface.equals(source)) {
                                adjacentInterface.writeEthernetFrame(
                                        this, frame, timeToLive - TTL_COST);
                            }
                        });
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }

        final NetworkInterface tunnelInterface = adjacentInterfaces.getTunnelInterface();
        if (tunnelInterface != null) {
            // Drain frames received over the tunnel since the last tick and inject them
            // into the local network, flooding to every neighbor except the tunnel itself.
            // poll-drain instead of forEach+clear: a frame arriving mid-drain would
            // otherwise be cleared without ever being processed.
            byte[] packet;
            while ((packet = packetQueue.poll()) != null) {
                writeEthernetFrame(tunnelInterface, packet, 255);
            }
            if (tunnelInterface instanceof TunnelManager.TunnelInterface tunnel
                    && tunnel.droppedFrames.get() > 0) {
                LOGGER.warn(
                        "VXLAN hub dropped {} inbound frames (queue capacity {}, consider"
                                + " raising vxlanPacketQueueCapacity)",
                        tunnel.droppedFrames.getAndSet(0),
                        Config.vxlanPacketQueueCapacity);
            }
        } else {
            LOGGER.warn("VXLAN block is unregistered upstream: VTI={}", vti);
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (level != null && !level.isClientSide() && tag.contains("vti")) {
            final int loaded = tag.getInt("vti");
            // NBT is player-writable (creative pick-block exports etc.): an out-of-range
            // or hostile value falls back to a fresh random identifier.
            vti = loaded >= 0 && loaded < VNI_LIMIT ? loaded : VTI_RANDOM.nextInt(VNI_LIMIT);
        }
    }

    @Override
    public void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (level != null && !level.isClientSide()) {
            tag.putInt("vti", vti);
        }
    }

    @Override
    protected void onUnload(final boolean isRemove) {
        if (level != null && !level.isClientSide()) {
            adjacentInterfaces.setTunnelInterface(null);
            final TunnelManager manager = TunnelManager.instance();
            // The manager is gone after ServerStopping; unloading chunks then must not NPE.
            if (manager != null) {
                manager.unregisterVti(vti);
            }
        }
        super.onUnload(isRemove);
    }

    @Override
    public void loadServer() {
        final TunnelManager manager = TunnelManager.instance();
        if (manager != null) {
            adjacentInterfaces.setTunnelInterface(manager.registerVti(vti, packetQueue));
        } else {
            LOGGER.warn("VXLAN tunnel manager unavailable: VTI={} stays unregistered", vti);
        }
        final ServerLevel level = (ServerLevel) this.level;
        adjacentInterfaces.registerListeners(level, getBlockPos());
    }

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final VxlanBlockEntity self) {
                        return self;
                    }
                    return null;
                },
                Blocks.VXLAN_HUB.get());
    }
}