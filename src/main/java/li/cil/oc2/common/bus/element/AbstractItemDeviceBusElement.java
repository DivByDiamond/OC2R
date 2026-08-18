package li.cil.oc2.common.bus.element;

import static li.cil.oc2.common.util.RegistryUtils.optionalKey;
import static li.cil.oc2.common.util.async.OptionalUtils.instanceOf;

import java.util.*;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.info.ItemDeviceInfo;
import li.cil.oc2.common.bus.element.group.GroupEntry;
import li.cil.oc2.common.bus.element.group.GroupQueryResult;
import li.cil.oc2.common.util.item.ItemDeviceUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractItemDeviceBusElement
        extends AbstractGroupingDeviceBusElement<
                AbstractItemDeviceBusElement.ItemEntry, ItemDeviceQuery> {
    public AbstractItemDeviceBusElement(final int groupCount) {
        super(groupCount);
    }

    public boolean groupContains(final int groupIndex, final Device device) {
        for (final ItemEntry entry : groups.get(groupIndex)) {
            if (Objects.equals(entry.getDevice(), device)) {
                return true;
            }
        }

        return false;
    }

    public void handleSlotContentsChanged(
            final HolderLookup.Provider registries, final int slot, final ItemStack stack) {
        final ItemQueryResult queryResult = collectDevices(stack);

        setEntriesForGroup(registries, slot, queryResult);
    }

    public void exportDeviceDataToItemStack(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        final CompoundTag exportedTag = new CompoundTag();
        for (final ItemEntry entry : groups.get(slot)) {
            entry.getDeviceDataKey()
                    .ifPresent(
                            key -> {
                                // NOPMD - fresh tag needed per exported device
                                final CompoundTag deviceTag = new CompoundTag(); // NOPMD allocation depends on loop iteration / per-item state
                                entry.getDevice().exportToItemStack(deviceTag);
                                if (!deviceTag.isEmpty()) {
                                    exportedTag.put(key, deviceTag);
                                }
                            });
        }

        if (!exportedTag.isEmpty()) {
            ItemDeviceUtils.setItemDeviceData(stack, exportedTag);
        }
    }

    protected abstract ItemDeviceQuery makeQuery(final ItemStack stack);

    protected ItemQueryResult collectDevices(final ItemStack stack) {
        final ItemDeviceQuery query = makeQuery(stack);
        final Set<ItemEntry> entries = new HashSet<>();

        for (final ItemDeviceInfo deviceInfo : Devices.getDevices(query)) {
            entries.add(new ItemEntry(deviceInfo));
        }

        collectSyntheticDevices(query, entries);

        importDeviceDataFromItemStack(query, entries);

        return new ItemQueryResult(query, entries);
    }

    @SuppressWarnings("ConstantValue")
    protected void collectSyntheticDevices(
            final ItemDeviceQuery query, final Set<ItemEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        if (query.getItemStack().getDisplayName() != null) {
            entries.add(
                    new ItemEntry(
                            new ItemDeviceInfo(
                                    null,
                                    new TypeNameRPCDevice(
                                            query.getItemStack().getDisplayName().toString()),
                                    0)));
        }
    }

    @Override
    public void onEntryRemoved(
            final String dataKey, final CompoundTag tag, @Nullable final ItemDeviceQuery query) {
        super.onEntryRemoved(dataKey, tag, query);
        final Registry<ItemDeviceProvider> registry = Providers.itemDeviceProviderRegistry();
        final ItemDeviceProvider provider = registry.get(ResourceLocation.parse(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
        }
    }

    private void importDeviceDataFromItemStack(
            final ItemDeviceQuery query, final Set<ItemEntry> entries) {
        final CompoundTag exportedTag = ItemDeviceUtils.getItemDeviceData(query.getItemStack());
        if (!exportedTag.isEmpty()) {
            for (final ItemEntry entry : entries) {
                entry.getDeviceDataKey()
                        .map(exportedTag::get)
                        .or(
                                () ->
                                        // Older versions of the mod used a different id that often
                                        // collided between devices during save
                                        // Try loading from that if we can't load normally
                                        entry.getLegacyDeviceDataKey().map(exportedTag::get))
                        .flatMap(instanceOf(CompoundTag.class))
                        .ifPresent(entry.deviceInfo.device::importFromItemStack);
            }
        }
    }

    protected final class ItemQueryResult extends GroupQueryResult<ItemEntry, ItemDeviceQuery> {
        @Nullable private final ItemDeviceQuery query;
        private final Set<ItemEntry> entries;

        public ItemQueryResult(
                @Nullable final ItemDeviceQuery query, final Set<ItemEntry> entries) {
            super();
            this.query = query;
            this.entries = entries;
        }

        @Nullable
        @Override
        public ItemDeviceQuery getQuery() {
            return query;
        }

        @Override
        public Set<ItemEntry> getEntries() {
            return entries;
        }
    }

    protected record ItemEntry(ItemDeviceInfo deviceInfo) implements GroupEntry {
        @Override
        public Optional<String> getDeviceDataKey() {
            return optionalKey(deviceInfo.provider);
        }

        @Override
        public Optional<String> getLegacyDeviceDataKey() {
            if (deviceInfo.provider != null) {
                return Optional.of("oc2r:item_device_provider");
            }
            return Optional.empty();
        }

        @Override
        public int getDeviceEnergyConsumption() {
            return deviceInfo.getEnergyConsumption();
        }

        @Override
        public ItemDevice getDevice() {
            return deviceInfo.device;
        }
    }
}