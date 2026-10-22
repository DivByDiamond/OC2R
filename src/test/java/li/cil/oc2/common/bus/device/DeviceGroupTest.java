package li.cil.oc2.common.bus.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import li.cil.oc2.api.bus.device.Device;
import org.junit.jupiter.api.Test;

class DeviceGroupTest {
    @Test
    @SuppressFBWarnings(
            value = "NP_NONNULL_PARAM_VIOLATION",
            justification =
                    "the group is deliberately created without a block entity; the test only"
                        + " exercises the member key bookkeeping")
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
    @SuppressFBWarnings(
            value = "NP_NONNULL_PARAM_VIOLATION",
            justification =
                    "the group is deliberately created without a block entity; the test only"
                        + " exercises the member key bookkeeping")
    void duplicateMemberKeyCannotOverwriteAnotherDevice() {
        final var group = new DeviceGroup(null);
        group.addDevice("monitor", mock(Device.class));
        assertThrows(IllegalArgumentException.class,
                () -> group.addDevice("monitor", mock(Device.class)));
    }
}
