package li.cil.oc2.data.model;

import li.cil.oc2.common.item.Items;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;

final class CableItemTransforms {
    static void buildBusCableModel(final ItemModelProvider itemModels, final ModelFile parent) {
        itemModels
                .getBuilder(Items.BUS_CABLE.getId().getPath())
                .parent(parent)
                .transforms()
                .transform(ItemDisplayContext.GUI)
                .rotation(30, 225, 0)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, 0)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.FIXED)
                .rotation(0, 180, 0)
                .scale(1.0f)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 45, 0)
                .translation(0, 2.5f, 0)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 45, 0)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 225, 0)
                .scale(0.75f)
                .end();
    }

    static void buildBusInterfaceModel(final ItemModelProvider itemModels, final ModelFile parent) {
        itemModels
                .getBuilder(Items.BUS_INTERFACE.getId().getPath())
                .parent(parent)
                .transforms()
                .transform(ItemDisplayContext.GUI)
                .rotation(30, 315, 0)
                .translation(2, 1, 0)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, -5)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.FIXED)
                .rotation(0, 180, 0)
                .translation(0, 0, 4)
                .scale(1.0f)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 180, 0)
                .translation(0, -1, 0)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 180, 0)
                .translation(0, 0, 2)
                .scale(0.75f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 180, 0)
                .translation(0, 0, 2)
                .scale(0.75f)
                .end();
    }
}
