package li.cil.oc2.common.vm.provider;

import java.util.Optional;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.sedna.api.device.Device;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.devicetree.DevicePropertyNames;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.api.devicetree.DeviceTreeProvider;
import li.cil.sedna.api.memory.MappedMemoryRange;
import li.cil.sedna.api.memory.MemoryMap;

public final class SimpleFramebufferDeviceProvider implements DeviceTreeProvider {
    @Override
    public Optional<String> getName(final Device device) {
        return Optional.of("framebuffer");
    }

    @Override
    public Optional<DeviceTree> createNode(
            final DeviceTree root,
            final MemoryMap memoryMap,
            final Device device,
            final String deviceName) {
        final Optional<MappedMemoryRange> range =
                memoryMap.getMemoryRange((MemoryMappedDevice) device);
        return range.map(
                r -> {
                    // These are independently mapped display devices, not a single boot
                    // display. Linux only instantiates the first simple-framebuffer under
                    // /chosen; publishing them on the board's existing simple-bus lets it
                    // enumerate every monitor/projector. R5Board already supplies /soc's
                    // address/size cells and identity ranges, so leave shared properties alone.
                    return root.find("/soc").getChild(deviceName, r.address());
                });
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        final SimpleFramebufferDevice fb = (SimpleFramebufferDevice) device;
        node.addProp(DevicePropertyNames.COMPATIBLE, "simple-framebuffer")
                .addProp("width", fb.getWidth())
                .addProp("height", fb.getHeight())
                .addProp("stride", fb.getWidth() * SimpleFramebufferDevice.STRIDE)
                .addProp("format", "r5g6b5")
                .addProp(DevicePropertyNames.STATUS, "okay");
    }
}
