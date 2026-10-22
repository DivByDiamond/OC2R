package li.cil.oc2.manual.client.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ManualRegistryTest {
    @Test
    void factoriesWaitForFirstUseAndAreEvaluatedOnlyOnce() {
        final ManualRegistry<Object> registry = new ManualRegistry<>(ResourceKey.createRegistryKey(
                ResourceLocation.parse("test:lazy_registry")));
        final AtomicInteger constructions = new AtomicInteger();
        final var supplier = registry.register("entry", () -> {
            constructions.incrementAndGet();
            return new Object();
        });
        assertEquals(0, constructions.get());
        assertSame(supplier.get(), supplier.get());
        assertSame(supplier.get(), registry.iterator().next());
        assertEquals(1, constructions.get());
    }

    @Test
    void registrationKeepsIdentityAndRejectsDuplicatesBeforeConstructingThem() {
        final ManualRegistry<Object> registry = new ManualRegistry<>(ResourceKey.createRegistryKey(
                ResourceLocation.parse("test:manual_registry")));
        final ResourceLocation id = ResourceLocation.parse("test:entry");
        final Object value = new Object();
        final var supplier = registry.register(id, () -> value);
        assertSame(value, supplier.get());
        assertSame(value, registry.iterator().next());
        assertEquals(id, RegistryUtils.id(registry.getKey(value).orElseThrow()));
        assertTrue(registry.getKey(new Object()).isEmpty());
        final AtomicInteger constructions = new AtomicInteger();
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(id, constructions::incrementAndGet));
        assertEquals(0, constructions.get());
    }

    @Test
    void iteratorsCannotRemoveRegisteredProviders() {
        final ManualRegistry<Object> registry = new ManualRegistry<>(ResourceKey.createRegistryKey(
                ResourceLocation.parse("test:immutable_registry")));
        registry.register("entry", Object::new);
        final var iterator = registry.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals(1, registry.entrySet().size());
    }
}
