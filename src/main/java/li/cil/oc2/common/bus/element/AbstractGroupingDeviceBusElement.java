/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.element;

import li.cil.oc2.api.bus.device.Device;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractGroupingDeviceBusElement<TEntry extends GroupEntry, TQuery> extends AbstractDeviceBusElement {
    protected final GroupManager<TEntry, TQuery> groupManager;

    protected final int groupCount;
    protected final ArrayList<HashSet<TEntry>> groups;
    protected final UUID[] groupIds;
    protected final CompoundTag[] groupData;

    protected AbstractGroupingDeviceBusElement(final int groupCount) {
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

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        return groupManager.getDeviceIdentifier(device);
    }

    protected final void setEntriesForGroupUnloaded(final HolderLookup.Provider registries, final int index) {
        groupManager.setEntriesForGroupUnloaded(registries, index);
    }

    protected final void setEntriesForGroup(final HolderLookup.Provider registries, final int index, final GroupQueryResult<TEntry, TQuery> queryResult) {
        groupManager.setEntriesForGroup(registries, index, queryResult);
    }

    protected void onEntryAdded(final TEntry entry) {
    }

    protected void onEntryRemoved(final TEntry entry) {
    }

    protected void onEntryRemoved(final String dataKey, final CompoundTag data, @Nullable final TQuery query) {
    }
}
