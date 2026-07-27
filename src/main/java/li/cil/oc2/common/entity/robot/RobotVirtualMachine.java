package li.cil.oc2.common.entity.robot;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.*;
import li.cil.oc2.common.util.TerminalUtils;
import li.cil.oc2.common.vm.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.terminal.Terminal;
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
                output ->
                        Network.sendToClientsTrackingEntity(
                                new RobotTerminalOutputMessage(robot, output), robot));

        movementController.clear();
    }

    @Override
    public AbstractTerminalVMRunner createRunner() {
        return new RobotVMRunner(this, terminal);
    }

    @Override
    protected void handleBusStateChanged(final BusState value) {
        Network.sendToClientsTrackingEntity(new RobotBusStateMessage(robot, value), robot);
    }

    @Override
    protected void handleRunStateChanged(final VMRunState value) {
        Network.sendToClientsTrackingEntity(new RobotRunStateMessage(robot, value), robot);
    }

    @Override
    protected void handleBootErrorChanged(@Nullable Component value) {
        if (value == null) {
            value = Component.literal("");
        }
        Network.sendToClientsTrackingEntity(new RobotBootErrorMessage(robot, value), robot);
    }

    private final class RobotVMRunner extends AbstractTerminalVMRunner {
        public RobotVMRunner(final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
            super(virtualMachine, terminal);
        }

        @Override
        protected void sendTerminalUpdateToClient(final ByteBuffer output) {
            Network.sendToClientsTrackingEntity(
                    new RobotTerminalOutputMessage(RobotVirtualMachine.this.robot, output),
                    RobotVirtualMachine.this.robot);
        }
    }
}