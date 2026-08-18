package li.cil.oc2.common.vm.lifecycle;

import java.time.Duration;
import java.util.Optional;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.controller.event.AfterDeviceScanEvent;
import li.cil.oc2.common.bus.controller.event.DevicesChangedEvent;
import li.cil.oc2.common.bus.device.rpc.item.CPUItemDevice;
import li.cil.oc2.common.util.tick.TickUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.sedna.api.memory.MemoryAccessException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    }

    public void handleAfterDeviceScan(final AfterDeviceScanEvent event) {
        vm.state.rpcAdapter.resume(vm.busController, event.didDevicesChange());
    }

    public void handleDevicesAdded(final DevicesChangedEvent event) {
        joinWorkerThread();
        vm.state.vmAdapter.addDevices(event.devices());
        // Deferred mount: on a running VM the added devices are not mounted
        // until the VM is restarted (see AbstractVirtualMachine.tick), which
        // lets the guest re-enumerate hardware on the next boot.
        vm.markDevicesChanged();
    }

    public void handleDevicesRemoved(final DevicesChangedEvent event) {
        joinWorkerThread();
        vm.state.vmAdapter.removeDevices(event.devices());
        vm.markDevicesChanged();
    }

    public void load() {
        if (loadDevicesDelay > 0) {
            loadDevicesDelay--;
            return;
        }

        if (!hasRequiredDevicesAndEnergy()) {
            return;
        }

        assert vm.runner == null : "Runner active while still in load phase.";

        if (!mountDevices()) {
            return;
        }

        if (vm.runner == null) {
            if (!startBoard()) {
                return;
            }
            vm.runner = vm.createRunner();
        }

        vm.state.rpcAdapter.mountDevices();

        vm.setRunState(VMRunState.RUNNING);
    }

    private boolean hasRequiredDevicesAndEnergy() {
        if (!vm.consumeEnergy(vm.busController.getEnergyConsumption(), true)) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY));
            return false;
        }

        if (vm.busController.getDevices().stream()
                .noneMatch(device -> device instanceof FirmwareLoader)) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_FIRMWARE));
            return false;
        }

        final Optional<CPUItemDevice> cpu =
                vm.busController.getDevices().stream()
                        .filter(device -> device instanceof CPUItemDevice)
                        .map(device -> (CPUItemDevice) device)
                        .findFirst();
        if (cpu.isEmpty()) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_CPU));
            return false;
        }
        vm.state.board.getCpu().setFrequency(cpu.get().getFrequency());
        return true;
    }

    private boolean mountDevices() {
        final VMDeviceLoadResult loadResult = vm.state.vmAdapter.mountDevices();
        if (loadResult.wasSuccessful()) {
            return true;
        }
        if (loadResult.getErrorMessage() != null) {
            vm.error(loadResult.getErrorMessage(), false);
        } else {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN), false);
        }
        loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
        return false;
    }

    private boolean startBoard() {
        try {
            vm.state.board.reset();
            vm.state.board.initialize();
            vm.state.board.setRunning(true);
            return true;
        } catch (final IllegalStateException e) {
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
            return false;
        } catch (final MemoryAccessException e) {
            LOGGER.error(e);
            vm.error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
            return false;
        }
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