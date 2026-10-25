package li.cil.oc2.common.vm;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.server.MinecraftServer;

/** Quiesces guest workers before the server writes its final CPU and mapped-RAM state. */
public final class VirtualMachineShutdown {
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Set<AbstractVirtualMachine> MACHINES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private VirtualMachineShutdown() {}

    public static void register() {
        CommonEvents.SERVER_STOPPING.register(VirtualMachineShutdown::prepare);
    }

    public static void track(final AbstractVirtualMachine machine) {
        LOCK.lock();
        try {
            MACHINES.add(machine);
        } finally {
            LOCK.unlock();
        }
    }

    public static void untrack(final AbstractVirtualMachine machine) {
        LOCK.lock();
        try {
            MACHINES.remove(machine);
        } finally {
            LOCK.unlock();
        }
    }

    public static void prepare(final MinecraftServer server) {
        final AbstractVirtualMachine[] machines;
        LOCK.lock();
        try {
            machines = MACHINES.toArray(AbstractVirtualMachine[]::new);
        } finally {
            LOCK.unlock();
        }
        for (final AbstractVirtualMachine machine : machines) {
            machine.prepareForServerShutdown(server);
        }
    }
}
