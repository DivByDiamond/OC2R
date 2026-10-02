package li.cil.oc2.platform;

import org.jetbrains.annotations.Nullable;

/**
 * Cached lookup of a single block capability, mirroring NeoForge's {@code BlockCapabilityCache}:
 * it re-queries the level only when the capability at the cached position is invalidated.
 *
 * @param <T> the type the cached capability exposes
 */
public interface BlockCapabilityCache<T> {
    /** The cached capability value, or {@code null} when unavailable or invalidated. */
    @Nullable
    T get();
}
