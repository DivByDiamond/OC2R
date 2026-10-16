package li.cil.oc2.common.bus.element.group;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.element.AbstractGroupingDeviceBusElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class DeferredItemDeviceLoadTest {
    @Test
    void collectingAfterLevelAssignmentRestoresPendingDeviceDataAndIdentity() {
        final HolderLookup.Provider registries = mock(HolderLookup.Provider.class);
        final CompoundTag deviceData = new CompoundTag();
        deviceData.putString("blob", "persisted-memory-image");
        deviceData.putLong("address", 0x80000000L);
        final Device original = mock(Device.class);
        when(original.serializeNBT(registries)).thenReturn(deviceData);
        final Element source = new Element();
        source.collect(registries, original);
        final UUID savedId = source.groupIds[0];
        final CompoundTag saved = source.save(registries);

        // loadAdditional runs before a computer has a level: no devices can exist yet.
        final Element restored = new Element();
        restored.loadAdditional(saved, registries);
        assertTrue(restored.getLocalDevices().isEmpty());
        assertEquals(savedId, restored.groupIds[0]);

        // loadServer recollects the installed items once server providers are available.
        final Device device = mock(Device.class);
        restored.collect(registries, device);
        assertEquals(Set.of(device), Set.copyOf(restored.getLocalDevices()));
        assertEquals(savedId, restored.getDeviceIdentifier(device).orElseThrow());
        verify(device).deserializeNBT(registries, deviceData);
        assertEquals(deviceData, restored.groupData[0].get("oc2r:memory"));
        verify(original, never()).dispose();
    }

    private static final class Element extends AbstractGroupingDeviceBusElement<GroupEntry, Object> {
        Element() {
            super(1);
        }

        void collect(final HolderLookup.Provider registries, final Device device) {
            final GroupEntry entry = mock(GroupEntry.class);
            when(entry.getDevice()).thenReturn(device);
            when(entry.getDeviceDataKey()).thenReturn(Optional.of("oc2r:memory"));
            when(entry.getLegacyDeviceDataKey()).thenReturn(Optional.empty());
            setEntriesForGroup(registries, 0, new GroupQueryResult<>() {
                @Override
                public Object getQuery() {
                    return null;
                }

                @Override
                public Set<GroupEntry> getEntries() {
                    return Set.of(entry);
                }
            });
        }
    }
}
