package li.cil.oc2.common.vm.handler;

import java.util.*;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.common.bus.element.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.element.AbstractItemDeviceBusElement;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.container.handler.AbstractDeviceItemStackHandler;
import li.cil.oc2.common.container.handler.AbstractTypedDeviceItemStackHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

public abstract class AbstractVMItemStackHandlers implements VMItemStackHandlers {
    public record GroupDefinition(DeviceType deviceType, int count) {}

    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x20000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;
    private static final long OTHER_DEVICE_BASE_ADDRESS = 0x30000000L;

    public final AbstractDeviceBusElement busElement = new VMBusElement();

    // NB: linked hash map such that order of parameters in constructor is retained.
    //     This is relevant when assigning default addresses for devices.
    private final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers =
            Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new LinkedHashMap<>())));

    public final IItemHandler combinedItemHandlers;

    public AbstractVMItemStackHandlers(
            Supplier<HolderLookup.Provider> providerSupplier, final GroupDefinition... groups) {
        for (final GroupDefinition group : groups) {
            itemHandlers.put(
                    group.deviceType,
                    new VMItemHandler(providerSupplier, group.count, group.deviceType));
        }

        combinedItemHandlers =
                new CombinedInvWrapper(
                        itemHandlers.values().toArray(new IItemHandlerModifiable[0]));
    }

    @Override
    public Optional<IItemHandler> getItemHandler(final DeviceType deviceType) {
        return Optional.ofNullable(itemHandlers.get(deviceType));
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < combinedItemHandlers.getSlots(); slot++) {
            if (!combinedItemHandlers.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public OptionalLong getDeviceAddressBase(final VMDevice wrapper) {
        long address = ITEM_DEVICE_BASE_ADDRESS;

        for (final Map.Entry<DeviceType, AbstractTypedDeviceItemStackHandler> entry :
                itemHandlers.entrySet()) {
            final DeviceType deviceType = entry.getKey();
            final AbstractDeviceItemStackHandler handler = entry.getValue();

            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getBusElement().groupContains(i, wrapper)) {
                    // Ahhh, such special casing, much wow. Honestly I don't expect this
                    // special case to ever be needed for anything other than physical
                    // memory, so it's fine. Prove me wrong.
                    if (deviceType == DeviceTypes.MEMORY) {
                        return OptionalLong.empty();
                    } else {
                        return OptionalLong.of(address);
                    }
                }

                address += ITEM_DEVICE_STRIDE;
            }
        }

        return OptionalLong.of(OTHER_DEVICE_BASE_ADDRESS);
    }

    @Override
    public void exportDeviceDataToItemStacks() {
        for (final AbstractDeviceItemStackHandler handler : itemHandlers.values()) {
            handler.exportDeviceDataToItemStacks();
        }
    }

    public void saveItems(RestrictedContainer container) {
        VMItemStackHandlerSerialization.saveItems(itemHandlers, container);
    }

    public void saveItems(HolderLookup.Provider provider, final CompoundTag tag) {
        VMItemStackHandlerSerialization.saveItems(itemHandlers, provider, tag);
    }

    public CompoundTag saveItems(HolderLookup.Provider provider) {
        return VMItemStackHandlerSerialization.saveItems(itemHandlers, provider);
    }

    public void loadItems(HolderLookup.Provider provider, RestrictedContainer container) {
        VMItemStackHandlerSerialization.loadItems(itemHandlers, provider, container);
    }

    public void loadItems(HolderLookup.Provider provider, final CompoundTag tag) {
        VMItemStackHandlerSerialization.loadItems(itemHandlers, provider, tag);
    }

    public void saveDevices(HolderLookup.Provider registries, final CompoundTag tag) {
        VMItemStackHandlerSerialization.saveDevices(itemHandlers, registries, tag);
    }

    public CompoundTag saveDevices(HolderLookup.Provider registries) {
        return VMItemStackHandlerSerialization.saveDevices(itemHandlers, registries);
    }

    public void loadDevices(HolderLookup.Provider registries, final CompoundTag tag) {
        VMItemStackHandlerSerialization.loadDevices(itemHandlers, registries, tag);
    }

    protected abstract ItemDeviceQuery makeQuery(final ItemStack stack);

    protected void onChanged() {}

    private final class VMItemHandler extends AbstractTypedDeviceItemStackHandler {
        private final VMItemBusElement busElement;

        public VMItemHandler(
                Supplier<HolderLookup.Provider> providerSupplier,
                final int size,
                final DeviceType deviceType) {
            super(providerSupplier, size, deviceType);
            this.busElement = new VMItemBusElement(getSlots());
        }

        @Override
        public AbstractItemDeviceBusElement getBusElement() {
            return busElement;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);
            onChanged();
        }
    }

    private final class VMItemBusElement extends AbstractItemDeviceBusElement {
        public VMItemBusElement(final int groupCount) {
            super(groupCount);
        }

        @Override
        protected ItemDeviceQuery makeQuery(final ItemStack stack) {
            return AbstractVMItemStackHandlers.this.makeQuery(stack);
        }
    }

    private final class VMBusElement extends AbstractDeviceBusElement {
        @Override
        public Optional<Collection<DeviceBusElement>> getNeighbors() {
            return Optional.of(
                    itemHandlers.values().stream()
                            .map(AbstractDeviceItemStackHandler::getBusElement)
                            .collect(Collectors.toList()));
        }
    }
}