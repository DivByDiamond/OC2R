package li.cil.oc2.common.bus.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Set;
import li.cil.oc2.api.bus.device.Device;
import org.junit.jupiter.api.Test;

class DeviceGroupTest {
    @Test
    void sameDeviceTypeCanHaveDistinctStableMemberKeys() {
        final var group = new DeviceGroup(null);
        final var first = mock(Device.class);
        final var second = mock(Device.class);
        group.addDevice("first", first);
        group.addDevice("second", second);
        group.addDevice("first", first);
        assertEquals(Set.of(first, second), group.getDevices());
        assertEquals("first", group.getDeviceKey(first));
        assertEquals("second", group.getDeviceKey(second));
    }

    @Test
    void duplicateMemberKeyCannotOverwriteAnotherDevice() {
        final var group = new DeviceGroup(null);
        group.addDevice("monitor", mock(Device.class));
        assertThrows(IllegalArgumentException.class,
                () -> group.addDevice("monitor", mock(Device.class)));
    }
}
