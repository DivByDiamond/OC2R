package li.cil.oc2.common.vm.provider;

import li.cil.oc2.common.vm.device.PciRootPortDevice;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.oc2.common.vm.device.frame.FramePresentDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.AbstractVirtIODevice;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.devicetree.provider.GoldfishRTCProvider;
import li.cil.sedna.devicetree.provider.SystemControllerProvider;
import li.cil.sedna.riscv.device.R5CoreLocalInterrupter;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;
import li.cil.sedna.riscv.device.R5SystemController;
import li.cil.sedna.riscv.devicetree.R5CoreLocalInterrupterProvider;
import li.cil.sedna.riscv.devicetree.R5PlatformLevelInterruptControllerProvider;

public final class DeviceTreeProviders {
    public static void initialize() {
        DeviceTreeRegistry.putProvider(
                SimpleFramebufferDevice.class, new SimpleFramebufferDeviceProvider());
        DeviceTreeRegistry.putProvider(FramePresentDevice.class, new FramePresentDeviceProvider());
        DeviceTreeRegistry.putProvider(PciRootPortDevice.class, new PciRootPortDeviceProvider());
        // sedna's UART16550AProvider / VirtIOProvider don't implement createNode
        // (interface default returns empty) — without these the device tree has no
        // uart/virtio nodes and the guest kernel can't discover the console or the
        // block devices. GoldfishRTCProvider and the R5 providers do implement it.
        DeviceTreeRegistry.putProvider(
                AbstractVirtIODevice.class, new MmioDeviceTreeProvider("virtio", "virtio,mmio"));
        DeviceTreeRegistry.putProvider(
                UART16550A.class, new MmioDeviceTreeProvider("uart", "ns16550a", 3686400));
        DeviceTreeRegistry.putProvider(GoldfishRTC.class, new GoldfishRTCProvider());
        DeviceTreeRegistry.putProvider(
                R5CoreLocalInterrupter.class, new R5CoreLocalInterrupterProvider());
        DeviceTreeRegistry.putProvider(
                R5PlatformLevelInterruptController.class,
                new R5PlatformLevelInterruptControllerProvider());
        // Sedna 5.2.0 maps a system controller onto every R5 board. Publishing it (with the
        // syscon-reboot/syscon-poweroff nodes) lets guest firmware and kernels that know the
        // binding reboot and power off through it; guests that do not simply ignore the node.
        // Note: the host-driven requestPowerOff/requestReset calls stay unused on purpose --
        // no shipped guest enables them yet (CONTROL_ENABLE is guest-owned) and the syscon
        // interrupt is not routed, so a request would never be observed. Revisit once a guest
        // implements the host-request side.
        DeviceTreeRegistry.putProvider(R5SystemController.class, new SystemControllerProvider());
    }
}
