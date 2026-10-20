package li.cil.oc2.common.vm.lifecycle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.misc.BuiltinDevices;
import li.cil.oc2.common.vm.misc.VersionedRootImages;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.sedna.Sedna;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class VMLifecycleRootRestoreTest {
    @Test
    void missingBaseStopsBootAndPreservesTheExactSavedDiskUntilItCanBeRecovered() throws Exception {
        Sedna.initialize();
        final var vm = new StubVirtualMachine();
        vm.state.builtinDevices = mock(BuiltinDevices.class);
        doThrow(new VersionedRootImages.MissingImageException("missing-test-base"))
                .when(vm.state.builtinDevices).prepareSavedOnyxRoot(anyString());
        final CompoundTag root = new CompoundTag();
        root.putString("baseImageId", "missing-test-base");
        final CompoundTag overlay = new CompoundTag();
        overlay.putByteArray("preserved-user-pages", new byte[] {17, 42, 91});
        root.put("overlay", overlay);
        final CompoundTag devices = new CompoundTag();
        devices.put("onyxRootFilesystem", root);
        final CompoundTag state = new CompoundTag();
        state.put("builtinDevices", devices);
        final CompoundTag saved = new CompoundTag();
        saved.put("state", state);
        final CompoundTag expected = saved.copy();
        final VMLifecycle lifecycle = new VMLifecycle(vm);

        lifecycle.deserialize(saved);
        saved.putString("external-mutation", "must not affect the retained copy");
        assertEquals(VMRunState.STOPPED, vm.runState);
        assertEquals(expected, lifecycle.serialize());
        vm.runState = VMRunState.LOADING_DEVICES;
        lifecycle.load();
        assertEquals(VMRunState.STOPPED, vm.runState);
        assertEquals(expected, lifecycle.serialize());
        assertTrue(vm.bootError.getString().contains("missing-test-base"));
    }

    @Test
    void malformedBaseIdentityDoesNotFallBackToTheLegacyFilesystem() {
        Sedna.initialize();
        for (final boolean emptyString : new boolean[] {false, true}) {
            final var vm = new StubVirtualMachine(); // NOPMD - Each test case needs independent fixture inputs.
            vm.state.builtinDevices = mock(BuiltinDevices.class);
            final CompoundTag root = new CompoundTag(); // NOPMD - Each test case needs independent fixture inputs.
            if (emptyString) root.putString("baseImageId", "");
            else root.putInt("baseImageId", 42);
            root.put("overlay", new CompoundTag()); // NOPMD - Each test case needs independent fixture inputs.
            final CompoundTag devices = new CompoundTag(); // NOPMD - Each test case needs independent fixture inputs.
            devices.put("onyxRootFilesystem", root);
            final CompoundTag state = new CompoundTag(); // NOPMD - Each test case needs independent fixture inputs.
            state.put("builtinDevices", devices);
            final CompoundTag saved = new CompoundTag(); // NOPMD - Each test case needs independent fixture inputs.
            saved.put("state", state);
            final VMLifecycle lifecycle = new VMLifecycle(vm); // NOPMD - Each test case needs independent fixture inputs.
            lifecycle.deserialize(saved);
            assertEquals(VMRunState.STOPPED, vm.runState);
            assertEquals(saved, lifecycle.serialize());
            verifyNoInteractions(vm.state.builtinDevices);
        }
    }

    private static final class StubVirtualMachine extends AbstractVirtualMachine {
        StubVirtualMachine() {
            super(new CommonDeviceBusController(mock(DeviceBusElement.class), 100));
        }
        @Override public void tick() {}
        @Override public boolean consumeEnergy(final int amount, final boolean simulate) { return true; }
        @Override public AbstractTerminalVMRunner createRunner() { return null; }
    }
}
