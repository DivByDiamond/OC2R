package li.cil.oc2.client.model;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Loader independent state queries shared by the NeoForge and Fabric bus cable models. */
public final class BusCableModelUtils {
    private BusCableModelUtils() {}

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

}
