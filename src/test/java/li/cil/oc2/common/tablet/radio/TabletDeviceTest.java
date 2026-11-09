package li.cil.oc2.common.tablet.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.common.entity.robot.state.RobotDevice;
import org.junit.jupiter.api.Test;

class TabletDeviceTest {
    private static Set<String> callbacks(final Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Callback.class))
                .map(Method::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> radioCallbacks(final Class<?> type) {
        return callbacks(type).stream()
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).contains("radio"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    void tabletOffersTheSameRadioCallsAsTheRobotExceptActionBinding() {
        final Set<String> expected = radioCallbacks(RobotDevice.class);
        // Only a robot has queued actions that can back a command result.
        expected.remove("bindRadioCommand");
        assertEquals(expected, radioCallbacks(TabletDevice.class));
    }

    @Test
    void messengerCallsAreAvailable() {
        final Set<String> names = callbacks(TabletDevice.class);
        assertTrue(names.containsAll(Set.of(
                "sendRadioMessage", "pollRadioMessages", "listRadioDevices", "pingRadio", "pollRadioEvents",
                "joinRadioNetwork", "getRadioAddress", "registerRadioService")));
    }

    @Test
    void theGuestCanNotSetCredentialsOrReadSecrets() {
        for (final String name : callbacks(TabletDevice.class)) {
            assertFalse(name.toLowerCase(java.util.Locale.ROOT).contains("credential"), name);
            assertFalse(name.toLowerCase(java.util.Locale.ROOT).contains("identity"), name);
            assertFalse(name.toLowerCase(java.util.Locale.ROOT).contains("secret"), name);
        }
    }
}
