package li.cil.oc2.common.bus.device.util.info;

import java.util.Objects;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;

public abstract class AbstractDeviceInfo<P, D extends Device> {
    @Nullable public final P provider;
    public final D device;

    protected AbstractDeviceInfo(@Nullable final P provider, final D device) {
        this.provider = provider;
        this.device = device;
    }

    public int getEnergyConsumption() {
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractDeviceInfo<?, ?> that = (AbstractDeviceInfo<?, ?>) o;
        return Objects.equals(provider, that.provider) && device.equals(that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, device);
    }
}