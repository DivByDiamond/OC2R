package li.cil.oc2.common.blockentity.network.hub;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.world.level.LevelUtils;
import li.cil.oc2.platform.CapabilityInvalidationListener;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class NetworkHubBlockEntity extends ModBlockEntity implements NetworkInterface {
    private static final int TTL_COST = 1;

    private int frameCount;
    private long lastGameTime;

    @SuppressWarnings("FieldCanBeLocal")
    private final CapabilityInvalidationListener neighborListener =
            new CapabilityInvalidationListener(this::handleNeighborChanged);

    private final NetworkInterface[] adjacentBlockInterfaces =
            new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private boolean haveAdjacentBlocksChanged = true;

    public NetworkHubBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_HUB.get(), pos, state);
    }

    private static final byte[] NO_FRAME = new byte[0];

    @Override
    public byte[] readEthernetFrame() {
        return NO_FRAME;
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, final byte[] frame, final int timeToLive) {
        if (level == null) {
            return;
        }

        // Give a cap on top of the TLL, just in case trolls intentionally build
        // loops that exponentially multiply ethernet frames after people crank up
        // the default TTL.
        final long gameTime = level.getGameTime();
        if (gameTime > lastGameTime) {
            lastGameTime = gameTime;
            frameCount = 1;
        } else if (frameCount > Config.hubEthernetFramesPerTick) {
            return;
        } else {
            frameCount++;
        }

        getAdjacentInterfaces()
                .forEach(
                        adjacentInterface -> {
                            if (!adjacentInterface.equals(source)) {
                                adjacentInterface.writeEthernetFrame(
                                        this, frame, timeToLive - TTL_COST);
                            }
                        });
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final NetworkHubBlockEntity self) {
                        return self;
                    }
                    return null;
                },
                Blocks.NETWORK_HUB.get());
    }

    private Stream<NetworkInterface> getAdjacentInterfaces() {
        validateAdjacentBlocks();
        return Arrays.stream(adjacentBlockInterfaces).filter(Objects::nonNull);
    }

    private void validateAdjacentBlocks() {
        if (!isValid() || !haveAdjacentBlocksChanged) {
            return;
        }

        for (final Direction side : Constants.DIRECTIONS) {
            adjacentBlockInterfaces[side.get3DDataValue()] = null;
        }

        haveAdjacentBlocksChanged = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            final var neighborPos = pos.relative(side);
            final BlockEntity neighborBlockEntity =
                    LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
            if (neighborBlockEntity != null) {
                final NetworkInterface adjacentInterface =
                        Platform.capabilities()
                                .getBlockCapability(
                                        Capabilities.NetworkInterface.BLOCK,
                                        level,
                                        neighborPos,
                                        null,
                                        neighborBlockEntity,
                                        side.getOpposite());
                if (adjacentInterface != null) {
                    adjacentBlockInterfaces[side.get3DDataValue()] = adjacentInterface;
                }
            }
        }
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        final ServerLevel level = (ServerLevel) this.level;
        final BlockPos pos = getBlockPos();
        for (var side : Constants.DIRECTIONS) {
            final var neighborPos = pos.relative(side);
            Platform.capabilities()
                    .registerBlockCapabilityListener(level, neighborPos, neighborListener);
        }

        haveAdjacentBlocksChanged = true;
    }

    public boolean handleNeighborChanged() {
        haveAdjacentBlocksChanged = true;
        return true;
    }
}