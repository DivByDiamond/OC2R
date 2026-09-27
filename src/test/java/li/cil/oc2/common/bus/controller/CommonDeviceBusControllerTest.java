package li.cil.oc2.common.bus.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.Consumer;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.controller.event.AfterDeviceScanEvent;
import org.junit.jupiter.api.*;

class CommonDeviceBusControllerTest {

    DeviceBusElement rootElement;
    CommonDeviceBusController controller;

    @BeforeEach
    void setUp() {
        rootElement = mock(DeviceBusElement.class);
        controller = new CommonDeviceBusController(rootElement, 100);
    }

    @Test
    void testConstructorInitializesWithRootElement() {
        assertNotNull(controller);
        // Energy consumption is 0 initially since no elements are scanned yet
        assertEquals(0, controller.getEnergyConsumption());
    }

    @Test
    void testGetStateReturnsManagerState() {
        BusState state = controller.getState();
        assertNotNull(state);
    }

    @Test
    void testScheduleBusScanDelegatesToManager() {
        controller.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
        // scheduleBusScan(BUS_CHANGE) unconditionally moves the manager's state machine to
        // SCAN_PENDING (BusElementManager#scheduleBusScan), so this is observable via getState().
        assertEquals(BusState.SCAN_PENDING, controller.getState());
    }

    @Test
    void testGetDevicesReturnsEmptySetInitially() {
        assertTrue(controller.getDevices().isEmpty());
    }

    @Test
    void testGetDeviceIdentifiersReturnsEmptyForUnknownDevice() {
        Device unknown = mock(Device.class);
        assertTrue(controller.getDeviceIdentifiers(unknown).isEmpty());
    }

    @Test
    void testScanDevicesFiresBeforeDeviceScanListeners() {
        Runnable listener = mock(Runnable.class);
        controller.beforeDeviceScanListeners.add(listener);

        controller.scanDevices();

        verify(listener).run();
    }

    @Test
    void testScanDevicesFiresAfterDeviceScanListeners() {
        // Consumer.class cannot carry type arguments; suppress the single unchecked
        // conversion it forces, so the set entry and accept() stay fully typed.
        @SuppressWarnings("unchecked")
        final Consumer<AfterDeviceScanEvent> listener = mock(Consumer.class);
        controller.afterDeviceScanListeners.add(listener);

        controller.scanDevices();

        verify(listener).accept(any());
    }

    @Test
    void testDisposeDisposesManager() {
        controller.dispose();
        // No bus scan ever ran, so the manager holds no elements to remove this controller from;
        // dispose() must still be idempotent and leave energy consumption untouched.
        assertEquals(0, controller.getEnergyConsumption());
        assertDoesNotThrow(controller::dispose);
    }

    @Test
    void testSetDeviceContainersChangedDoesNothingInBase() {
        // Base implementation does nothing: neither the bus state nor energy consumption
        // should be affected by calling it.
        final BusState stateBefore = controller.getState();
        final int energyBefore = controller.getEnergyConsumption();

        controller.setDeviceContainersChanged();

        assertEquals(stateBefore, controller.getState());
        assertEquals(energyBefore, controller.getEnergyConsumption());
    }

    @Test
    void testOwnershipGoesToLowestKey() {
        final DeviceBusElement shared = mock(DeviceBusElement.class);
        final var low = new CommonDeviceBusController(mock(DeviceBusElement.class), 100, () -> 1L);
        final var high = new CommonDeviceBusController(mock(DeviceBusElement.class), 100, () -> 2L);
        when(shared.getControllers()).thenReturn(List.of(high, low));

        assertTrue(low.isOwnerOf(shared));
        assertFalse(high.isOwnerOf(shared));
    }

    @Test
    void testOwnershipTieBreakIsStable() {
        final DeviceBusElement shared = mock(DeviceBusElement.class);
        final var first = new CommonDeviceBusController(mock(DeviceBusElement.class), 100, () -> 7L);
        final var second =
                new CommonDeviceBusController(mock(DeviceBusElement.class), 100, () -> 7L);
        when(shared.getControllers()).thenReturn(List.of(second, first));

        // Equal keys fall back to identityHashCode: it must pick exactly one owner and keep
        // picking it, otherwise shared devices flip ownership between consecutive scans.
        final boolean firstOwns = first.isOwnerOf(shared);
        assertNotEquals(firstOwns, second.isOwnerOf(shared));
        for (int i = 0; i < 8; i++) {
            assertEquals(firstOwns, first.isOwnerOf(shared));
        }
    }

    @Test
    void testBusOverflowDefaultsToZero() {
        assertEquals(0, controller.getBusOverflow());
    }
}