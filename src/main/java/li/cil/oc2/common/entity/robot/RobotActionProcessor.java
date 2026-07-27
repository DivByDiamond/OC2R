package li.cil.oc2.common.entity.robot;

import java.util.ArrayDeque;
import java.util.Queue;
import javax.annotation.Nullable;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

final class RobotActionProcessor {
    private static final int MAX_QUEUED_ACTIONS = 16;
    private static final int MAX_QUEUED_RESULTS = 16;

    private static final String QUEUE_TAG_NAME = "queue";
    private static final String ACTION_TAG_NAME = "action";
    private static final String RESULTS_TAG_NAME = "results";
    private static final String LAST_ACTION_ID_TAG_NAME = "last_action_id";

    private final Robot robot;

    final Queue<AbstractRobotAction> queue = new ArrayDeque<>(MAX_QUEUED_ACTIONS - 1);
    @Nullable AbstractRobotAction action;

    final Queue<RobotActionProcessorResult> results = new ArrayDeque<>(MAX_QUEUED_RESULTS);
    int lastActionId;

    RobotActionProcessor(final Robot robot) {
        this.robot = robot;
    }

    boolean hasQueuedActions() {
        return action != null || !queue.isEmpty();
    }

    int getQueuedActionCount() {
        return (action != null ? 1 : 0) + queue.size();
    }

    void tick() {
        if (robot.level().isClientSide()) {
            RobotActions.performClient(robot);
        } else {
            if (action != null) {
                final RobotActionResult result = action.perform(robot);
                if (result != RobotActionResult.INCOMPLETE) {
                    synchronized (results) {
                        if (results.size() == MAX_QUEUED_RESULTS) {
                            results.remove();
                        }

                        results.add(new RobotActionProcessorResult(action.getId(), result));
                    }

                    action = null;
                }
            }
            if (action == null) {
                action = queue.poll();
                if (action != null) {
                    action.initialize(robot);
                } else {
                    return;
                }
            }
            RobotActions.performServer(robot, action);
        }
    }

    void clear() {
        queue.clear();
        results.clear();
        lastActionId = 0;
    }

    CompoundTag serialize() {
        final CompoundTag tag = new CompoundTag();

        final ListTag queueTag = new ListTag();
        for (final AbstractRobotAction action : queue) {
            queueTag.add(RobotActions.serialize(action));
        }
        tag.put(QUEUE_TAG_NAME, queueTag);

        if (action != null) {
            tag.put(ACTION_TAG_NAME, RobotActions.serialize(action));
        }

        final ListTag resultsTag = new ListTag();
        for (final RobotActionProcessorResult result : results) {
            resultsTag.add(result.serialize());
        }
        tag.put(RESULTS_TAG_NAME, resultsTag);

        tag.putInt(LAST_ACTION_ID_TAG_NAME, lastActionId);

        return tag;
    }

    void deserialize(final CompoundTag tag) {
        queue.clear();
        results.clear();

        final ListTag queueTag = tag.getList(QUEUE_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(queueTag.size(), MAX_QUEUED_ACTIONS - 1); i++) {
            final AbstractRobotAction action = RobotActions.deserialize(queueTag.getCompound(i));
            if (action != null) {
                queue.add(action);
            }
        }

        action = RobotActions.deserialize(tag.getCompound(ACTION_TAG_NAME));

        final ListTag resultsTag = tag.getList(RESULTS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(resultsTag.size(), MAX_QUEUED_RESULTS); i++) {
            final RobotActionProcessorResult result =
                    new RobotActionProcessorResult(resultsTag.getCompound(i));
            if (result.actionId != 0) {
                results.add(result);
            }
        }

        lastActionId = tag.getInt(LAST_ACTION_ID_TAG_NAME);
    }

    boolean addAction(final AbstractRobotAction action) {
        if (robot.level().isClientSide()) {
            return false;
        }

        if (robot.getVirtualMachine() != null && !robot.getVirtualMachine().isRunning()) {
            return false;
        }

        if (queue.size() < MAX_QUEUED_ACTIONS - 1) {
            lastActionId = (lastActionId + 1) & 0x7FFFFFFF;
            action.setId(lastActionId);
            synchronized (queue) {
                queue.add(action);
            }
            return true;
        } else {
            return false;
        }
    }
}