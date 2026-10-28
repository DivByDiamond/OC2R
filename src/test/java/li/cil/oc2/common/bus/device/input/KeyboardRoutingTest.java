package li.cil.oc2.common.bus.device.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.runner.VMDeviceBusAdapter;
import li.cil.sedna.Sedna;
import li.cil.sedna.riscv.R5Board;
import org.junit.jupiter.api.Test;

class KeyboardRoutingTest {
    @Test
    void independentlyManagedEndpointsShareOnlyTheirOwnGuestAndLoseIdentityOnUnmount() {
        Sedna.initialize();
        final GlobalVMContext firstContext = new GlobalVMContext(new R5Board());
        final GlobalVMContext secondContext = new GlobalVMContext(new R5Board());
        final VMDeviceBusAdapter firstGuest = new VMDeviceBusAdapter(firstContext);
        final VMDeviceBusAdapter secondGuest = new VMDeviceBusAdapter(secondContext);
        final KeyboardDevice<Object> first = new KeyboardDevice<>(new Object());
        final KeyboardDevice<Object> peer = new KeyboardDevice<>(new Object());
        final KeyboardDevice<Object> foreign = new KeyboardDevice<>(new Object());
        try {
            firstGuest.addDevices(List.of(first, peer));
            secondGuest.addDevices(List.of(foreign));
            assertTrue(firstGuest.mountDevices().wasSuccessful());
            assertTrue(secondGuest.mountDevices().wasSuccessful());
            assertTrue(first.sharesMachineWith(peer));
            assertTrue(peer.sharesMachineWith(first));
            assertFalse(first.sharesMachineWith(foreign));
            firstGuest.unmountDevices();
            assertFalse(first.sharesMachineWith(peer));
            assertFalse(first.sharesMachineWith(foreign));
        } finally {
            firstGuest.disposeDevices();
            secondGuest.disposeDevices();
            firstContext.invalidate();
            secondContext.invalidate();
        }
    }
}
