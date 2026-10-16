//? if >=26.1 {
/*package li.cil.oc2.client.hooks;

import li.cil.oc2.client.model.BusCableModelTypes;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public final class BusCableModelHooks {
    private BusCableModelHooks() {}

    // The block entity only hands over the facade block; cable geometry follows block state.
    public static ModelData computeModelData(final BusCableBlockEntity owner, final ModelData current) {
        final ItemStack facadeItem = owner.getFacade();
        if (!facadeItem.isEmpty()) {
            final BlockState facadeState = ItemStackUtils.getBlockState(facadeItem);
            if (facadeState != null) {
                return ModelData.of(BusCableModelTypes.BUS_CABLE_FACADE_STATE_PROPERTY, facadeState);
            }
        }
        return ModelData.EMPTY;
    }
}
*///?} else {
package li.cil.oc2.client.hooks;

import li.cil.oc2.client.model.BusCableModelTypes;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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

        // Drop stale support data: only explicit cable/interface parts are rendered.
        return ModelData.EMPTY;
    }
}
//?}
