package li.cil.oc2.common.entity.robot.movement;

import java.util.Queue;
import javax.annotation.Nullable;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.AbstractRobotAction;
import li.cil.oc2.common.entity.robot.action.RobotActionResult;
import li.cil.oc2.common.entity.robot.action.processor.RobotActionProcessor;
import li.cil.oc2.common.entity.robot.action.processor.RobotActionProcessorResult;
import li.cil.oc2.common.entity.robot.rotation.RobotRotationAction;
import li.cil.oc2.common.entity.robot.rotation.RotationDirection;
import net.minecraft.nbt.CompoundTag;

public class RobotMovementController {

    private final RobotActionProcessor actionProcessor;

    public RobotMovementController(final Robot robot) {
        this.actionProcessor = new RobotActionProcessor(robot);
    }

    public void tick() {
        actionProcessor.tick();
    }

    public boolean hasQueuedActions() {
        return actionProcessor.hasQueuedActions();
    }

    public void clear() {
        actionProcessor.clear();
    }

    public int getQueuedActionCount() {
        return actionProcessor.getQueuedActionCount();
    }

    public boolean move(final MovementDirection direction) {
        return actionProcessor.addAction(new RobotMovementAction(direction));
    }

    public boolean rotate(final RotationDirection direction) {
        return actionProcessor.addAction(new RobotRotationAction(direction));
    }

    public int getLastActionId() {
        return actionProcessor.getLastActionId();
    }

    @Nullable
    public AbstractRobotAction getCurrentAction() {
        return actionProcessor.getCurrentAction();
    }

    public Queue<AbstractRobotAction> getQueue() {
        return actionProcessor.getQueue();
    }

    public Queue<RobotActionProcessorResult> getResults() {
        return actionProcessor.getResults();
    }

    public CompoundTag serialize() {
        return actionProcessor.serialize();
    }

    public void deserialize(final CompoundTag tag) {
        actionProcessor.deserialize(tag);
    }

    @Nullable
    public RobotActionResult findActionResult(final int actionId) {
        return actionProcessor.findActionResult(actionId);
    }
}