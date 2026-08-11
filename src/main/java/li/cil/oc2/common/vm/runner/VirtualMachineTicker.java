package li.cil.oc2.common.vm.runner;

import java.util.concurrent.atomic.AtomicBoolean;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.vm.VMRunState;

final class VirtualMachineTicker {
    private static final int DEVICE_CHANGE_RESTART_DELAY = 2;

    private final AbstractVirtualMachine vm;
    private final AtomicBoolean devicesChangedWhileRunning = new AtomicBoolean();
    private int deviceChangeRestartDelay;

    VirtualMachineTicker(final AbstractVirtualMachine vm) {
        this.vm = vm;
    }

    void markDevicesChanged() {
        devicesChangedWhileRunning.set(true);
    }

    void tick() {
        vm.busController.scan();
        vm.setBusState(vm.busController.getState());
        if (vm.busState != BusState.READY) return;

        if (vm.state.board.isRestarting()) {
            vm.stop();
            vm.start();
        }

        // If the device set changed while the VM was running, schedule a soft
        // restart so the guest re-enumerates hardware on the next boot. Defer a
        // couple of ticks so that multiple devices connected at the same time
        // (e.g. a monitor together with a keyboard) only cause a single restart.
        // The atomic flag protects against races between the scan callback and
        // this tick. Devices that can be hot-plugged (RPC devices) are handled
        // live by the RPC adapter and do not reach this point.
        if (devicesChangedWhileRunning.getAndSet(false)) {
            if (vm.runState == VMRunState.RUNNING && vm.state.board.isRunning()) {
                deviceChangeRestartDelay = DEVICE_CHANGE_RESTART_DELAY;
            }
        }
        if (deviceChangeRestartDelay > 0) {
            deviceChangeRestartDelay--;
            if (deviceChangeRestartDelay == 0 && vm.state.board.isRunning()) {
                vm.stop();
                vm.start();
            }
        }

        if (vm.runState == VMRunState.LOADING_DEVICES) {
            vm.lifecycle.load();
        } else if (vm.runState == VMRunState.RUNNING) {
            vm.lifecycle.run();
        }
    }
}
