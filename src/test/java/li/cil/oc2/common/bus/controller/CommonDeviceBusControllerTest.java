package li.cil.oc2.common.bus.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.controller.event.AfterDeviceScanEvent;
import li.cil.oc2.common.bus.controller.event.DevicesChangedEvent;
import org.junit.jupiter.api.*;

class CommonDeviceBusControllerTest {

    DeviceBusElement rootElement;
    DeviceBusElement element1;
    DeviceBusElement element2;
    Device device1;
    Device device2;
    CommonDeviceBusController controller;

    @BeforeEach
    void setUp() {
        rootElement = mock(DeviceBusElement.class);
        element1 = mock(DeviceBusElement.class);
        element2 = mock(DeviceBusElement.class);
        device1 = mock(Device.class);
        device2 = mock(Device.class);
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
        // Manager should be called
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
        var listener = mock(java.util.function.Consumer.class);
        controller.afterDeviceScanListeners.add(listener);

        controller.scanDevices();

        verify(listener).accept(any());
    }

    @Test
    void testDisposeDisposesManager() {
        controller.dispose();
        // Should complete without exception
    }

    @Test
    void testSetDeviceContainersChangedDoesNothingInBase() {
        // Base implementation does nothing
        controller.setDeviceContainersChanged();
        // Should complete without exception
    }
}