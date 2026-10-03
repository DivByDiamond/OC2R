package li.cil.oc2.common.blockentity.network.cable;

import java.util.function.BiFunction;
import li.cil.oc2.platform.Platform;
import net.neoforged.neoforge.client.model.data.ModelData;

final class BusCableModelData {
    // A method reference to a Dist.CLIENT class is safe in common code as long as it is never
    // invoked off-client (invokedynamic resolves the target lazily, on first call) — same
    // pattern as MonitorStateManager#createMonitorSupplier. Wiring it once here avoids the
    // Class.forName/getMethod/invoke reflection this used to redo on every render frame.
    private static final BiFunction<BusCableBlockEntity, ModelData, ModelData> COMPUTE_MODEL_DATA =
            createComputeModelData();
    private final BusCableBlockEntity owner;
    private ModelData currentModelData;

    BusCableModelData(final BusCableBlockEntity owner) {
        this.owner = owner;
        this.currentModelData = Platform.environment().isClient() ? ModelData.EMPTY : null;
    }

    private static BiFunction<BusCableBlockEntity, ModelData, ModelData> createComputeModelData() {
        if (Platform.environment().isClient()) {
            return li.cil.oc2.client.hooks.BusCableModelHooks::computeModelData;
        }
        return (owner, current) -> ModelData.EMPTY;
    }

    ModelData getModelData() {
        if (!Platform.environment().isClient()) {
            return ModelData.EMPTY;
        }
        currentModelData = COMPUTE_MODEL_DATA.apply(owner, currentModelData);
        return currentModelData;
    }
}
