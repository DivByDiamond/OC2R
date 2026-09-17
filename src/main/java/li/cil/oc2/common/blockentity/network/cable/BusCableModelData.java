package li.cil.oc2.common.blockentity.network.cable;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.model.data.ModelData;

final class BusCableModelData {
    private final BusCableBlockEntity owner;
    private Object currentModelData;

    BusCableModelData(final BusCableBlockEntity owner) {
        this.owner = owner;
        if (FMLLoader.getDist() == Dist.CLIENT) {
            this.currentModelData = ModelData.EMPTY;
        } else {
            this.currentModelData = null;
        }
    }

    ModelData getModelData() {
        if (FMLLoader.getDist() != Dist.CLIENT) {
            return ModelData.EMPTY;
        }
        try {
            final Class<?> hooks = Class.forName("li.cil.oc2.client.hooks.BusCableModelHooks");
            final ModelData current = (ModelData) currentModelData;
            final Object result = hooks
                    .getMethod(
                            "computeModelData",
                            li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity.class,
                            ModelData.class)
                    .invoke(null, owner, current);
            if (result != null) {
                currentModelData = result;
                return (ModelData) result;
            }
            return ModelData.EMPTY;
        } catch (final ReflectiveOperationException e) {
            return ModelData.EMPTY;
        }
    }
}
