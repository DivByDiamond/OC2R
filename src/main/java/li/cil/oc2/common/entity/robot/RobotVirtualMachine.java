package li.cil.oc2.common.entity.robot;

import javax.annotation.Nullable;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.movement.RobotMovementController;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.robot.RobotBootErrorMessage;
import li.cil.oc2.common.network.message.robot.RobotBusStateMessage;
import li.cil.oc2.common.network.message.robot.state.RobotRunStateMessage;
import li.cil.oc2.common.network.message.robot.terminal.RobotTerminalDiffMessage;
import li.cil.oc2.common.util.tick.TerminalUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import net.minecraft.network.chat.Component;

public final class RobotVirtualMachine extends AbstractVirtualMachine {
    private final Robot robot;
    private final Terminal terminal;
    private final RobotMovementController movementController;

    public RobotVirtualMachine(
            final Robot robot,
            final CommonDeviceBusController busController,
            final Terminal terminal,
            final RobotMovementController movementController) {
        super(busController);
        this.robot = robot;
        this.terminal = terminal;
        this.movementController = movementController;
        state.vmAdapter.setBaseAddressProvider(
                robot.getRobotInventory().getDeviceItems()::getDeviceAddressBase);
    }

    @Override
    public boolean consumeEnergy(final int amount, final boolean simulate) {
        if (!Config.robotsUseEnergy()) {
            return true;
        }

        if (amount > robot.getEnergyStorage().getEnergyStored()) {
            return false;
        }

        robot.getEnergyStorage().extractEnergy(amount, simulate);
        return true;
    }

    @Override
    protected void stopRunnerAndReset() {
        super.stopRunnerAndReset();

        TerminalUtils.resetTerminal(
                terminal,
                snapshot ->
                        NetworkMessages.sendToClientsTrackingEntity(
                                new RobotTerminalDiffMessage(robot, snapshot), robot));

        movementController.clear();
    }

    @Override
    public AbstractTerminalVMRunner createRunner() {
        return new RobotVMRunner(this, terminal);
    }

    @Override
    protected void handleBusStateChanged(final BusState value) {
        NetworkMessages.sendToClientsTrackingEntity(new RobotBusStateMessage(robot, value), robot);
    }

    @Override
    protected void handleRunStateChanged(final VMRunState value) {
        NetworkMessages.sendToClientsTrackingEntity(new RobotRunStateMessage(robot, value), robot);
    }

    @Override
    protected void handleBootErrorChanged(@Nullable final Component value) {
        final Component effective = value == null ? Component.literal("") : value;
        NetworkMessages.sendToClientsTrackingEntity(new RobotBootErrorMessage(robot, effective), robot);
    }

    private final class RobotVMRunner extends AbstractTerminalVMRunner {
        public RobotVMRunner(final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
            super(virtualMachine, terminal);
        }

        @Override
        protected void sendTerminalDiffToClient(final TerminalDiff.Snapshot snapshot) {
            NetworkMessages.sendToClientsTrackingEntity(
                    new RobotTerminalDiffMessage(RobotVirtualMachine.this.robot, snapshot),
                    RobotVirtualMachine.this.robot);
        }
    }
}