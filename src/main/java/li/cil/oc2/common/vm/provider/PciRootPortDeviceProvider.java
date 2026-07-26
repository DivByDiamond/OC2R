/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.provider;

import li.cil.oc2.common.vm.device.PciRootPortDevice;
import li.cil.sedna.api.device.Device;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.devicetree.DeviceNames;
import li.cil.sedna.api.devicetree.DevicePropertyNames;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.api.devicetree.DeviceTreeProvider;
import li.cil.sedna.api.memory.MappedMemoryRange;
import li.cil.sedna.api.memory.MemoryMap;

import java.util.Optional;

public final class PciRootPortDeviceProvider implements DeviceTreeProvider {
    @Override
    public Optional<String> getName(final Device device) {
        return Optional.of("pci");
    }

    @Override
    public Optional<DeviceTree> createNode(final DeviceTree root, final MemoryMap memoryMap, final Device device, final String deviceName) {
        final Optional<MappedMemoryRange> range = memoryMap.getMemoryRange((MemoryMappedDevice) device);
        return range.map(r -> {
            final DeviceTree pci = root.find("/pci");
            return pci.getChild(deviceName, r.address());
        });
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        final PciRootPortDevice pr = (PciRootPortDevice) device;
        final Optional<MappedMemoryRange> range = memoryMap.getMemoryRange((MemoryMappedDevice) device);
        node
            .addProp(DevicePropertyNames.COMPATIBLE, "pci-host-cam-generic")
            .addProp(DevicePropertyNames.DEVICE_TYPE, DeviceNames.PCI)
            .addProp(DevicePropertyNames.NUM_ADDRESS_CELLS,3)
            .addProp(DevicePropertyNames.NUM_SIZE_CELLS, 2)
            .addProp("bus-range", 0, 1)
            //.addProp("linux,pci-probe-only", 1)
            .addProp(DevicePropertyNames.RANGES,
                //          type       pci.hi      pci.lo      cpu.hi       cpu.lo     len.hi      len.lo
                0x02000000, 0x00000000, 0x40000000, 0x00000000, 0x40000000, 0x00000000, 0x20000000); //
    }
}

