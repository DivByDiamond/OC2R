package li.cil.oc2.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PlatformTest {
    @Test
    void missingImplementationFailsWithClearMessage() {
        final IllegalStateException e = assertThrows(IllegalStateException.class, Platform::registries);
        assertEquals(true, e.getMessage().contains(RegistryBridge.class.getName()));
    }

    @Test
    void bridgeContractIsUsableWithoutMinecraft() {
        final RegistryBridge bridge = new RegistryBridge() {
            @Override
            public <T> Supplier<T> register(final String registryId, final String namespace, final String name,
                                            final Supplier<? extends T> factory) {
                return factory::get;
            }
        };
        assertEquals("x", bridge.<String>register("minecraft:block_type", "oc2r", "x", () -> "x").get());
    }
}
