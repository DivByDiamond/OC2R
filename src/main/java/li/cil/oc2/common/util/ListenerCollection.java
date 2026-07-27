package li.cil.oc2.common.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

class ListenerCollection {
    private final Set<Runnable> listeners = Collections.newSetFromMap(new WeakHashMap<>());

    public void add(final Runnable listener) {
        listeners.add(listener);
    }

    public void remove(final Runnable listener) {
        listeners.remove(listener);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    public void run() {
        for (final Runnable runnable : listeners) {
            runnable.run();
        }
    }
}