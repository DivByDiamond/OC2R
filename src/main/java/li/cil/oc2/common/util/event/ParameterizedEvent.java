package li.cil.oc2.common.util.event;

import java.util.HashSet;
import java.util.function.Consumer;

public final class ParameterizedEvent<T> extends HashSet<Consumer<T>>
        implements Consumer<T> {
    @Override
    public void accept(final T event) {
        for (final Consumer<T> listener : this) {
            listener.accept(event);
        }
    }
}