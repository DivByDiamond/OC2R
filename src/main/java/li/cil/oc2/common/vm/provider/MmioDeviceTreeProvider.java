package li.cil.oc2.common.vm.provider;

import java.util.Optional;
import li.cil.sedna.api.device.Device;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.api.devicetree.DeviceTreeProvider;
import li.cil.sedna.api.memory.MemoryMap;

/**
 * Registers a memory-mapped device in the device tree under {@code /soc} as a
 * {@code <name>@<addr>} node with a {@code reg} property.
 *
 * <p>sedna's stock {@code UART16550AProvider} and {@code VirtIOProvider}
 * implement {@link DeviceTreeProvider} but not {@code createNode} — the
 * interface default returns {@code Optional.empty()} — so without this the
 * generated device tree has no uart/virtio nodes and the guest kernel cannot
 * discover the console or the block devices.
 */
public final class MmioDeviceTreeProvider implements DeviceTreeProvider {
    private final String name;
    private final String compatible;

    public MmioDeviceTreeProvider(final String name, final String compatible) {
        this.name = name;
        this.compatible = compatible;
    }

    @Override
    public Optional<String> getName(final Device device) {
        return Optional.of(name);
    }

    @Override
    public Optional<DeviceTree> createNode(
            final DeviceTree root,
            final MemoryMap memoryMap,
            final Device device,
            final String deviceName) {
        final DeviceTree soc = root.find("/soc");
        if (soc == null) {
            return Optional.empty();
        }
        return memoryMap
                .getMemoryRange((MemoryMappedDevice) device)
                .map(r -> soc.getChild(name, r.address()));
    }

    @Override
    public void visit(
            final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        node.addProp("compatible", compatible);
        memoryMap
                .getMemoryRange((MemoryMappedDevice) device)
                .ifPresent(r -> node.addProp("reg", r.address(), r.size()));
    }
}
