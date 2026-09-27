package li.cil.oc2.platform;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlatformTest {
    @Test
    void missingImplementationFailsWithClearMessage() {
        final IllegalStateException e = assertThrows(IllegalStateException.class, Platform::registries);
        assertTrue(e.getMessage().contains(RegistryBridge.class.getName()));
    }
}
