package li.cil.oc2.common.blockentity.network.cable;

import static li.cil.oc2.client.model.BusCableModelTypes.BUS_CABLE_FACADE_PROPERTY;
import static li.cil.oc2.client.model.BusCableModelTypes.BUS_CABLE_SUPPORT_PROPERTY;

import li.cil.oc2.client.model.BusCableModelTypes;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.data.ModelData;

final class BusCableModelData {
    private final BusCableBlockEntity owner;
    private ModelData currentModelData = ModelData.EMPTY;

    BusCableModelData(final BusCableBlockEntity owner) {
        this.owner = owner;
    }

    ModelData getModelData() {
        final Level level = owner.getLevel();
        if (level == null) {
            return ModelData.EMPTY;
        }
        final BlockState state = owner.getBlockState();
        final BlockPos pos = owner.getBlockPos();
        if (hasFacade(state)) {
            return getFacadeModelData(level, pos);
        }
        return getSupportModelData(level, state, pos);
    }

    private static boolean hasFacade(final BlockState state) {
        return state.hasProperty(BusCableStateProperties.HAS_FACADE)
                && state.getValue(BusCableStateProperties.HAS_FACADE);
    }

    private ModelData getFacadeModelData(
            final Level level, final BlockPos pos) {
        BlockState facadeState = ItemStackUtils.getBlockState(owner.facadeManager.getFacade());
        if (facadeState == null) {
            facadeState = Blocks.IRON_BLOCK.defaultBlockState();
        }

        final var shapes = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        final var model = shapes.getBlockModel(facadeState);
        final ModelData data = model.getModelData(level, pos, facadeState, currentModelData);

        currentModelData =
                ModelData.builder()
                        .with(
                                BUS_CABLE_FACADE_PROPERTY,
                                new BusCableModelTypes.BusCableFacade(facadeState, model, data))
                        .build();

        return currentModelData;
    }

    private ModelData getSupportModelData(
            final Level level, final BlockState state, final BlockPos pos) {
        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (BusCableModelTypes.isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<ConnectionType> property =
                        BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property)
                        && state.getValue(property) == ConnectionType.INTERFACE) {
                    return currentModelData;
                }

                if (supportSide == null) {
                    supportSide = direction;
                }
            }
        }

        if (supportSide != null) {
            currentModelData =
                    ModelData.builder()
                            .with(
                                    BUS_CABLE_SUPPORT_PROPERTY,
                                    new BusCableModelTypes.BusCableSupportSide(supportSide))
                            .build();
            return currentModelData;
        }

        return currentModelData;
    }
}