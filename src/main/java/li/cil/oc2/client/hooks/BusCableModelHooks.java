package li.cil.oc2.client.hooks;

import li.cil.oc2.client.model.BusCableModelTypes;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public final class BusCableModelHooks {
    private BusCableModelHooks() {}

    public static ModelData computeModelData(final BusCableBlockEntity owner, final ModelData current) {
        final ItemStack facadeItem = owner.getFacade();
        if (!facadeItem.isEmpty()) {
            final BlockState facadeState = ItemStackUtils.getBlockState(facadeItem);
            if (facadeState != null) {
                final BlockModelShaper shapes = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
                final BakedModel model = shapes.getBlockModel(facadeState);
                final ModelData data = model.getModelData(owner.getLevel(), owner.getBlockPos(), facadeState, current);
                return ModelData.builder()
                        .with(BusCableModelTypes.BUS_CABLE_FACADE_PROPERTY, new BusCableModelTypes.BusCableFacade(facadeState, model, data))
                        .build();
            }
        }

        // No facade: compute support side from actual solid neighbor (original
        // BusCableModelData.getSupportModelData logic). Hardcoding Direction.UP
        // ships wrong visual state for every cable without a facade.
        final Level level = owner.getLevel();
        if (level == null) {
            return current;
        }
        final BlockState state = owner.getBlockState();
        final BlockPos pos = owner.getBlockPos();
        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (BusCableModelTypes.isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<ConnectionType> property =
                        BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property)
                        && state.getValue(property) == ConnectionType.INTERFACE) {
                    return current;
                }
                if (supportSide == null) {
                    supportSide = direction;
                }
            }
        }
        if (supportSide != null) {
            return ModelData.builder()
                    .with(BusCableModelTypes.BUS_CABLE_SUPPORT_PROPERTY, new BusCableModelTypes.BusCableSupportSide(supportSide))
                    .build();
        }
        return current;
    }
}