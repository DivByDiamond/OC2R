package li.cil.oc2.common.vm.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.bus.device.util.optional.OptionalInterrupt;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.Sedna;
import li.cil.sedna.riscv.R5Board;
import org.junit.jupiter.api.Test;

class VMDeviceBusAdapterTest {
    @Test
    void coldRestartReclaimsInterruptsInsteadOfExhaustingOldReservations() {
        Sedna.initialize();
        final var context = new GlobalVMContext(new R5Board());
        final var adapter = new VMDeviceBusAdapter(context);
        final var devices = interruptDevices(20);
        adapter.addDevices(new ArrayList<>(devices));
        try {
            assertTrue(adapter.mountDevices().wasSuccessful(), "initial mount");
            adapter.disposeDevices();
            assertTrue(adapter.mountDevices().wasSuccessful(), "cold restart must reuse disposed IRQ slots");
            assertEquals(20, devices.stream().map(device -> device.interrupt.getAsInt()).distinct().count());
        } finally {
            adapter.disposeDevices();
            context.invalidate();
        }
    }

    @Test
    void suspensionKeepsReservationsForDevicesThatWillResume() {
        Sedna.initialize();
        final var context = new GlobalVMContext(new R5Board());
        final var adapter = new VMDeviceBusAdapter(context);
        final var newcomer = new VMDeviceBusAdapter(context);
        final var devices = interruptDevices(20);
        adapter.addDevices(new ArrayList<>(devices));
        try {
            assertTrue(adapter.mountDevices().wasSuccessful());
            final var previous = devices.stream().map(device -> device.interrupt.getAsInt()).toList();
            adapter.unmountDevices();
            final var added = new InterruptDevice();
            newcomer.addDevices(List.of(added));
            assertTrue(newcomer.mountDevices().wasSuccessful());
            assertFalse(previous.contains(added.interrupt.getAsInt()), "new device must leave suspended IRQs reserved");
            assertTrue(adapter.mountDevices().wasSuccessful());
            assertEquals(previous, devices.stream().map(device -> device.interrupt.getAsInt()).toList());
        } finally {
            newcomer.disposeDevices();
            adapter.disposeDevices();
            context.invalidate();
        }
    }

    private static List<InterruptDevice> interruptDevices(final int count) {
        final var devices = new ArrayList<InterruptDevice>();
        for (int index = 0; index < count; index++) {
            devices.add(new InterruptDevice()); // NOPMD each device owns a separate interrupt allocation
        }
        return devices;
    }

    private static final class InterruptDevice implements VMDevice {
        private final OptionalInterrupt interrupt = new OptionalInterrupt();

        @Override
        public VMDeviceLoadResult mount(final VMContext context) {
            return interrupt.claim(context) ? VMDeviceLoadResult.success() : VMDeviceLoadResult.fail();
        }

        @Override
        public void unmount() {}

        @Override
        public void dispose() {
            interrupt.clear();
        }
    }
}
