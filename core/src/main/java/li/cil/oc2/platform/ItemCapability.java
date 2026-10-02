package li.cil.oc2.platform;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-independent key for a capability exposed by an item stack, mirroring NeoForge's
 * {@code ItemCapability}. Item capabilities are never sided; see {@link BlockCapability} for the
 * shared model.
 *
 * @param <T> the type the capability exposes
 */
public final class ItemCapability<T> {
    private final ResourceLocation id;
    private final Class<T> type;

    private ItemCapability(final ResourceLocation id, final Class<T> type) {
        this.id = id;
        this.type = type;
    }

    /** Creates an item capability. */
    public static <T> ItemCapability<T> createVoid(
            final ResourceLocation id, final Class<T> type) {
        return new ItemCapability<>(id, type);
    }

    /** The globally unique id of this capability. */
    public ResourceLocation getId() {
        return id;
    }

    /** The Java type of the value this capability exposes. */
    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final ItemCapability<?> other)) {
            return false;
        }
        return id.equals(other.id) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "ItemCapability[" + id + ":" + type.getName() + "]";
    }
}
