package li.cil.oc2.common.blockentity.network.vxlan;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
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

@EventBusSubscriber(modid = API.MOD_ID)
public final class VxlanBlockEntity extends ModBlockEntity
        implements NetworkInterface, TickableBlockEntity {

    private final ReentrantLock lock = new ReentrantLock();

    private static final int TTL_COST = 1;
    private int vti = 1000;
    private int frameCount;
    private long lastGameTime;

    private final Queue<byte[]> packetQueue = new ArrayBlockingQueue<>(32);

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
            lock.lock();
            try {

                packetQueue.forEach(packet -> writeEthernetFrame(tunnelInterface, packet, 255));
                packetQueue.clear();
            
            } finally {
                lock.unlock();
            }
        } else {
            System.out.printf("VXLAN block is unregistered upstream: VTI=%d\n", vti);
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (level != null && !level.isClientSide() && tag.contains("vti")) {
            vti = tag.getInt("vti");
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
            TunnelManager.instance().unregisterVti(vti);
        }
        super.onUnload(isRemove);
    }

    @Override
    public void loadServer() {
        adjacentInterfaces.setTunnelInterface(
                TunnelManager.instance().registerVti(vti, packetQueue));
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