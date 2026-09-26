package li.cil.oc2.bus.topology;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Picks the single controller that owns a shared device (docs/CABLE-SYSTEM.md, "Device ownership").
 *
 * <p>Ownership is computed from the controllers currently loaded on the network and never stored, so
 * when the owner disappears the next resolve deterministically hands the device to the next controller.
 */
public final class OwnerResolver {
    private OwnerResolver() {}

    /** The controller with the smallest key; ties are broken by {@code tieBreaker} for stability. */
    public static <C> Optional<C> owner(
            final Collection<? extends C> controllers,
            final java.util.function.ToLongFunction<C> key,
            final Comparator<C> tieBreaker) {
        C best = null;
        for (final C candidate : controllers) {
            if (best == null) {
                best = candidate;
                continue;
            }
            final int byKey = Long.compare(key.applyAsLong(candidate), key.applyAsLong(best));
            if (byKey < 0 || (byKey == 0 && tieBreaker.compare(candidate, best) < 0)) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }
}
