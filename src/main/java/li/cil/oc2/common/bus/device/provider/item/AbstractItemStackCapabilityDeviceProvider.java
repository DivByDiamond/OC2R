package li.cil.oc2.common.bus.device.provider.item;

import java.util.Optional;
import java.util.function.Supplier;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.platform.ItemCapability;
import li.cil.oc2.platform.Platform;

public abstract class AbstractItemStackCapabilityDeviceProvider<T>
        extends AbstractItemDeviceProvider {
    private final Supplier<ItemCapability<T>> capabilitySupplier;

    protected AbstractItemStackCapabilityDeviceProvider(
            final Supplier<ItemCapability<T>> capabilitySupplier) {
        super();
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemCapability<T> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final T optional =
                Platform.capabilities()
                        .getItemCapability(capability, query.getItemStack());
        if (optional == null) {
            return Optional.empty();
        }

        return getItemDevice(query, optional);
    }

    protected abstract Optional<ItemDevice> getItemDevice(ItemDeviceQuery query, T value);
}