package li.cil.oc2.common.vm.provider;

import java.util.Optional;
import li.cil.oc2.common.vm.device.frame.FramePresentDevice;
import li.cil.sedna.api.device.Device;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.api.devicetree.DeviceTreeProvider;
import li.cil.sedna.api.memory.MemoryMap;

/** Describes the optional presentation aperture and its framebuffer association. */
public final class FramePresentDeviceProvider implements DeviceTreeProvider {
    @Override
    public Optional<String> getName(final Device device) { return Optional.of("frame-present"); }

    @Override
    public Optional<DeviceTree> createNode(final DeviceTree root, final MemoryMap memory,
                                           final Device device, final String name) {
        return memory.getMemoryRange((FramePresentDevice) device)
                .map(range -> root.find("/soc").getChild(name, range.address()));
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memory, final Device device) {
        final FramePresentDevice present = (FramePresentDevice) device;
        node.addProp("compatible", "oc2r,frame-present-v1").addProp("status", "okay");
        memory.getMemoryRange(present.framebuffer()).ifPresent(range ->
                node.addProp("framebuffer-base", range.address()));
    }
}
