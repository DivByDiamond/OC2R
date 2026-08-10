package li.cil.oc2.common.vm.provider;

import li.cil.oc2.common.vm.device.PciRootPortDevice;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.AbstractVirtIODevice;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.devicetree.provider.GoldfishRTCProvider;
import li.cil.sedna.riscv.device.R5CoreLocalInterrupter;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;
import li.cil.sedna.riscv.devicetree.R5CoreLocalInterrupterProvider;
import li.cil.sedna.riscv.devicetree.R5PlatformLevelInterruptControllerProvider;

public final class DeviceTreeProviders {
    public static void initialize() {
        DeviceTreeRegistry.putProvider(
                SimpleFramebufferDevice.class, new SimpleFramebufferDeviceProvider());
        DeviceTreeRegistry.putProvider(PciRootPortDevice.class, new PciRootPortDeviceProvider());
        // sedna's UART16550AProvider / VirtIOProvider don't implement createNode
        // (interface default returns empty) — without these the device tree has no
        // uart/virtio nodes and the guest kernel can't discover the console or the
        // block devices. GoldfishRTCProvider and the R5 providers do implement it.
        DeviceTreeRegistry.putProvider(
                AbstractVirtIODevice.class, new MmioDeviceTreeProvider("virtio", "virtio,mmio"));
        DeviceTreeRegistry.putProvider(
                UART16550A.class, new MmioDeviceTreeProvider("uart", "ns16550a"));
        DeviceTreeRegistry.putProvider(GoldfishRTC.class, new GoldfishRTCProvider());
        DeviceTreeRegistry.putProvider(
                R5CoreLocalInterrupter.class, new R5CoreLocalInterrupterProvider());
        DeviceTreeRegistry.putProvider(
                R5PlatformLevelInterruptController.class,
                new R5PlatformLevelInterruptControllerProvider());
    }
}
