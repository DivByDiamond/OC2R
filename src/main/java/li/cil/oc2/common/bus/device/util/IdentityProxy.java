package li.cil.oc2.common.bus.device.util;

import java.util.Objects;

import javax.annotation.Nullable;

public abstract class IdentityProxy<T> {
    protected final T identity;

    public IdentityProxy(final T identity) {
        this.identity = identity;
    }

    @Override
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
