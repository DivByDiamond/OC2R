package li.cil.oc2.platform.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A loader-independent event: mod code registers listeners, the loader module fires it from
 * whatever hook the loader offers (NeoForge event bus, Fabric callbacks).
 *
 * <p>Listeners are kept in registration order and may be added or removed while the event fires.
 *
 * @param <T> the listener type
 */
public final class Event<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();

    /** Adds {@code listener}; it runs on every subsequent firing until removed. */
    public void register(final T listener) {
        listeners.add(listener);
    }

    /** Removes a listener previously passed to {@link #register}. */
    public void unregister(final T listener) {
        listeners.remove(listener);
    }

    /** Runs {@code invoker} once for every registered listener. */
    public void fire(final Consumer<T> invoker) {
        for (final T listener : listeners) {
            invoker.accept(listener);
        }
    }
}
