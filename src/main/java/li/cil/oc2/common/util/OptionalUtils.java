package li.cil.oc2.common.util;

import java.util.Optional;
import java.util.function.Function;

public final class OptionalUtils {
    public static <T, C> Function<T, Optional<C>> instanceOf(final Class<C> type) {
        return (t) -> {
            if (type.isInstance(t)) {
                //noinspection unchecked
                return Optional.of((C) t);
            } else {
                return Optional.empty();
            }
        };
    }
}
