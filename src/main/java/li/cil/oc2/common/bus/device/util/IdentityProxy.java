package li.cil.oc2.common.bus.device.util;

import java.util.Objects;
import javax.annotation.Nullable;

public abstract class IdentityProxy<T> {
    protected final T identity;

    public IdentityProxy(final T identity) {
        this.identity = identity;
    }

    /**
     * Exact-type equality is intentional here: proxies of different concrete types must never
     * be equal even when they wrap the same identity object (e.g. two devices over one block
     * entity), so {@code instanceof} alone would be too permissive.
     */
    @Override
    @SuppressWarnings("EqualsGetClass")
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final IdentityProxy<?> that = (IdentityProxy<?>) o;
        return identity.equals(that.identity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity);
    }
}