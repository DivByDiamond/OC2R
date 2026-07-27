package li.cil.oc2.common.entity.robot;

import java.util.Queue;
import javax.annotation.Nullable;
import li.cil.oc2.common.entity.Robot;
import net.minecraft.nbt.CompoundTag;

public class RobotMovementController {
    private final Robot robot;
    private final RobotActionProcessor actionProcessor;

    public RobotMovementController(final Robot robot) {
        this.robot = robot;
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
        return actionProcessor.lastActionId;
    }

    @Nullable
    public AbstractRobotAction getCurrentAction() {
        return actionProcessor.action;
    }

    public Queue<AbstractRobotAction> getQueue() {
        return actionProcessor.queue;
    }

    public Queue<RobotActionProcessorResult> getResults() {
        return actionProcessor.results;
    }

    public CompoundTag serialize() {
        return actionProcessor.serialize();
    }

    public void deserialize(final CompoundTag tag) {
        actionProcessor.deserialize(tag);
    }

    @Nullable
    public RobotActionResult findActionResult(final int actionId) {
        final AbstractRobotAction currentAction = actionProcessor.action;
        if (currentAction != null && currentAction.getId() == actionId) {
            return RobotActionResult.INCOMPLETE;
        }
        synchronized (actionProcessor.queue) {
            for (final AbstractRobotAction action : actionProcessor.queue) {
                if (action.getId() == actionId) {
                    return RobotActionResult.INCOMPLETE;
                }
            }
        }
        synchronized (actionProcessor.results) {
            for (final RobotActionProcessorResult result : actionProcessor.results) {
                if (result.actionId == actionId) {
                    return result.result;
                }
            }
        }
        return null;
    }
}