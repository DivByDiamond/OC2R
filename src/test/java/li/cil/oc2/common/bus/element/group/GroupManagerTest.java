package li.cil.oc2.common.bus.element.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.element.AbstractGroupingDeviceBusElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class GroupManagerTest {
    private static final String MONITOR_KEY = "monitor";
    private static final String KEYBOARD_KEY = "keyboard";
    private final HolderLookup.Provider registries = mock(HolderLookup.Provider.class);

    @Test
    void groupedDevicesKeepIndependentStateWhenDiscoveredAfterLoading() {
        assertRoundTrip(false);
    }

    @Test
    void groupedDevicesKeepIndependentStateWhenDiscoveredBeforeLoading() {
        assertRoundTrip(true);
    }

    @Test
    void singleProviderDeviceKeepsItsFlatSavedFormat() {
        final var original = new Element();
        final var device = new SavedDevice(123);
        original.discover(new Entry(device, Optional.empty()));
        final var saved = original.save(registries);
        final var restored = new Element();
        restored.loadAdditional(saved, registries);
        final var target = new SavedDevice(0);
        restored.discover(new Entry(target, Optional.empty()));
        assertEquals(123, target.value);
        assertEquals(device.serializeNBT(registries), restored.groupData[0].get("test:provider"));
    }

    @Test
    void removingMemberPreservesSiblingAndDoesNotRestoreMissingMemberFromWrapper() {
        final var element = new Element();
        final var monitor = new Entry(new SavedDevice(123), Optional.of(MONITOR_KEY));
        element.discover(monitor, new Entry(new SavedDevice(456), Optional.of(KEYBOARD_KEY)));
        element.save(registries);
        element.discover(monitor);
        final var provider = (CompoundTag) element.groupData[0].get("test:provider");
        final var members = (CompoundTag) provider.get("oc2r:group_members");
        assertTrue(members.contains(MONITOR_KEY));
        assertFalse(members.contains(KEYBOARD_KEY));
        final var keyboard = new SavedDevice(789);
        element.discover(monitor, new Entry(keyboard, Optional.of(KEYBOARD_KEY)));
        assertEquals(789, keyboard.value);
        assertNull(keyboard.loaded);
    }

    @Test
    void groupedMemberCanReadLegacyFlatState() {
        final var original = new Element();
        original.discover(new Entry(new SavedDevice(123), Optional.empty()));
        final var restored = new Element();
        restored.loadAdditional(original.save(registries), registries);
        final var device = new SavedDevice(0);
        restored.discover(new Entry(device, Optional.of(MONITOR_KEY)));
        assertEquals(123, device.value);
    }

    @Test
    void savingBeforeDiscoveryPreservesLoadedDeviceState() {
        final var original = new Element();
        original.discover(new Entry(new SavedDevice(123), Optional.of(MONITOR_KEY)),
                new Entry(new SavedDevice(456), Optional.of(KEYBOARD_KEY)));
        final var restored = new Element();
        restored.loadAdditional(original.save(registries), registries);
        // Initial world saves and chunk update packets can precede the first bus scan.
        restored.save(registries);
        final var monitor = new SavedDevice(0);
        final var keyboard = new SavedDevice(0);
        restored.discover(new Entry(monitor, Optional.of(MONITOR_KEY)),
                new Entry(keyboard, Optional.of(KEYBOARD_KEY)));
        assertEquals(123, monitor.value);
        assertEquals(456, keyboard.value);
    }

    @Test
    void savingUnloadedGroupPreservesCachedDeviceState() {
        final var element = new Element();
        element.discover(new Entry(new SavedDevice(123), Optional.of(MONITOR_KEY)));
        element.unload();
        element.save(registries);
        final var restored = new SavedDevice(0);
        element.discover(new Entry(restored, Optional.of(MONITOR_KEY)));
        assertEquals(123, restored.value);
    }

    @Test
    void confirmedEmptyDiscoveryDiscardsPendingDeviceState() {
        final var original = new Element();
        original.discover(new Entry(new SavedDevice(123), Optional.of(MONITOR_KEY)));
        final var restored = new Element();
        restored.loadAdditional(original.save(registries), registries);
        restored.discover();
        restored.save(registries);
        final var device = new SavedDevice(789);
        restored.discover(new Entry(device, Optional.of(MONITOR_KEY)));
        assertEquals(789, device.value);
        assertNull(device.loaded);
    }

    private void assertRoundTrip(final boolean discoverFirst) {
        final var original = new Element();
        original.discover(new Entry(new SavedDevice(123), Optional.of(MONITOR_KEY)),
                new Entry(new SavedDevice(456), Optional.of(KEYBOARD_KEY)));
        final var saved = original.save(registries);
        final var restored = new Element();
        final var monitor = new SavedDevice(0);
        final var keyboard = new SavedDevice(0);
        if (!discoverFirst) restored.loadAdditional(saved, registries);
        restored.discover(new Entry(keyboard, Optional.of(KEYBOARD_KEY)),
                new Entry(monitor, Optional.of(MONITOR_KEY)));
        if (discoverFirst) restored.loadAdditional(saved, registries);
        assertEquals(123, monitor.value, "monitor state was overwritten by another provider member");
        assertEquals(456, keyboard.value, "keyboard state was overwritten by another provider member");
        assertEquals(new SavedDevice(123).serializeNBT(registries), monitor.loaded);
        assertEquals(new SavedDevice(456).serializeNBT(registries), keyboard.loaded);
    }

    private record Entry(Device device, Optional<String> subKey) implements GroupEntry {
        @Override public Optional<String> getDeviceDataKey() { return Optional.of("test:provider"); }
        @Override public Optional<String> getDeviceDataSubKey() { return subKey; }
        @Override public Optional<String> getLegacyDeviceDataKey() { return Optional.empty(); }
        @Override public int getDeviceEnergyConsumption() { return 0; }
        @Override public Device getDevice() { return device; }
    }

    private final class Element extends AbstractGroupingDeviceBusElement<Entry, Object> {
        Element() { super(1); }
        void unload() { setEntriesForGroupUnloaded(registries, 0); }
        void discover(final Entry... entries) {
            setEntriesForGroup(registries, 0, new GroupQueryResult<>() {
                @Override public Object getQuery() { return null; }
                @Override public Set<Entry> getEntries() { return Set.of(entries); }
            });
        }
    }

    private static final class SavedDevice implements Device {
        private int value;
        private CompoundTag loaded;
        SavedDevice(final int value) { this.value = value; }
        @Override public CompoundTag serializeNBT(final HolderLookup.Provider registries) {
            final var tag = new CompoundTag();
            tag.putInt("value", value);
            tag.putLong("address", ((long) value) << 20);
            tag.putInt("interrupt", value % 24 + 8);
            return tag;
        }
        @Override public void deserializeNBT(final HolderLookup.Provider registries, final CompoundTag tag) {
            loaded = tag.copy();
            //? if >=26.1 {
            /*value = tag.getIntOr("value", 0);
            *///?} else {
            value = tag.getInt("value");
            //?}
        }
    }
}
