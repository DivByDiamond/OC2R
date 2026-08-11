package li.cil.oc2.common.vm.context.managed;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.api.bus.device.vm.context.VMLifecycleEventBus;
import li.cil.oc2.common.vm.context.event.EventManager;

final class ManagedEventBus implements VMLifecycleEventBus {
    private final VMLifecycleEventBus parent;
    private final EventManager manager;
    private final List<Object> subscribers = new ArrayList<>();
    private boolean isFrozen;

    public ManagedEventBus(final VMLifecycleEventBus parent, final EventManager manager) {
        this.parent = parent;
        this.manager = manager;
    }

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        for (final Object subscriber : subscribers) {
            manager.unregister(subscriber);
        }
        subscribers.clear();
    }

    @Override
    public void register(final Object subscriber) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        parent.register(subscriber);
        subscribers.add(subscriber);
    }
}