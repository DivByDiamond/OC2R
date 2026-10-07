package li.cil.oc2.common.blockentity.network.cable.facade;

import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.platform.CapabilityInvalidationListener;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public final class NeighborListener extends CapabilityInvalidationListener {

    public NeighborListener(
            final ServerLevel level,
            final AbstractBlockDeviceBusElement busElement,
            final Direction side) {
        super(
                () -> {
                    ServerScheduler.schedule(level, () -> busElement.updateDevicesForNeighbor(side));
                    return true;
                });
    }
}