package li.cil.oc2.platform;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-independent key for a capability exposed by an entity, mirroring NeoForge's
 * {@code EntityCapability}. See {@link BlockCapability} for the shared model.
 *
 * @param <T> the type the capability exposes
 */
public final class EntityCapability<T> {
    private final ResourceLocation id;
    private final Class<T> type;
    private final boolean sided;

    private EntityCapability(final ResourceLocation id, final Class<T> type, final boolean sided) {
        this.id = id;
        this.type = type;
        this.sided = sided;
    }

    /** Creates a sided capability: providers and lookups carry a {@code Direction}. */
    public static <T> EntityCapability<T> createSided(
            final ResourceLocation id, final Class<T> type) {
        return new EntityCapability<>(id, type, true);
    }

    /** Creates a void capability: providers and lookups never carry a side. */
    public static <T> EntityCapability<T> createVoid(
            final ResourceLocation id, final Class<T> type) {
        return new EntityCapability<>(id, type, false);
    }

    /** The globally unique id of this capability. */
    public ResourceLocation getId() {
        return id;
    }

    /** The Java type of the value this capability exposes. */
    public Class<T> getType() {
        return type;
    }

    /** Whether this capability is queried with a side ({@code true}) or without one. */
    public boolean isSided() {
        return sided;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final EntityCapability<?> other)) {
            return false;
        }
        return sided == other.sided && id.equals(other.id) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, sided);
    }

    @Override
    public String toString() {
        return "EntityCapability[" + id + ":" + type.getName() + "]";
    }
}
