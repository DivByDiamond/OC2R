package li.cil.oc2.common.vm;

import java.util.Map;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.container.AbstractTypedDeviceItemStackHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

final class VMItemStackHandlerSerialization {
    static void saveItems(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final RestrictedContainer container) {
        itemHandlers.forEach((deviceType, handler) -> handler.saveItems(container));
    }

    static void saveItems(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider provider,
            final CompoundTag tag) {
        itemHandlers.forEach(
                (deviceType, handler) -> {
                    if (!handler.isEmpty()) {
                        tag.put(deviceType.getName().toString(), handler.saveItems(provider));
                    }
                });
    }

    static CompoundTag saveItems(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        saveItems(itemHandlers, provider, tag);
        return tag;
    }

    static void loadItems(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider provider,
            final RestrictedContainer container) {
        itemHandlers.forEach((deviceType, handler) -> handler.loadItems(provider, container));
    }

    static void loadItems(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider provider,
            final CompoundTag tag) {
        itemHandlers.forEach(
                (deviceType, handler) ->
                        handler.loadItems(
                                provider, tag.getCompound(deviceType.getName().toString())));
    }

    static void saveDevices(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider registries,
            final CompoundTag tag) {
        itemHandlers.forEach(
                (deviceType, handler) ->
                        tag.put(deviceType.getName().toString(), handler.saveDevices(registries)));
    }

    static CompoundTag saveDevices(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        saveDevices(itemHandlers, registries, tag);
        return tag;
    }

    static void loadDevices(
            final Map<DeviceType, AbstractTypedDeviceItemStackHandler> itemHandlers,
            final HolderLookup.Provider registries,
            final CompoundTag tag) {
        itemHandlers.forEach(
                (deviceType, handler) ->
                        handler.loadDevices(
                                registries, tag.getCompound(deviceType.getName().toString())));
    }
}
