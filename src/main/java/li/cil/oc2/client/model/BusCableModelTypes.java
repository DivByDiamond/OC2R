package li.cil.oc2.client.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class BusCableModelTypes {
    public static final ModelProperty<BusCableSupportSide> BUS_CABLE_SUPPORT_PROPERTY =
            new ModelProperty<>();
    public static final ModelProperty<BusCableFacade> BUS_CABLE_FACADE_PROPERTY =
            new ModelProperty<>();

    public record BusCableSupportSide(Direction value) {}

    public record BusCableFacade(BlockState blockState, BakedModel model, ModelData data) {}
}
