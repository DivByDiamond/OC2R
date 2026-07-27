package li.cil.oc2.common.vm;

import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.lifecycle.VMLifecycle;
import li.cil.oc2.common.vm.state.SerializedState;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class AbstractVirtualMachine implements VirtualMachine {
    public static final String BUS_STATE_TAG_NAME = "busState";
    public static final String RUN_STATE_TAG_NAME = "runState";
    public static final String BOOT_ERROR_TAG_NAME = "bootError";

    public final CommonDeviceBusController busController;
    private BusState busState = BusState.SCAN_PENDING;

    public final SerializedState state = new SerializedState();
    public AbstractTerminalVMRunner runner;
    public VMRunState runState = VMRunState.STOPPED;
    @Nullable public Component bootError;

    final VMLifecycle lifecycle;

    public AbstractVirtualMachine(final CommonDeviceBusController busController) {
        this.busController = busController;
        this.bootError = Component.literal("");

        lifecycle = new VMLifecycle(this);
        busController.onBeforeDeviceScan.add(lifecycle::handleBeforeDeviceScan);
        busController.onAfterDeviceScan.add(lifecycle::handleAfterDeviceScan);
        busController.onDevicesAdded.add(lifecycle::handleDevicesAdded);
        busController.onDevicesRemoved.add(lifecycle::handleDevicesRemoved);

        state.board = new R5Board();
        state.context = new GlobalVMContext(state.board);
        state.builtinDevices = new BuiltinDevices(state.context);
        state.rpcAdapter = new RPCDeviceBusAdapter(state.builtinDevices.rpcSerialDevice);
        state.vmAdapter = new VMDeviceBusAdapter(state.context);

        state.board.getCpu().setFrequency(Constants.CPU_FREQUENCY);
        state.board.setBootArguments("root=/dev/vda rw");
        state.board.setStandardOutputDevice(state.builtinDevices.uart);
    }

    public void dispose() {
        lifecycle.joinWorkerThread();
        state.context.invalidate();
        busController.dispose();
    }

    public void suspend() {
        lifecycle.joinWorkerThread();
        state.vmAdapter.unmountDevices();
        state.rpcAdapter.unmountDevices();
    }

    @Override
    public boolean isRunning() {
        return getBusState() == BusState.READY && getRunState() == VMRunState.RUNNING;
    }

    @Override
    public BusState getBusState() {
        return busState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBusStateClient(final BusState value) {
        busState = value;
    }

    @Override
    public VMRunState getRunState() {
        return runState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setRunStateClient(final VMRunState value) {
        runState = value;
    }

    @Override
    @Nullable
    public Component getBootError() {
        return bootError;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBootErrorClient(@Nullable final Component value) {
        bootError = value;
    }

    @Override
    @Nullable
    public Component getError() {
        switch (busState) {
            case SCAN_PENDING:
            case INCOMPLETE:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_INCOMPLETE);
            case TOO_COMPLEX:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_TOO_COMPLEX);
            case MULTIPLE_CONTROLLERS:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS);
            case READY:
                switch (runState) {
                    case STOPPED:
                    case LOADING_DEVICES:
                        return bootError;
                }
                break;
        }
        return null;
    }

    @Override
    public void start() {
        if (runState == VMRunState.RUNNING) return;

        setBootError(Component.literal(""));
        setRunState(VMRunState.LOADING_DEVICES);
        lifecycle.resetLoadDevicesDelay();
    }

    @Override
    public void stop() {
        lifecycle.stopRunnerAndReset();
    }

    public void tick() {
        busController.scan();
        setBusState(busController.getState());
        if (busState != BusState.READY) return;

        if (state.board.isRestarting()) {
            stop();
            start();
        }

        switch (runState) {
            case LOADING_DEVICES -> lifecycle.load();
            case RUNNING -> lifecycle.run();
        }
    }

    public CompoundTag serialize() {
        return lifecycle.serialize();
    }

    public void deserialize(final CompoundTag tag) {
        lifecycle.deserialize(tag);
    }

    public abstract AbstractTerminalVMRunner createRunner();

    public abstract boolean consumeEnergy(int amount, boolean simulate);

    protected void handleBusStateChanged(final BusState value) {}

    protected void handleRunStateChanged(final VMRunState value) {}

    protected void handleBootErrorChanged(@Nullable final Component value) {}

    public void error(@Nullable final Component message) {
        error(message, true);
    }

    public void error(@Nullable Component message, final boolean reset) {
        if (reset) stopRunnerAndReset();
        if (message == null) message = Component.literal("");
        setBootError(message);
    }

    protected void stopRunnerAndReset() {
        lifecycle.stopRunnerAndReset();
    }

    private void setBusState(final BusState value) {
        if (value == busState) return;
        busState = value;
        handleBusStateChanged(busState);
    }

    public void setRunState(final VMRunState value) {
        if (value == runState) return;
        runState = value;
        handleRunStateChanged(value);
    }

    private void setBootError(final Component value) {
        bootError = value;
        handleBootErrorChanged(value);
    }
}