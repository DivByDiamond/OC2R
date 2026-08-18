package li.cil.oc2.common.vm.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.context.managed.ManagedVMContext;
import li.cil.oc2.common.vm.memory.BaseAddressProvider;

public final class VMDeviceBusAdapter {
    private final Map<VMDevice, ManagedVMContext> mountedDevices = new ConcurrentHashMap<>();
    private final List<VMDevice> unmountedDevices = new ArrayList<>();
    private BaseAddressProvider baseAddressProvider = unused -> OptionalLong.empty();

    private final GlobalVMContext globalContext;

    public VMDeviceBusAdapter(final GlobalVMContext context) {
        this.globalContext = context;
    }

    public void setBaseAddressProvider(final BaseAddressProvider provider) {
        baseAddressProvider = provider;
    }

    public VMDeviceLoadResult mountDevices() {
        for (final VMDevice device : unmountedDevices) {
            // NOPMD - each device requires its own ManagedVMContext.
            final ManagedVMContext context =
                    new ManagedVMContext(// NOPMD allocation depends on loop iteration / per-item state
                            globalContext,
                            globalContext,
                            () -> baseAddressProvider.getBaseAddress(device));

            final VMDeviceLoadResult result = device.mount(context);
            context.freeze();

            if (!result.wasSuccessful()) {
                context.invalidate();
                mountedDevices.forEach(
                        (mountedDevice, mountedContext) -> {
                            mountedDevice.unmount();
                            mountedContext.invalidate();
                        });
                mountedDevices.clear();
                return result;
            }

            mountedDevices.put(device, context);
        }

        unmountedDevices.clear();

        globalContext.updateReservations();

        return VMDeviceLoadResult.success();
    }

    public void unmountDevices() {
        mountedDevices.forEach(
                (device, context) -> {
                    device.unmount();
                    context.invalidate();
                });

        unmountedDevices.addAll(mountedDevices.keySet());
        mountedDevices.clear();
    }

    public void disposeDevices() {
        unmountDevices();

        unmountedDevices.forEach(VMDevice::dispose);
    }

    public void addDevices(final Collection<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof final VMDevice vmDevice && !mountedDevices.containsKey(vmDevice)) {
                // Add to set of unmounted devices if we don't already track it. It's a set, so
                // there won't be duplicates in the unmounted set due to this.
                unmountedDevices.add(vmDevice);
            }
        }
    }

    public void removeDevices(final Collection<Device> devices) {
        for (final Device device : devices) {
            if (device instanceof final VMDevice vmDevice) {
                final ManagedVMContext context = mountedDevices.remove(vmDevice);
                if (context != null) {
                    vmDevice.unmount();
                    context.invalidate();
                } else {
                    unmountedDevices.remove(vmDevice);
                }
            }
        }
    }
}