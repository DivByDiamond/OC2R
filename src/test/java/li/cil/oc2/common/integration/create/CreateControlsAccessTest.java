package li.cil.oc2.common.integration.create;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateControlsAccessTest {
    private CreateControlsAccess api;

    @BeforeEach
    void bindPublicContract() throws ReflectiveOperationException {
        api = new CreateControlsAccess(CreateControlsFixtures.Smart.class,
                CreateControlsFixtures.Scroll.class, CreateControlsFixtures.Options.class);
    }

    @Test
    void recognizesOnlySmartEntitiesAndScrollSettings() {
        final var setting = new CreateControlsFixtures.Scroll(-256, 256);
        final var block = new CreateControlsFixtures.Smart().with(new Object()).with(setting);
        assertTrue(api.supports(block));
        assertFalse(api.supports(new Object()));
        assertEquals(List.of(setting), api.settings(block));
    }

    @Test
    void writesUseThePublicSetterIncludingClampAndNotificationEffects() {
        final var setting = new CreateControlsFixtures.Scroll(-128, 128);
        assertEquals(128, api.set(setting, 999));
        assertEquals(1, setting.callbacks);
        assertEquals(1, setting.changed);
        assertEquals(1, setting.syncs);
        assertEquals(-128, api.set(setting, -999));
        assertEquals(2, setting.callbacks);
        assertEquals(0, api.set(setting, 0));
        assertEquals(3, setting.callbacks);
    }

    @Test
    void unchangedValueRetainsCreatesNoOpSemantics() {
        final var setting = new CreateControlsFixtures.Scroll(-256, 256);
        assertEquals(0, api.set(setting, 0));
        assertEquals(0, setting.callbacks);
        assertEquals(0, setting.syncs);
    }

    @Test
    void inactiveControlsCannotBeChanged() {
        final var setting = new CreateControlsFixtures.Scroll(-256, 256);
        setting.active = false;
        assertFalse(api.active(setting));
        assertThrows(IllegalStateException.class, () -> api.set(setting, 100));
        assertEquals(0, setting.value);
        assertEquals(0, setting.callbacks);
    }

    @Test
    void exposesLabelsFormattingAndEnumOptions() {
        final var setting = new CreateControlsFixtures.Scroll(-256, 256);
        api.set(setting, -64);
        assertEquals("Rotation speed", api.label(setting));
        assertEquals("-64 RPM", api.formatted(setting));
        assertEquals(List.of(), api.options(setting));
        final var mode = new CreateControlsFixtures.Options();
        assertEquals(List.of("KEEP", "PLACE", "PLACE_RETURNED"), api.options(mode));
        assertEquals(2, api.set(mode, 999));
        assertEquals(CreateControlsFixtures.Mode.PLACE_RETURNED, mode.get());
    }

    @Test
    void inactiveControlsStayInEnumerationSoIndicesDoNotShift() {
        final var first = new CreateControlsFixtures.Scroll(-256, 256);
        final var second = new CreateControlsFixtures.Scroll(0, 64);
        first.active = false;
        final var block = new CreateControlsFixtures.Smart().with(first).with(second);
        assertEquals(List.of(first, second), api.settings(block));
    }

    @Test
    void preservesUsefulExceptionsFromCreateCallbacks() {
        final var error = assertThrows(IllegalStateException.class,
                () -> api.set(new CreateControlsFixtures.Broken(), 50));
        assertEquals("Machine offline", error.getMessage());
    }

    @Test
    void incompatiblePublicContractIsRejected() {
        assertThrows(NoSuchMethodException.class,
                () -> new CreateControlsAccess(Object.class,
                        CreateControlsFixtures.Scroll.class, CreateControlsFixtures.Options.class));
    }

    @Test
    void missingOptionalModCannotBreakAdapterLoading() {
        final ClassLoader empty = new ClassLoader(null) {};
        assertTrue(CreateControlsAccess.load(empty).isEmpty());
    }
}
