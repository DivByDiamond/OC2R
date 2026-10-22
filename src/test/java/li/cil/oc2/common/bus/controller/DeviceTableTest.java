package li.cil.oc2.common.bus.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import org.junit.jupiter.api.Test;

class DeviceTableTest {
    @Test
    void unchangedScanDoesNotReportDeviceChanges() {
        final var root = mock(DeviceBusElement.class);
        final var device = mock(Device.class);
        when(root.getLocalDevices()).thenReturn(List.of(device));
        final var controller = controller(root);
        controller.scanDevices();

        final List<Set<Device>> added = new ArrayList<>();
        final List<Set<Device>> removed = new ArrayList<>();
        final List<Boolean> scans = new ArrayList<>();
        controller.devicesAddedListeners.add(event -> added.add(Set.copyOf(event.devices())));
        controller.devicesRemovedListeners.add(event -> removed.add(Set.copyOf(event.devices())));
        controller.afterDeviceScanListeners.add(event -> scans.add(event.didDevicesChange()));

        controller.scanDevices();

        // VM lifecycle listeners schedule a reboot for device-change events. A scan
        // of the same hardware must still finish, without scheduling that reboot.
        assertTrue(added.isEmpty(), "unchanged scan reported added devices: " + added);
        assertTrue(removed.isEmpty(), "unchanged scan reported removed devices: " + removed);
        assertEquals(List.of(false), scans);
        assertEquals(Set.of(device), controller.getDevices());
    }

    @Test
    void reportsOnlyActuallyAddedAndRemovedDevices() {
        final var root = mock(DeviceBusElement.class);
        final var device = mock(Device.class);
        final var controller = controller(root);
        final List<Set<Device>> added = new ArrayList<>();
        final List<Set<Device>> removed = new ArrayList<>();
        controller.devicesAddedListeners.add(event -> added.add(Set.copyOf(event.devices())));
        controller.devicesRemovedListeners.add(event -> removed.add(Set.copyOf(event.devices())));

        when(root.getLocalDevices()).thenReturn(List.of(device));
        controller.scanDevices();
        assertEquals(List.of(Set.of(device)), added);
        assertTrue(removed.isEmpty());

        when(root.getLocalDevices()).thenReturn(List.of());
        controller.scanDevices();
        assertEquals(List.of(Set.of(device)), added);
        assertEquals(List.of(Set.of(device)), removed);
        assertFalse(controller.getDevices().contains(device));
    }

    private static CommonDeviceBusController controller(final DeviceBusElement root) {
        return new CommonDeviceBusController(root, 0) {
            @Override
            protected List<DeviceBusElement> getElements() {
                return List.of(root);
            }
        };
    }
}
