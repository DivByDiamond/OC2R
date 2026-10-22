//? if >=26.1 {
/*
package li.cil.oc2.client.model;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelProperty;

public final class BusCableModelTypes {
    // The facade block of a camouflaged cable, handed from the block entity to the baked model.
    public static final ModelProperty<BlockState> BUS_CABLE_FACADE_STATE_PROPERTY =
            new ModelProperty<>();

    private BusCableModelTypes() {}
}
*///?} else {

package li.cil.oc2.client.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class BusCableModelTypes {
    public static final ModelProperty<BusCableFacade> BUS_CABLE_FACADE_PROPERTY =
            new ModelProperty<>();

    public record BusCableFacade(BlockState blockState, BakedModel model, ModelData data) {}
}
//?}
