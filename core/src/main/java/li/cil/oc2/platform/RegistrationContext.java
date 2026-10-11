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

    public static ResourceLocation current() {
        final ResourceLocation id = CURRENT_ID.get();
        if (id == null) {
            throw new IllegalStateException("Block or item properties created outside of a registry factory");
        }
        return id;
    }
}
