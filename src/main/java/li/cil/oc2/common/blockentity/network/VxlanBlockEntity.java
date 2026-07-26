package li.cil.oc2.common.blockentity.network;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;

import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.vxlan.TunnelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

@EventBusSubscriber(modid = API.MOD_ID)
public final class VxlanBlockEntity extends ModBlockEntity implements NetworkInterface, TickableBlockEntity {
    private static final int TTL_COST = 1;
    //private int vti = ((int) (Math.random() * Integer.MAX_VALUE)) & 0x00ff_ffff;
    private int vti = 1000;
    private int frameCount;
    private long lastGameTime;

    private final Queue<byte[]> packetQueue = new ArrayBlockingQueue<>(32);

    ///////////////////////////////////////////////////////////////////

    // Each face and the default TunnelInterface connecting to the outernet
    private final NetworkInterface[] adjacentBlockInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT + 1];
    private boolean haveAdjacentBlocksChanged = true;

    // NeoForge will only hold a weak reference to this listener (so that registering a listener cause a memory leak)
    // Therefore we must hold the reference to keep it from being garbage collected while we're still around
    @SuppressWarnings("FieldCanBeLocal")
    private final ICapabilityInvalidationListener adjacentBlockListener = () -> { this.haveAdjacentBlocksChanged = true; return true; };

    ///////////////////////////////////////////////////////////////////

    public VxlanBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.VXLAN_HUB.get(), pos, state);
    }


    ///////////////////////////////////////////////////////////////////

    public void handleNeighborChanged() {
        haveAdjacentBlocksChanged = true;
    }

    @Override
    @Nullable
    public byte[] readEthernetFrame() {
        return null;
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
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

        getAdjacentInterfaces().forEach(adjacentInterface -> {
            if (adjacentInterface != source) {
                adjacentInterface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            }
        });
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }

        if (adjacentBlockInterfaces[0] != null) {
            // CircularFifoQueue isn't thread-safe, so we have to synchronize on it.
            synchronized (packetQueue) {
                packetQueue.forEach(packet -> writeEthernetFrame(adjacentBlockInterfaces[0], packet, 255));
                packetQueue.clear();
            }
        } else {
            System.out.printf("VXLAN block is unregistered upstream: VTI=%d\n", vti);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)  {
        super.loadAdditional(tag, registries);
        if (level != null && !level.isClientSide() && tag.contains("vti")) {
            vti = tag.getInt("vti");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)  {
        super.saveAdditional(tag, registries);
        if (level != null && !level.isClientSide()) {
            tag.putInt("vti", vti);
        }
    }

    @Override
    protected void onUnload(final boolean isRemove) {
        if (level != null && !level.isClientSide()) {
            adjacentBlockInterfaces[0] = null;
            TunnelManager.instance().unregisterVti(vti);
        }

        super.onUnload(isRemove);
    }

    @Override
    public void loadServer() {
        adjacentBlockInterfaces[0] = TunnelManager.instance().registerVti(vti, this.packetQueue);
        var level = (ServerLevel) this.level;
        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(side);
            level.registerCapabilityListener(neighborPos, this.adjacentBlockListener);
        }
        haveAdjacentBlocksChanged = true;
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.NetworkInterface.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final VxlanBlockEntity self) {
                    return self;
                }
                return null;
            },
            Blocks.VXLAN_HUB.get()
        );
    }

    ///////////////////////////////////////////////////////////////////

    private Stream<NetworkInterface> getAdjacentInterfaces() {
        validateAdjacentBlocks();
        return Arrays.stream(adjacentBlockInterfaces).filter(Objects::nonNull);
    }

    private void validateAdjacentBlocks() {
        if (isRemoved() || !haveAdjacentBlocksChanged) {
            return;
        }

        for (final Direction side : Constants.DIRECTIONS) {
            adjacentBlockInterfaces[side.get3DDataValue() + 1] = null;
        }

        haveAdjacentBlocksChanged = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(side);
            final BlockEntity neighborBlockEntity = LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
            if (neighborBlockEntity != null) {
                final NetworkInterface adjacentInterface = level.getCapability(Capabilities.NetworkInterface.BLOCK, neighborPos, null, neighborBlockEntity, side.getOpposite());
                if (adjacentInterface != null) {
                    adjacentBlockInterfaces[side.get3DDataValue() + 1] = adjacentInterface;
                }
            }
        }
    }
}
