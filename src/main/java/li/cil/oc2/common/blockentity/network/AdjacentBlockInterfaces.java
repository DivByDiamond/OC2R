package li.cil.oc2.common.blockentity.network;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.world.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;

class AdjacentBlockInterfaces {
    private static final int TUNNEL_INDEX = 0;

    private final NetworkInterface[] interfaces =
            new NetworkInterface[Constants.BLOCK_FACE_COUNT + 1];
    private boolean haveChanged = true;

    @SuppressWarnings("FieldCanBeLocal")
    private final ICapabilityInvalidationListener listener =
            () -> {
                haveChanged = true;
                return true;
            };

    private final BlockEntity blockEntity;

    AdjacentBlockInterfaces(final BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    void handleNeighborChanged() {
        haveChanged = true;
    }

    void setTunnelInterface(@Nullable final NetworkInterface tunnelInterface) {
        interfaces[TUNNEL_INDEX] = tunnelInterface;
    }

    @Nullable
    NetworkInterface getTunnelInterface() {
        return interfaces[TUNNEL_INDEX];
    }

    void registerListeners(final ServerLevel level, final BlockPos pos) {
        for (final Direction side : Constants.DIRECTIONS) {
            level.registerCapabilityListener(pos.relative(side), listener);
        }
        haveChanged = true;
    }

    Stream<NetworkInterface> getAll() {
        validate();
        return Arrays.stream(interfaces).filter(Objects::nonNull);
    }

    private void validate() {
        if (blockEntity.isRemoved() || !haveChanged) {
            return;
        }

        final Level level = blockEntity.getLevel();
        for (final Direction side : Constants.DIRECTIONS) {
            interfaces[side.get3DDataValue() + 1] = null;
        }

        haveChanged = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = blockEntity.getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(side);
            final BlockEntity neighbor = LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
            if (neighbor != null) {
                final NetworkInterface iface =
                        level.getCapability(
                                Capabilities.NetworkInterface.BLOCK,
                                neighborPos,
                                null,
                                neighbor,
                                side.getOpposite());
                if (iface != null) {
                    interfaces[side.get3DDataValue() + 1] = iface;
                }
            }
        }
    }
}