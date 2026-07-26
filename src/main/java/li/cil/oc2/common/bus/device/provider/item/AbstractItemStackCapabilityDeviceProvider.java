
package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractItemStackCapabilityDeviceProvider<TCapability> extends AbstractItemDeviceProvider {
    private final Supplier<ItemCapability<TCapability, @Nullable Void>> capabilitySupplier;


    protected AbstractItemStackCapabilityDeviceProvider(final Supplier<ItemCapability<TCapability, @Nullable Void>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }


    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemCapability<TCapability, @Nullable Void> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final TCapability optional = query.getItemStack().getCapability(capability);
        if (optional == null) {
            return Optional.empty();
        }

        return getItemDevice(query, optional);
    }

    protected abstract Optional<ItemDevice> getItemDevice(ItemDeviceQuery query, TCapability value);
}
