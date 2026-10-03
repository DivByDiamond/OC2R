package li.cil.oc2.common.blockentity.network.cable;

import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.platform.Platform;

/**
 * Holds the render model data of a bus cable. The data type is loader specific and client only, so it is
 * handled as an opaque object here; the client computes it through {@link ClientProxy}.
 */
final class BusCableModelData {
    private final BusCableBlockEntity owner;
    private Object currentModelData;

    BusCableModelData(final BusCableBlockEntity owner) {
        this.owner = owner;
        this.currentModelData = Platform.environment().isClient() ? ClientProxy.get().emptyModelData() : null;
    }

    Object getModelData() {
        if (!Platform.environment().isClient()) {
            return null;
        }
        currentModelData = ClientProxy.get().computeBusCableModelData(owner, currentModelData);
        return currentModelData;
    }
}
