package li.cil.oc2.common.bus.device.vm.block;

import java.util.Set;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.vm.item.misc.GPUDevice;

/**
 * Links GPUs on a computer's bus to the monitors connected to the same bus.
 *
 * <p>Called from after-device-scan listeners: every scan, the first GPU found in the device
 * set defines the framebuffer resolution for all connected {@link MonitorDevice}s. Without
 * a GPU, monitors mount without a framebuffer (dark screen). Resolution changes propagate
 * through the regular device-change soft restart (see AbstractVirtualMachine), so the next
 * mount always uses fresh values.
 */
public final class MonitorGpuLink {
    private MonitorGpuLink() {}

    public static void updateMonitors(final Set<Device> devices) {
        final GPUDevice gpu = findGpu(devices);
        for (final Device device : devices) {
            if (device instanceof MonitorDevice monitor) {
                if (gpu != null) {
                    monitor.setGpuResolution(gpu.getWidth(), gpu.getHeight());
                } else {
                    monitor.clearGpuResolution();
                }
            }
        }
    }

    @Nullable
    private static GPUDevice findGpu(final Set<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof GPUDevice gpu) {
                return gpu;
            }
        }
        return null;
    }
}
