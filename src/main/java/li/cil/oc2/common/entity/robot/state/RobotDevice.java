package li.cil.oc2.common.entity.robot.state;

import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActionResult;
import li.cil.oc2.common.entity.robot.movement.MovementDirection;
import li.cil.oc2.common.entity.robot.rotation.RotationDirection;
import net.minecraft.world.item.ItemStack;

public final class RobotDevice {
    private final Robot robot;

    public RobotDevice(final Robot robot) {
        this.robot = robot;
    }

    @Callback(synchronize = false)
    public int getEnergyStored() {
        return robot.getEnergyStorage().getEnergyStored();
    }

    @Callback(synchronize = false)
    public int getEnergyCapacity() {
        return robot.getEnergyStorage().getMaxEnergyStored();
    }

    @Callback(synchronize = false)
    public int getSelectedSlot() {
        return robot.getSelectedSlot();
    }

    @Callback(synchronize = false)
    public void setSelectedSlot(@Parameter("slot") final int slot) {
        robot.setSelectedSlot(slot);
    }

    @Callback
    public ItemStack getStackInSlot(@Parameter("slot") final int slot) {
        return robot.getInventory().getStackInSlot(slot);
    }

    @Callback(synchronize = false)
    public boolean move(@Parameter("direction") @Nullable final MovementDirection direction) {
        if (direction == null) throw new IllegalArgumentException();
        return robot.getMovementController().move(direction);
    }

    @Callback(synchronize = false)
    public boolean turn(@Parameter("direction") @Nullable final RotationDirection direction) {
        if (direction == null) throw new IllegalArgumentException();
        return robot.getMovementController().rotate(direction);
    }

    @Callback(synchronize = false)
    public int getLastActionId() {
        return robot.getMovementController().getLastActionId();
    }

    @Callback(synchronize = false)
    public int getQueuedActionCount() {
        return robot.getMovementController().getQueuedActionCount();
    }

    @Nullable
    @Callback(synchronize = false)
    public RobotActionResult getActionResult(@Parameter("actionId") final int actionId) {
        return robot.getMovementController().findActionResult(actionId);
    }
}