package li.cil.oc2.common.bus.element.group;

import static li.cil.oc2.common.util.async.OptionalUtils.instanceOf;

import java.util.*;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.element.AbstractGroupingDeviceBusElement;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GroupManager<E extends GroupEntry, Q> {
    private static final String GROUPS_TAG_NAME = "groups";
    private static final String GROUP_ID_TAG_NAME = "groupId";
    private static final String GROUP_DATA_TAG_NAME = "groupData";

    private final AbstractGroupingDeviceBusElement<E, Q> element;

    public GroupManager(final AbstractGroupingDeviceBusElement<E, Q> element) {
        this.element = element;
    }

    public CompoundTag save(final HolderLookup.Provider registries) {
        final List<Tag> listTag = new ListTag();
        for (int i = 0; i < element.groupCount; i++) {
            saveGroup(registries, i);

            // NOPMD: a distinct sideTag per group is required as each is added to the list
            final CompoundTag sideTag = new CompoundTag(); // NOPMD allocation depends on loop iteration / per-item state
            sideTag.putUUID(GROUP_ID_TAG_NAME, element.groupIds[i]);
            sideTag.put(GROUP_DATA_TAG_NAME, element.groupData[i]);

            listTag.add(sideTag);
        }

        final CompoundTag tag = new CompoundTag();
        tag.put(GROUPS_TAG_NAME, (ListTag) listTag);
        return tag;
    }

    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
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

            for (final E entry : element.groups.get(i)) {
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

    public Optional<UUID> getDeviceIdentifier(final Device device) {
        for (int i = 0; i < element.groupCount; i++) {
            final Set<E> group = element.groups.get(i);
            for (final E deviceInfo : group) {
                if (Objects.equals(device, deviceInfo.getDevice())) {
                    return Optional.of(element.groupIds[i]);
                }
            }
        }
        return Optional.empty();
    }

    public void setEntriesForGroupUnloaded(final HolderLookup.Provider registries, final int index) {
        final Set<E> oldEntries = element.groups.get(index);
        if (oldEntries.isEmpty()) {
            return;
        }

        saveGroup(registries, index);

        for (final E entry : oldEntries) {
            element.devices.removeInt(entry.getDevice());
            element.onEntryRemoved(entry);
        }

        oldEntries.clear();

        element.scanDevices();
    }

    public void setEntriesForGroup(
            final HolderLookup.Provider registries,
            final int index,
            final GroupQueryResult<E, Q> queryResult) {
        final Set<E> newEntries = queryResult.getEntries();
        final Set<E> entries = element.groups.get(index);
        if (Objects.equals(newEntries, entries)) {
            if (entries.isEmpty()) {
                clearUnusedDataKeys(index, queryResult.getQuery());
            }
            return;
        }

        final boolean hadOldEntries = !entries.isEmpty();

        final Set<E> removedEntries = new HashSet<>(entries);
        removedEntries.removeAll(newEntries);
        removeEntries(removedEntries);

        final Set<E> addedEntries = new HashSet<>(newEntries);
        addedEntries.removeAll(entries);
        addEntries(addedEntries);

        entries.removeAll(removedEntries);
        entries.addAll(newEntries);

        updateDeviceData(
                registries, index, removedEntries, addedEntries, queryResult.getQuery());

        if (hadOldEntries) {
            element.groupIds[index] = UUID.randomUUID();
        }

        element.scanDevices();

        for (final E entry : removedEntries) {
            entry.getDevice().dispose();
        }
    }

    private void clearUnusedDataKeys(final int index, final Q query) {
        final CompoundTag devicesTag = element.groupData[index];
        if (devicesTag.isEmpty()) {
            return;
        }

        final Iterator<String> iterator = devicesTag.getAllKeys().iterator();
        while (iterator.hasNext()) {
            final String dataKey = iterator.next();
            if (devicesTag.contains(dataKey, NBTTagIds.TAG_COMPOUND)) {
                final CompoundTag tag = devicesTag.getCompound(dataKey);
                element.onEntryRemoved(dataKey, tag, query);
            }
            iterator.remove();
        }
    }

    private void removeEntries(final Set<E> removedEntries) {
        for (final E entry : removedEntries) {
            element.devices.removeInt(entry.getDevice());
            element.onEntryRemoved(entry);
        }
    }

    private void addEntries(final Set<E> addedEntries) {
        for (final E entry : addedEntries) {
            element.devices.put(entry.getDevice(), entry.getDeviceEnergyConsumption());
            element.onEntryAdded(entry);
        }
    }

    private void updateDeviceData(
            final HolderLookup.Provider registries,
            final int index,
            final Set<E> removedEntries,
            final Set<E> addedEntries,
            final Q query) {
        final CompoundTag devicesTag = element.groupData[index];
        for (final E entry : removedEntries) {
            entry.getDeviceDataKey().ifPresent(devicesTag::remove);
        }

        final Set<String> invalidDataKeys = new HashSet<>(devicesTag.getAllKeys());
        for (final E entry : addedEntries) {
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

        for (final String invalidDataKey : invalidDataKeys) {
            if (devicesTag.contains(invalidDataKey, NBTTagIds.TAG_COMPOUND)) {
                final CompoundTag tag = devicesTag.getCompound(invalidDataKey);
                element.onEntryRemoved(invalidDataKey, tag, query);
            }
            devicesTag.remove(invalidDataKey);
        }
    }

    private void saveGroup(final HolderLookup.Provider registries, final int index) {
        final CompoundTag devicesTag = new CompoundTag();
        for (final E entry : element.groups.get(index)) {
            entry.getDeviceDataKey()
                    .ifPresent(
                            key -> {
                                devicesTag.put(key, entry.getDevice().serializeNBT(registries));
                            });
        }
        element.groupData[index] = devicesTag;
    }
}