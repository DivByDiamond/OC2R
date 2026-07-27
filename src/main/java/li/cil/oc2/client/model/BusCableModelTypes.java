package li.cil.oc2.client.model;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class BusCableModelTypes {
    public static final ModelProperty<BusCableSupportSide> BUS_CABLE_SUPPORT_PROPERTY =
            new ModelProperty<>();
    public static final ModelProperty<BusCableFacade> BUS_CABLE_FACADE_PROPERTY =
            new ModelProperty<>();

    public record BusCableSupportSide(Direction value) {}

    public record BusCableFacade(BlockState blockState, BakedModel model, ModelData data) {}

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
}
