package li.cil.oc2.common.entity.robot.action.processor;

import li.cil.oc2.common.entity.robot.action.RobotActionResult;
import li.cil.oc2.common.util.nbt.NBTUtils;
import net.minecraft.nbt.CompoundTag;

public final class RobotActionProcessorResult {
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