package li.cil.oc2.platform;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * The id of the registry entry whose factory is currently running on this thread. 26.x blocks and items
 * need their id in their properties before they are constructed, while the mod's blocks and items build
 * their own properties in their constructors.
 */
public final class RegistrationContext {
    private static final ThreadLocal<ResourceLocation> CURRENT_ID = new ThreadLocal<>();

    private RegistrationContext() {}

    /**
     * Runs the given factory with the given registry id bound to the current thread.
     * <p>
     * The id is available to the factory via {@link #current()}, and the previous binding is
     * restored once the factory returns.
     *
     * @param id      the id of the registry entry the factory creates.
     * @param factory the factory creating the registry entry.
     * @param <T>     the type of the registry entry.
     * @return the value returned by the factory.
     */
    public static <T> T with(final ResourceLocation id, final Supplier<T> factory) {
        final ResourceLocation previous = CURRENT_ID.get();
        CURRENT_ID.set(id);
        try {
            return factory.get();
        } finally {
            if (previous == null) {
                CURRENT_ID.remove();
            } else {
                CURRENT_ID.set(previous);
            }
        }
    }

    /**
     * The id of the registry entry whose factory is currently running on this thread.
     *
     * @return the id of the registry entry being created.
     * @throws IllegalStateException if no registry factory is currently running on this thread.
     */
    public static ResourceLocation current() {
        final ResourceLocation id = CURRENT_ID.get();
        if (id == null) {
            throw new IllegalStateException("Block or item properties created outside of a registry factory");
        }
        return id;
    }
}
