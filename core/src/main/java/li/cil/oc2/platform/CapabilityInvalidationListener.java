package li.cil.oc2.platform;

import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.Nullable;

/**
 * Notified when the capabilities at a block position are invalidated (the block changed or its
 * block entity was removed), so cached lookups of that position can be refreshed. Mirrors NeoForge's
 * {@code ICapabilityInvalidationListener}.
 *
 * <p>This is a class rather than an interface because the loader holds registered listeners
 * <em>weakly</em> (NeoForge collects garbage-collected listeners through a reference queue). An
 * interface would force the bridge to hand the loader a fresh adapter object that nothing else
 * references, and the loader would drop it on the next collection; instead the bridge stores its
 * adapter in the listener itself, so its lifetime is the caller's (typically a block entity field).
 */
public class CapabilityInvalidationListener {
    private final BooleanSupplier callback;

    private transient Object nativeListener;

    /** Creates a listener that reports {@code callback}'s result for every invalidation. */
    public CapabilityInvalidationListener(final BooleanSupplier callback) {
        this.callback = callback;
    }

    /**
     * Called when capabilities at the observed position were invalidated.
     *
     * @return {@code true} to stay registered for future invalidations, {@code false} to be removed
     */
    public boolean onInvalidate() {
        return callback.getAsBoolean();
    }

    /** The loader-specific listener handed to the loader, created once by the bridge. */
    @Nullable
    Object getNativeListener() {
        return nativeListener;
    }

    /** Stores the loader-specific listener so the bridge does not build a second one. */
    void setNativeListener(@Nullable final Object nativeListener) {
        this.nativeListener = nativeListener;
    }
}
