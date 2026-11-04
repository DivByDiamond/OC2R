package li.cil.oc2.common.bus.element;

import java.util.*;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.element.group.GroupEntry;
import li.cil.oc2.common.bus.element.group.GroupManager;
import li.cil.oc2.common.bus.element.group.GroupQueryResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public abstract class AbstractGroupingDeviceBusElement<E extends GroupEntry, Q>
        extends AbstractDeviceBusElement {
    protected final GroupManager<E, Q> groupManager;

    public final int groupCount;
    public final List<Set<E>> groups;
    public final UUID[] groupIds;
    public final CompoundTag[] groupData;

    protected AbstractGroupingDeviceBusElement(final int groupCount) {
        super();
        this.groupCount = groupCount;
        this.groups = new ArrayList<>(groupCount);
        this.groupIds = new UUID[groupCount];
        this.groupData = new CompoundTag[groupCount];

        for (int i = 0; i < groupCount; i++) {
            groups.add(new HashSet<>());
            groupIds[i] = UUID.randomUUID();
            groupData[i] = new CompoundTag();
        }

        this.groupManager = new GroupManager<>(this);
    }

    public CompoundTag save(final HolderLookup.Provider registries) {
        return groupManager.save(registries);
    }

    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        groupManager.loadAdditional(tag, registries);
    }

    /**
     * Drops all group entries and their saved device data. Called before loading saved
     * state so devices of a previous state cannot be merged into the loaded one.
     */
    public void reset() {
        final boolean hadEntries = groups.stream().anyMatch(group -> !group.isEmpty());
        for (int i = 0; i < groupCount; i++) {
            for (final E entry : groups.get(i)) {
                devices.removeInt(entry.getDevice());
                onEntryRemoved(entry);
            }
            groups.get(i).clear();
            groupData[i] = new CompoundTag();
        }

        if (hadEntries) {
            scanDevices();
        }
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        return groupManager.getDeviceIdentifier(device);
    }

    protected final void setEntriesForGroupUnloaded(
            final HolderLookup.Provider registries, final int index) {
        groupManager.setEntriesForGroupUnloaded(registries, index);
    }

    protected final void setEntriesForGroup(
            final HolderLookup.Provider registries,
            final int index,
            final GroupQueryResult<E, Q> queryResult) {
        groupManager.setEntriesForGroup(registries, index, queryResult);
    }

    public void onEntryAdded(final E entry) {}

    public void onEntryRemoved(final E entry) {}

    public void onEntryRemoved(
            final String dataKey, final CompoundTag data, @Nullable final Q query) {}
}