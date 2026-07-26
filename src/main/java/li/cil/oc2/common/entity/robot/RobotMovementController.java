/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;

public class RobotMovementController {
    private static final int MAX_QUEUED_ACTIONS = 16;
    private static final int MAX_QUEUED_RESULTS = 16;

    private final Robot robot;
    private final RobotActionProcessor actionProcessor = new RobotActionProcessor();

    public RobotMovementController(final Robot robot) {
        this.robot = robot;
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

    ///////////////////////////////////////////////////////////////////

    private static final class RobotActionProcessorResult {
        private static final String ACTION_ID_TAG_NAME = "action_id";
        private static final String RESULT_TAG_NAME = "result";

        public int actionId;
        public RobotActionResult result;

        public RobotActionProcessorResult(final int actionId, final RobotActionResult result) {
            this.actionId = actionId;
            this.result = result;
        }

        public RobotActionProcessorResult(final CompoundTag tag) {
            deserialize(tag);
        }

        public CompoundTag serialize() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt(ACTION_ID_TAG_NAME, actionId);
            NBTUtils.putEnum(tag, RESULT_TAG_NAME, result);
            return tag;
        }

        public void deserialize(final CompoundTag tag) {
            actionId = tag.getInt(ACTION_ID_TAG_NAME);
            result = NBTUtils.getEnum(tag, RESULT_TAG_NAME, RobotActionResult.class);
        }
    }

    private final class RobotActionProcessor {
        private static final String QUEUE_TAG_NAME = "queue";
        private static final String ACTION_TAG_NAME = "action";
        private static final String RESULTS_TAG_NAME = "results";
        private static final String LAST_ACTION_ID_TAG_NAME = "last_action_id";

        private final Queue<AbstractRobotAction> queue = new ArrayDeque<>(MAX_QUEUED_ACTIONS - 1);
        @Nullable private AbstractRobotAction action;

        private final Queue<RobotActionProcessorResult> results = new ArrayDeque<>(MAX_QUEUED_RESULTS);
        private int lastActionId;

        public boolean hasQueuedActions() {
            return action != null || !queue.isEmpty();
        }

        public int getQueuedActionCount() {
            return (action != null ? 1 : 0) + queue.size();
        }

        public void tick() {
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
                    }
                    else {
                        return;
                    }
                }
                RobotActions.performServer(robot, action);
            }
        }

        public void clear() {
            queue.clear();
            results.clear();
            lastActionId = 0;
        }

        public CompoundTag serialize() {
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

        public void deserialize(final CompoundTag tag) {
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
                final RobotActionProcessorResult result = new RobotActionProcessorResult(resultsTag.getCompound(i));
                if (result.actionId != 0) {
                    results.add(result);
                }
            }

            lastActionId = tag.getInt(LAST_ACTION_ID_TAG_NAME);
        }

        private boolean addAction(final AbstractRobotAction action) {
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
}
