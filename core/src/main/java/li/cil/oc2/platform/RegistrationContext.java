package li.cil.oc2.platform;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * The id of the registry entry whose factory is currently running on this thread. 26.x blocks and items
 * need their id in their properties before they are constructed, while the mod's blocks and items build
 * their own properties in their constructors.
 */
public final class RegistrationContext {
    private static final ThreadLocal<ResourceLocation> CURRENT = new ThreadLocal<>();

    private RegistrationContext() {}

    public static <T> T with(final ResourceLocation id, final Supplier<T> factory) {
        final ResourceLocation previous = CURRENT.get();
        CURRENT.set(id);
        try {
            return factory.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static ResourceLocation current() {
        final ResourceLocation id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("Block or item properties created outside of a registry factory");
        }
        return id;
    }
}
