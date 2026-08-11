package li.cil.oc2.common.entity.robot.action.processor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.AbstractRobotAction;
import li.cil.oc2.common.entity.robot.action.RobotActionResult;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import net.minecraft.nbt.CompoundTag;

public final class RobotActionProcessor {

    static final int MAX_QUEUED_ACTIONS = 16;
    static final int MAX_QUEUED_RESULTS = 16;

    private final ReentrantLock lock = new ReentrantLock();

    private final Robot robot;

    final Queue<AbstractRobotAction> queue = new ArrayDeque<>(MAX_QUEUED_ACTIONS - 1);
    @Nullable AbstractRobotAction action;

    final Queue<RobotActionProcessorResult> results = new ArrayDeque<>(MAX_QUEUED_RESULTS);
    int lastActionId;

    public RobotActionProcessor(final Robot robot) {
        this.robot = robot;
    }

    public int getLastActionId() {
        return withLock(() -> lastActionId);
    }

    @Nullable
    public AbstractRobotAction getCurrentAction() {
        return withLock(() -> action);
    }

    public Queue<AbstractRobotAction> getQueue() {
        return withLock(() -> new ArrayDeque<>(queue));
    }

    public Queue<RobotActionProcessorResult> getResults() {
        return withLock(() -> new ArrayDeque<>(results));
    }

    public boolean hasQueuedActions() {
        return withLock(() -> action != null || !queue.isEmpty());
    }

    public int getQueuedActionCount() {
        return withLock(() -> (action != null ? 1 : 0) + queue.size());
    }

    public void tick() {
        if (robot.level().isClientSide()) {
            RobotActions.performClient(robot);
            return;
        }

        AbstractRobotAction currentAction;
        lock.lock();
        try {
            currentAction = action;
            action = null;
        } finally {
            lock.unlock();
        }

        if (currentAction != null) {
            final RobotActionResult result = currentAction.perform(robot);
            if (result != RobotActionResult.INCOMPLETE) {
                lock.lock();
                try {
                    if (results.size() == MAX_QUEUED_RESULTS) {
                        results.remove();
                    }
                    results.add(new RobotActionProcessorResult(currentAction.getId(), result));
                } finally {
                    lock.unlock();
                }
            } else {
                lock.lock();
                try {
                    action = currentAction;
                } finally {
                    lock.unlock();
                }
            }
        }

        AbstractRobotAction nextAction;
        lock.lock();
        try {
            if (action == null) {
                nextAction = queue.poll();
                if (nextAction != null) {
                    nextAction.initialize(robot);
                    action = nextAction;
                }
            } else {
                nextAction = action;
            }
        } finally {
            lock.unlock();
        }

        if (nextAction != null) {
            RobotActions.performServer(robot, nextAction);
        }
    }

    public void clear() {
        lock.lock();
        try {
            queue.clear();
            results.clear();
            lastActionId = 0;
        } finally {
            lock.unlock();
        }
    }

    public CompoundTag serialize() {
        return withLock(() -> RobotActionProcessorSerialization.serialize(this));
    }

    public void deserialize(final CompoundTag tag) {
        lock.lock();
        try {
            RobotActionProcessorSerialization.deserialize(this, tag, MAX_QUEUED_ACTIONS);
        } finally {
            lock.unlock();
        }
    }

    public boolean addAction(final AbstractRobotAction action) {
        if (robot.level().isClientSide()) {
            return false;
        }

        if (robot.getVirtualMachine() != null && !robot.getVirtualMachine().isRunning()) {
            return false;
        }

        lock.lock();
        try {
            if (queue.size() < MAX_QUEUED_ACTIONS - 1) {
                lastActionId = (lastActionId + 1) & 0x7FFFFFFF;
                action.setId(lastActionId);
                queue.add(action);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public RobotActionResult findActionResult(final int actionId) {
        return withLock(
                () -> {
                    final AbstractRobotAction currentAction = action;
                    if (currentAction != null && currentAction.getId() == actionId) {
                        return RobotActionResult.INCOMPLETE;
                    }

                    for (final AbstractRobotAction queuedAction : queue) {
                        if (queuedAction.getId() == actionId) {
                            return RobotActionResult.INCOMPLETE;
                        }
                    }

                    for (final RobotActionProcessorResult result : results) {
                        if (result.actionId == actionId) {
                            return result.result;
                        }
                    }

                    return null;
                });
    }

    private <T> T withLock(final Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
