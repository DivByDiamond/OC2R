package li.cil.oc2.common.entity.robot.action.processor;

import java.util.List;
import li.cil.oc2.common.entity.robot.action.AbstractRobotAction;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class RobotActionProcessorSerialization {
    private static final String QUEUE_TAG_NAME = "queue";
    private static final String ACTION_TAG_NAME = "action";
    private static final String RESULTS_TAG_NAME = "results";
    private static final String LAST_ACTION_ID_TAG_NAME = "last_action_id";

    static CompoundTag serialize(final RobotActionProcessor processor) {
        final CompoundTag tag = new CompoundTag();

        final List<Tag> queueTag = new ListTag();
        for (final AbstractRobotAction action : processor.queue) {
            queueTag.add(RobotActions.serialize(action));
        }
        tag.put(QUEUE_TAG_NAME, (ListTag) queueTag);

        if (processor.action != null) {
            tag.put(ACTION_TAG_NAME, RobotActions.serialize(processor.action));
        }

        final List<Tag> resultsTag = new ListTag();
        for (final RobotActionProcessorResult result : processor.results) {
            resultsTag.add(result.serialize());
        }
        tag.put(RESULTS_TAG_NAME, (ListTag) resultsTag);

        tag.putInt(LAST_ACTION_ID_TAG_NAME, processor.lastActionId);

        return tag;
    }

    static void deserialize(
            final RobotActionProcessor processor, final CompoundTag tag, final int maxActions) {
        processor.queue.clear();
        processor.results.clear();

        final List<Tag> queueTag = tag.getList(QUEUE_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(queueTag.size(), maxActions - 1); i++) {
            final AbstractRobotAction action =
                    RobotActions.deserialize((CompoundTag) queueTag.get(i));
            if (action != null) {
                processor.queue.add(action);
            }
        }

        processor.action = RobotActions.deserialize(tag.getCompound(ACTION_TAG_NAME));

        final List<Tag> resultsTag = tag.getList(RESULTS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(resultsTag.size(), RobotActionProcessor.MAX_QUEUED_RESULTS); i++) {
            final RobotActionProcessorResult result =
                    new RobotActionProcessorResult((CompoundTag) resultsTag.get(i));
            if (result.actionId != 0) {
                processor.results.add(result);
            }
        }

        processor.lastActionId = tag.getInt(LAST_ACTION_ID_TAG_NAME);
    }
}
