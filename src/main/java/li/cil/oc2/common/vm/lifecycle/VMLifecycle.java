package li.cil.oc2.common.vm.lifecycle;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.controller.AfterDeviceScanEvent;
import li.cil.oc2.common.bus.controller.DevicesChangedEvent;
import li.cil.oc2.common.bus.device.rpc.item.CPUItemDevice;
import li.cil.oc2.common.util.TickUtils;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.sedna.api.memory.MemoryAccessException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Optional;

public final class VMLifecycle {
    private static final Logger LOGGER = LogManager.getLogger();
    static final String STATE_TAG_NAME = "state";
    static final String RUNNER_TAG_NAME = "runner";
    private static final int DEVICE_LOAD_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));

    private final AbstractVirtualMachine vm;
    private int loadDevicesDelay;

    public VMLifecycle(final AbstractVirtualMachine vm) {
        this.vm = vm;
    }

    public void resetLoadDevicesDelay() {
        loadDevicesDelay = 0;
    }

    public void handleBeforeDeviceScan() {
        vm.state.rpcAdapter.pause();
        if (vm.runState == VMRunState.RUNNING) {
            vm.runState = VMRunState.LOADING_DEVICES;
        }
    }

    public void handleAfterDeviceScan(final AfterDeviceScanEvent event) {
        vm.state.rpcAdapter.resume(vm.busController, event.didDevicesChange());
    }

    public void handleDevicesAdded(final DevicesChangedEvent event) {
        joinWorkerThread();
        vm.state.vmAdapter.addDevices(event.devices());
    }

    public void handleDevicesRemoved(final DevicesChangedEvent event) {
        joinWorkerThread();
        vm.state.vmAdapter.removeDevices(event.devices());
    }

    public void load() {
        if (loadDevicesDelay > 0) {
            loadDevicesDelay--;
            return;
        }

        if (!vm.consumeEnergy(vm.busController.getEnergyConsumption(), true)) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY));
            return;
        }

        if (vm.busController.getDevices().stream().noneMatch(device -> device instanceof FirmwareLoader)) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_FIRMWARE));
            return;
        }

        if (vm.busController.getDevices().stream().noneMatch(device -> device instanceof CPUItemDevice)) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_CPU));
            return;
        } else {
            Optional<Device> cpu = vm.busController.getDevices().stream().filter(device -> device instanceof CPUItemDevice).findFirst();
            if(cpu.isEmpty()) {
                vm.error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_CPU));
                return;
            }
            vm.state.board.getCpu().setFrequency(((CPUItemDevice) cpu.get()).getFrequency());
        }

        assert vm.runner == null : "Runner active while still in load phase.";

        final VMDeviceLoadResult loadResult = vm.state.vmAdapter.mountDevices();
        if (!loadResult.wasSuccessful()) {
            if (loadResult.getErrorMessage() != null) {
                vm.error(loadResult.getErrorMessage(), false);
            } else {
                vm.error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN), false);
            }
            loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
            return;
        }

        if (vm.runner == null) {
            try {
                vm.state.board.reset();
                vm.state.board.initialize();
                vm.state.board.setRunning(true);
            } catch (final IllegalStateException e) {
                vm.error(Component.translatable(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
                return;
            } catch (final MemoryAccessException e) {
                LOGGER.error(e);
                vm.error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            }

            vm.runner = vm.createRunner();
        }

        vm.state.rpcAdapter.mountDevices();

        vm.setRunState(VMRunState.RUNNING);
    }

    public void run() {
        final Component runtimeError = vm.runner.getRuntimeError();
        if (runtimeError != null) {
            vm.error(runtimeError);
            return;
        }

        if (!vm.state.board.isRunning()) {
            stopRunnerAndReset();
            return;
        }

        if (!vm.consumeEnergy(vm.busController.getEnergyConsumption(), false)) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY));
            return;
        }

        vm.runner.tick();
    }

    public void stopRunnerAndReset() {
        joinWorkerThread();
        vm.setRunState(VMRunState.STOPPED);

        vm.state.board.setRunning(false);
        vm.state.board.reset();
        vm.state.rpcAdapter.reset();
        vm.state.rpcAdapter.disposeDevices();
        vm.state.vmAdapter.disposeDevices();

        vm.runner = null;
    }

    public void joinWorkerThread() {
        if (vm.runner != null) {
            vm.runner.join();
        }
    }

    public CompoundTag serialize() {
        joinWorkerThread();
        return VMLifecycleSerialization.serialize(vm);
    }

    public void deserialize(final CompoundTag tag) {
        joinWorkerThread();
        VMLifecycleSerialization.deserialize(vm, tag);
    }
}
