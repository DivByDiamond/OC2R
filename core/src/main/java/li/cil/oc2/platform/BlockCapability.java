package li.cil.oc2.platform;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-independent key for a capability exposed by a block, mirroring NeoForge's
 * {@code BlockCapability} (an id plus the Java type of the exposed value).
 *
 * <p>Keys are created once by mod code (see {@code li.cil.oc2.common.capabilities.Capabilities})
 * and passed to {@link CapabilityRegistrar} for exposure and back through {@link CapabilityBridge}
 * for lookup; the loader implementation maps them onto its own capability system. A keyed
 * capability is either <em>sided</em> (its provider is asked for a specific {@code Direction}) or
 * void (never queried with a side).
 *
 * @param <T> the type the capability exposes
 */
public final class BlockCapability<T> {
    private final ResourceLocation id;
    private final Class<T> type;
    private final boolean sided;

    private BlockCapability(final ResourceLocation id, final Class<T> type, final boolean sided) {
        this.id = id;
        this.type = type;
        this.sided = sided;
    }

    /** Creates a sided capability: providers and lookups carry a {@code Direction}. */
    public static <T> BlockCapability<T> createSided(
            final ResourceLocation id, final Class<T> type) {
        return new BlockCapability<>(id, type, true);
    }

    /** Creates a void capability: providers and lookups never carry a side. */
    public static <T> BlockCapability<T> createVoid(
            final ResourceLocation id, final Class<T> type) {
        return new BlockCapability<>(id, type, false);
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
        if (!(o instanceof final BlockCapability<?> other)) {
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
        return "BlockCapability[" + id + ":" + type.getName() + "]";
    }
}
