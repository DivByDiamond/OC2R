package li.cil.oc2.common.bus.adapter;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.sedna.api.device.serial.SerialDevice;
import org.junit.jupiter.api.Test;

class RPCDeviceLifecycleTest {
    @Test
    void hotPlugMountsOnlyTheNewDevicesWhileTheBusIsActive() {
        final DeviceBusController controller = mock(DeviceBusController.class);
        final RPCDevice first = device(controller);
        final RPCDevice second = device(controller);
        final RPCDeviceBusAdapter adapter = new RPCDeviceBusAdapter(mock(SerialDevice.class));
        when(controller.getDevices()).thenReturn(Set.of(first));
        adapter.resume(controller, true);
        verify(first, never()).mount();
        adapter.mountDevices();
        verify(first).mount();
        when(controller.getDevices()).thenReturn(Set.of(first, second));
        adapter.resume(controller, true);
        verify(first).mount();
        verify(second).mount();
        adapter.resume(controller, true);
        verify(first).mount();
        verify(second).mount();
        when(controller.getDevices()).thenReturn(Set.of(second));
        adapter.resume(controller, true);
        verify(first).unmount();
        adapter.unmountDevices();
        verify(second).unmount();
        adapter.disposeDevices();
        verify(first).unmount();
        verify(second).unmount();
    }

    @Test
    void aStoppedBusDiscoversDevicesWithoutMountingThem() {
        final DeviceBusController controller = mock(DeviceBusController.class);
        final RPCDevice first = device(controller);
        final RPCDevice second = device(controller);
        final RPCDeviceBusAdapter adapter = new RPCDeviceBusAdapter(mock(SerialDevice.class));
        when(controller.getDevices()).thenReturn(Set.of(first));
        adapter.resume(controller, true);
        adapter.mountDevices();
        adapter.unmountDevices();
        when(controller.getDevices()).thenReturn(Set.of(first, second));
        adapter.resume(controller, true);
        verify(second, never()).mount();
        adapter.mountDevices();
        verify(first, times(2)).mount();
        verify(second).mount();
        adapter.disposeDevices();
        verify(first, times(2)).unmount();
        verify(second).unmount();
    }

    private static RPCDevice device(final DeviceBusController controller) {
        final RPCDevice device = mock(RPCDevice.class);
        when(device.getMethodGroups()).thenReturn(List.of(mock(RPCMethodGroup.class)));
        when(device.getTypeNames()).thenReturn(List.of("lifecycle_test"));
        when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(UUID.randomUUID()));
        return device;
    }
}
