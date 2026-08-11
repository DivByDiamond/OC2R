package li.cil.oc2.common.vm.context;

import li.cil.oc2.common.vm.context.event.EventManager;
import li.cil.oc2.common.vm.context.interrupt.InterruptManager;
import li.cil.oc2.common.vm.context.memory.MemoryRangeManager;

public interface VMContextManagerCollection {
    InterruptManager getInterruptManager();

    MemoryRangeManager getMemoryRangeManager();

    EventManager getEventManager();
}
