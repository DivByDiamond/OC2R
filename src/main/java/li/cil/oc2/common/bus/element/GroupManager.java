package li.cil.oc2.common.bus.element;

import static li.cil.oc2.common.util.OptionalUtils.instanceOf;

import java.util.*;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

class GroupManager<TEntry extends GroupEntry, TQuery> {
    private static final String GROUPS_TAG_NAME = "groups";
    private static final String GROUP_ID_TAG_NAME = "groupId";
    private static final String GROUP_DATA_TAG_NAME = "groupData";

    private final AbstractGroupingDeviceBusElement<TEntry, TQuery> element;

    GroupManager(final AbstractGroupingDeviceBusElement<TEntry, TQuery> element) {
        this.element = element;
    }

    CompoundTag save(final HolderLookup.Provider registries) {
        final List<Tag> listTag = new ListTag();
        for (int i = 0; i < element.groupCount; i++) {
            saveGroup(registries, i);

            final CompoundTag sideTag = new CompoundTag();
            sideTag.putUUID(GROUP_ID_TAG_NAME, element.groupIds[i]);
            sideTag.put(GROUP_DATA_TAG_NAME, element.groupData[i]);

            listTag.add(sideTag);
        }

        final CompoundTag tag = new CompoundTag();
        tag.put(GROUPS_TAG_NAME, (ListTag) listTag);
        return tag;
    }

    void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final List<Tag> listTag = tag.getList(GROUPS_TAG_NAME, NBTTagIds.TAG_COMPOUND);

        final int count = Math.min(element.groupCount, listTag.size());
        for (int i = 0; i < count; i++) {
            final CompoundTag sideTag = (CompoundTag) listTag.get(i);

            if (sideTag.hasUUID(GROUP_ID_TAG_NAME)) {
                element.groupIds[i] = sideTag.getUUID(GROUP_ID_TAG_NAME);
            }
            if (sideTag.contains(GROUP_DATA_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                element.groupData[i] = sideTag.getCompound(GROUP_DATA_TAG_NAME);
            }

            for (final TEntry entry : element.groups.get(i)) {
                final CompoundTag devicesTag = element.groupData[i];
                entry.getDeviceDataKey()
                        .map(devicesTag::get)
                        .or(() -> entry.getLegacyDeviceDataKey().map(devicesTag::get))
                        .flatMap(instanceOf(CompoundTag.class))
                        .ifPresent(
                                deviceTag ->
                                        entry.getDevice().deserializeNBT(registries, deviceTag));
            }
        }
    }

    Optional<UUID> getDeviceIdentifier(final Device device) {
        for (int i = 0; i < element.groupCount; i++) {
            final Set<TEntry> group = element.groups.get(i);
            for (final TEntry deviceInfo : group) {
                if (Objects.equals(device, deviceInfo.getDevice())) {
                    return Optional.of(element.groupIds[i]);
                }
            }
        }
        return Optional.empty();
    }

    void setEntriesForGroupUnloaded(final HolderLookup.Provider registries, final int index) {
        final Set<TEntry> oldEntries = element.groups.get(index);
        if (oldEntries.isEmpty()) {
            return;
        }

        saveGroup(registries, index);

        for (final TEntry entry : oldEntries) {
            element.devices.removeInt(entry.getDevice());
            element.onEntryRemoved(entry);
        }

        oldEntries.clear();

        element.scanDevices();
    }

    void setEntriesForGroup(
            final HolderLookup.Provider registries,
            final int index,
            final GroupQueryResult<TEntry, TQuery> queryResult) {
        final Set<TEntry> newEntries = queryResult.getEntries();
        final Set<TEntry> entries = element.groups.get(index);
        if (Objects.equals(newEntries, entries)) {
            if (entries.isEmpty()) {
                final CompoundTag devicesTag = element.groupData[index];
                if (!devicesTag.isEmpty()) {
                    final Iterator<String> iterator = devicesTag.getAllKeys().iterator();
                    while (iterator.hasNext()) {
                        final String dataKey = iterator.next();
                        if (devicesTag.contains(dataKey, NBTTagIds.TAG_COMPOUND)) {
                            final CompoundTag tag = devicesTag.getCompound(dataKey);
                            element.onEntryRemoved(dataKey, tag, queryResult.getQuery());
                        }
                        iterator.remove();
                    }
                }
            }
            return;
        }

        final boolean hadOldEntries = !entries.isEmpty();

        final Set<TEntry> removedEntries = new HashSet<>(entries);
        removedEntries.removeAll(newEntries);
        for (final TEntry entry : removedEntries) {
            element.devices.removeInt(entry.getDevice());
            element.onEntryRemoved(entry);
        }

        final Set<TEntry> addedEntries = new HashSet<>(newEntries);
        addedEntries.removeAll(entries);
        for (final TEntry entry : addedEntries) {
            element.devices.put(entry.getDevice(), entry.getDeviceEnergyConsumption());
            element.onEntryAdded(entry);
        }

        entries.removeAll(removedEntries);
        entries.addAll(newEntries);

        final CompoundTag devicesTag = element.groupData[index];
        for (final TEntry entry : removedEntries) {
            entry.getDeviceDataKey().ifPresent(devicesTag::remove);
        }

        final Set<String> invalidDataKeys = new HashSet<>(devicesTag.getAllKeys());
        for (final TEntry entry : addedEntries) {
            entry.getDeviceDataKey()
                    .ifPresent(
                            key -> {
                                invalidDataKeys.remove(key);
                                if (devicesTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                                    entry.getDevice()
                                            .deserializeNBT(
                                                    registries, devicesTag.getCompound(key));
                                } else {
                                    devicesTag.remove(key);
                                    entry.getLegacyDeviceDataKey()
                                            .map(devicesTag::get)
                                            .flatMap(instanceOf(CompoundTag.class))
                                            .ifPresent(
                                                    deviceTag ->
                                                            entry.getDevice()
                                                                    .deserializeNBT(
                                                                            registries, deviceTag));
                                }
                            });
        }

        final TQuery query = queryResult.getQuery();
        for (final String invalidDataKey : invalidDataKeys) {
            if (devicesTag.contains(invalidDataKey, NBTTagIds.TAG_COMPOUND)) {
                final CompoundTag tag = devicesTag.getCompound(invalidDataKey);
                element.onEntryRemoved(invalidDataKey, tag, query);
            }
            devicesTag.remove(invalidDataKey);
        }

        if (hadOldEntries) {
            element.groupIds[index] = UUID.randomUUID();
        }

        element.scanDevices();

        for (final TEntry entry : removedEntries) {
            entry.getDevice().dispose();
        }
    }

    private void saveGroup(final HolderLookup.Provider registries, final int index) {
        final CompoundTag devicesTag = new CompoundTag();
        for (final TEntry entry : element.groups.get(index)) {
            entry.getDeviceDataKey()
                    .ifPresent(
                            key -> {
                                devicesTag.put(key, entry.getDevice().serializeNBT(registries));
                            });
        }
        element.groupData[index] = devicesTag;
    }
}