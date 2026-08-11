package li.cil.oc2.common.container.robot;

import java.nio.ByteBuffer;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.base.AbstractMachineContainer;
import li.cil.oc2.common.container.base.AbstractMachineTerminalContainer;
import li.cil.oc2.common.container.data.IntPrecisionContainerData;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.robot.inventory.OpenRobotInventoryMessage;
import li.cil.oc2.common.network.message.robot.state.RobotPowerMessage;
import li.cil.oc2.common.network.message.robot.terminal.OpenRobotTerminalMessage;
import li.cil.oc2.common.network.message.robot.terminal.RobotTerminalInputMessage;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public abstract class AbstractRobotContainer extends AbstractMachineTerminalContainer {
    private final Robot robot;
    private static boolean captureInputState = Config.captureInputDefaultState;

    public AbstractRobotContainer(
            final MenuType<?> type,
            final int id,
            final Player player,
            final Robot robot,
            final IntPrecisionContainerData energyInfo) {
        super(type, id, energyInfo);
        this.robot = robot;

        this.robot.addTerminalUser(player);
    }

    @Override
    public void switchToInventory() {
        NetworkMessages.sendToServer(new OpenRobotInventoryMessage(robot));
    }

    @Override
    public void switchToTerminal() {
        NetworkMessages.sendToServer(new OpenRobotTerminalMessage(robot));
    }

    public Robot getRobot() {
        return robot;
    }

    @Override
    public VirtualMachine getVirtualMachine() {
        return robot.getVirtualMachine();
    }

    @Override
    public void sendPowerStateToServer(final boolean value) {
        NetworkMessages.sendToServer(new RobotPowerMessage(robot, value));
    }

    @Override
    public Terminal getTerminal() {
        return robot.getTerminal();
    }

    @Override
    public boolean getCaptureInputState() {
        return switch (Config.captureInputMode) {
            case PER_BLOCK -> robot.getCaptureInputState();
            case SHARED_BETWEEN_TYPE -> captureInputState;
            case GLOBAL_CAPTURE -> ClientSetup.getCaptureInputState();
            default -> throw new AssertionError(Config.captureInputMode);
        };
    }

    @Override
    public void setCaptureInputState(final boolean state) {
        switch (Config.captureInputMode) {
            case PER_BLOCK -> robot.setCaptureInputState(state);
            case SHARED_BETWEEN_TYPE -> captureInputState = state;
            case GLOBAL_CAPTURE -> ClientSetup.setCaptureInputState(state);
            default -> throw new AssertionError(Config.captureInputMode);
        }
    }

    @Override
    public void sendTerminalInputToServer(final ByteBuffer input) {
        NetworkMessages.sendToServer(new RobotTerminalInputMessage(robot, input));
    }

    @Override
    public boolean stillValid(final Player player) {
        return robot.isAlive() && robot.closerThan(player, 8);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);

        this.robot.removeTerminalUser(player);
    }

    protected static IntPrecisionContainerData createEnergyInfo(
            final FixedEnergyStorage energy, final CommonDeviceBusController busController) {
        return new IntPrecisionContainerData.Server() {
            @Override
            public int getInt(final int index) {
                return switch (index) {
                    case AbstractMachineContainer.ENERGY_STORED_INDEX -> energy.getEnergyStored();
                    case AbstractMachineContainer.ENERGY_CAPACITY_INDEX ->
                            energy.getMaxEnergyStored();
                    case AbstractMachineContainer.ENERGY_CONSUMPTION_INDEX ->
                            busController.getEnergyConsumption();
                    default -> 0;
                };
            }

            @Override
            public int getIntCount() {
                return ENERGY_INFO_SIZE;
            }
        };
    }
}