package li.cil.oc2.common.entity.robot;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class RobotActionProcessor {

    private final ReentrantLock lock = new ReentrantLock();

    private static final int MAX_QUEUED_ACTIONS = 16;
    private static final int MAX_QUEUED_RESULTS = 16;

    private static final String QUEUE_TAG_NAME = "queue";
    private static final String ACTION_TAG_NAME = "action";
    private static final String RESULTS_TAG_NAME = "results";
    private static final String LAST_ACTION_ID_TAG_NAME = "last_action_id";

    private final Robot robot;

    private final Queue<AbstractRobotAction> queue = new ArrayDeque<>(MAX_QUEUED_ACTIONS - 1);
    @Nullable private AbstractRobotAction action;

    private final Queue<RobotActionProcessorResult> results = new ArrayDeque<>(MAX_QUEUED_RESULTS);
    private int lastActionId;

    RobotActionProcessor(final Robot robot) {
        this.robot = robot;
    }

    int getLastActionId() {
        lock.lock();
        try {

            return lastActionId;
        
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    AbstractRobotAction getCurrentAction() {
        lock.lock();
        try {

            return action;
        
        } finally {
            lock.unlock();
        }
    }

    Queue<AbstractRobotAction> getQueue() {
        lock.lock();
        try {

            return new ArrayDeque<>(queue);
        
        } finally {
            lock.unlock();
        }
    }

    Queue<RobotActionProcessorResult> getResults() {
        lock.lock();
        try {

            return new ArrayDeque<>(results);
        
        } finally {
            lock.unlock();
        }
    }

    boolean hasQueuedActions() {
        lock.lock();
        try {

            return action != null || !queue.isEmpty();
        
        } finally {
            lock.unlock();
        }
    }

    int getQueuedActionCount() {
        lock.lock();
        try {

            return (action != null ? 1 : 0) + queue.size();
        
        } finally {
            lock.unlock();
        }
    }

    void tick() {
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

    void clear() {
        lock.lock();
        try {

            queue.clear();
            results.clear();
            lastActionId = 0;
        
        } finally {
            lock.unlock();
        }
    }

    CompoundTag serialize() {
        lock.lock();
        try {

            final CompoundTag tag = new CompoundTag();

            final List<Tag> queueTag = new ListTag();
            for (final AbstractRobotAction action : queue) {
                queueTag.add(RobotActions.serialize(action));
            }
            tag.put(QUEUE_TAG_NAME, (ListTag) queueTag);

            if (action != null) {
                tag.put(ACTION_TAG_NAME, RobotActions.serialize(action));
            }

            final List<Tag> resultsTag = new ListTag();
            for (final RobotActionProcessorResult result : results) {
                resultsTag.add(result.serialize());
            }
            tag.put(RESULTS_TAG_NAME, (ListTag) resultsTag);

            tag.putInt(LAST_ACTION_ID_TAG_NAME, lastActionId);

            return tag;
        
        } finally {
            lock.unlock();
        }
    }

    void deserialize(final CompoundTag tag) {
        lock.lock();
        try {

            queue.clear();
            results.clear();

            final List<Tag> queueTag = tag.getList(QUEUE_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < Math.min(queueTag.size(), MAX_QUEUED_ACTIONS - 1); i++) {
                final AbstractRobotAction action = RobotActions.deserialize((CompoundTag) queueTag.get(i));
                if (action != null) {
                    queue.add(action);
                }
            }

            action = RobotActions.deserialize(tag.getCompound(ACTION_TAG_NAME));

            final List<Tag> resultsTag = tag.getList(RESULTS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < Math.min(resultsTag.size(), MAX_QUEUED_RESULTS); i++) {
                final RobotActionProcessorResult result =
                        new RobotActionProcessorResult((CompoundTag) resultsTag.get(i));
                if (result.actionId != 0) {
                    results.add(result);
                }
            }

            lastActionId = tag.getInt(LAST_ACTION_ID_TAG_NAME);
        
        } finally {
            lock.unlock();
        }
    }

    boolean addAction(final AbstractRobotAction action) {
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
    RobotActionResult findActionResult(final int actionId) {
        lock.lock();
        try {

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
        
        } finally {
            lock.unlock();
        }
    }
}