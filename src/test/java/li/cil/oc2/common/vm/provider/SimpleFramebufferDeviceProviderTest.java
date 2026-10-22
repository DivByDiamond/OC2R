package li.cil.oc2.common.vm.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.memory.SimpleMemoryMap;
import org.junit.jupiter.api.Test;

class SimpleFramebufferDeviceProviderTest {
    @Test
    void enumeratesBothFramebuffersOnTheMmioBusWithoutChangingChosen() {
        Sedna.initialize();
        DeviceTreeRegistry.putProvider(
                SimpleFramebufferDevice.class, new SimpleFramebufferDeviceProvider());
        final SimpleMemoryMap memory = new SimpleMemoryMap();
        final SimpleFramebufferDevice projector = framebuffer(640, 480);
        final SimpleFramebufferDevice monitor = framebuffer(640, 400);
        assertTrue(memory.addDevice(0x30000000L, projector));
        assertTrue(memory.addDevice(0x30096000L, monitor));

        final DeviceTree root = DeviceTreeRegistry.create(memory);
        root.addProp("#address-cells", 2).addProp("#size-cells", 2);
        final DeviceTree soc = root.getChild("soc");
        soc.addProp("compatible", "simple-bus")
                .addProp("#address-cells", 2).addProp("#size-cells", 2).addProp("ranges");
        final DeviceTree chosen = root.getChild("chosen");
        chosen.addProp("bootargs", "console=ttyS0").addProp("stdout-path", "/soc/uart@10000000");
        final String originalChosen = chosen.toString();

        final DeviceTree first = DeviceTreeRegistry.visit(root, memory, projector);
        final DeviceTree second = DeviceTreeRegistry.visit(root, memory, monitor);
        assertNotNull(first);
        assertNotNull(second);
        assertEquals("/soc/framebuffer@30000000", first.getPath());
        assertEquals("/soc/framebuffer@30096000", second.getPath());
        assertFramebuffer(first, 480, "0x0 0x30000000 0x0 0x96000");
        assertFramebuffer(second, 400, "0x0 0x30096000 0x0 0x7d000");
        assertEquals(originalChosen, chosen.toString());
        assertEquals(1, occurrences(soc.toString(), "#address-cells ="));
        assertEquals(1, occurrences(soc.toString(), "#size-cells ="));
        assertEquals(1, occurrences(soc.toString(), "ranges;"));
        assertEquals(2, occurrences(root.toString(), "framebuffer@"));
    }

    private static SimpleFramebufferDevice framebuffer(final int width, final int height) {
        return new SimpleFramebufferDevice(
                width, height, ByteBuffer.allocate(width * height * SimpleFramebufferDevice.STRIDE));
    }

    private static void assertFramebuffer(
            final DeviceTree node, final int height, final String registerCells) {
        final String text = node.toString();
        assertTrue(text.contains("compatible = \"simple-framebuffer\";"), text);
        assertTrue(text.contains("reg = <" + registerCells + ">;"), text);
        assertTrue(text.contains("width = <0x280>;"), text);
        assertTrue(text.contains("height = <0x" + Integer.toHexString(height) + ">;"), text);
        assertTrue(text.contains("stride = <0x500>;"), text);
        assertTrue(text.contains("format = \"r5g6b5\";"), text);
        assertTrue(text.contains("status = \"okay\";"), text);
        assertEquals(1, occurrences(text, "reg ="));
        assertEquals(1, occurrences(text, "compatible ="));
    }

    private static int occurrences(final String text, final String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
