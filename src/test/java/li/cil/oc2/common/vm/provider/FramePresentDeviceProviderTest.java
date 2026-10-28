package li.cil.oc2.common.vm.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.oc2.common.vm.device.frame.FramePresentDevice;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.memory.SimpleMemoryMap;
import org.junit.jupiter.api.Test;

class FramePresentDeviceProviderTest {
    @Test
    void separateCompanionLinksBy64BitBaseWithoutExtendingAdjacentPixelRanges() {
        Sedna.initialize();
        DeviceTreeRegistry.putProvider(SimpleFramebufferDevice.class, new SimpleFramebufferDeviceProvider());
        DeviceTreeRegistry.putProvider(FramePresentDevice.class, new FramePresentDeviceProvider());
        final SimpleMemoryMap memory = new SimpleMemoryMap();
        final SimpleFramebufferDevice first = new SimpleFramebufferDevice(2, 1, ByteBuffer.allocate(4));
        final SimpleFramebufferDevice second = new SimpleFramebufferDevice(2, 1, ByteBuffer.allocate(4));
        final FramePresentDevice present = new FramePresentDevice(first);
        assertTrue(memory.addDevice(0x130000000L, first));
        assertTrue(memory.addDevice(0x130000004L, second));
        assertTrue(memory.addDevice(0x20000000L, present));
        final DeviceTree root = DeviceTreeRegistry.create(memory);
        root.addProp("#address-cells", 2).addProp("#size-cells", 2);
        root.getChild("soc").addProp("compatible", "simple-bus")
                .addProp("#address-cells", 2).addProp("#size-cells", 2).addProp("ranges");
        final DeviceTree node = DeviceTreeRegistry.visit(root, memory, present);
        assertNotNull(node);
        final String text = node.toString();
        assertEquals("/soc/frame-present@20000000", node.getPath());
        assertTrue(text.contains("compatible = \"oc2r,frame-present-v1\";"), text);
        assertTrue(text.contains("framebuffer-base = <0x1 0x30000000>;"), text);
        assertTrue(text.contains("reg = <0x0 0x20000000 0x0 0x20>;"), text);
        assertEquals(4, first.getLength());
        assertEquals(4, second.getLength());
    }
}
