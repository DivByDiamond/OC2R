package li.cil.oc2.client.model;

import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Loader independent state queries shared by the NeoForge and Fabric bus cable models. */
public final class BusCableModelUtils {
    private BusCableModelUtils() {}

    public static boolean isNeighborInDirectionSolid(
            final BlockAndTintGetter level, final BlockPos pos, final Direction direction) {
        final BlockPos neighborPos = pos.relative(direction);
        return level.getBlockState(neighborPos)
                .isFaceSturdy(level, neighborPos, direction.getOpposite());
    }

    public static boolean isStraightAlongAxis(final BlockState state, final Direction.Axis axis) {
        for (final Direction direction : Constants.DIRECTIONS) {
            final EnumProperty<ConnectionType> property =
                    BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(direction);
            if (axis.test(direction)) {
                if (state.getValue(property) != ConnectionType.CABLE) {
                    return false;
                }
            } else {
                if (state.getValue(property) != ConnectionType.NONE) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The side a cable at {@code pos} leans on, or null if it has no (or no needed) support. */
    @Nullable
    public static Direction getSupportSide(
            final BlockAndTintGetter level, final BlockPos pos, final BlockState state) {
        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<ConnectionType> property =
                        BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property)
                        && state.getValue(property) == ConnectionType.INTERFACE) {
                    return null; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }
        return supportSide;
    }
}
