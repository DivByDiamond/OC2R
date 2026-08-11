package li.cil.oc2.common.blockentity.network.cable.facade;

import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;

public final class NeighborListener implements ICapabilityInvalidationListener {
    ServerLevel level;
    AbstractBlockDeviceBusElement busElement;
    Direction side;

    public NeighborListener(
            final ServerLevel level,
            final AbstractBlockDeviceBusElement busElement,
            final Direction side) {
        this.level = level;
        this.busElement = busElement;
        this.side = side;
    }

    @Override
    public boolean onInvalidate() {
        ServerScheduler.schedule(level, () -> busElement.updateDevicesForNeighbor(side));
        return true;
    }
}