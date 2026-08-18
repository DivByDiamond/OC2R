package li.cil.oc2.common.bus.device.provider.item;

import java.util.Optional;
import java.util.function.Supplier;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractItemStackCapabilityDeviceProvider<T>
        extends AbstractItemDeviceProvider {
    private final Supplier<ItemCapability<T, @Nullable Void>> capabilitySupplier;

    protected AbstractItemStackCapabilityDeviceProvider(
            final Supplier<ItemCapability<T, @Nullable Void>> capabilitySupplier) {
        super();
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemCapability<T, @Nullable Void> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final T optional = query.getItemStack().getCapability(capability);
        if (optional == null) {
            return Optional.empty();
        }

        return getItemDevice(query, optional);
    }

    protected abstract Optional<ItemDevice> getItemDevice(ItemDeviceQuery query, T value);
}