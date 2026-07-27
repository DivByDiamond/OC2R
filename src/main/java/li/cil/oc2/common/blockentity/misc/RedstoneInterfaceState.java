package li.cil.oc2.common.blockentity.misc;

import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class RedstoneInterfaceState {
    private static final String OUTPUT_TAG_NAME = "output";
    private static final String BUNDLED_TAG_NAME = "bundled";

    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];
    private final byte[][] bundledOutput = new byte[Constants.BLOCK_FACE_COUNT][16];

    public void saveAdditional(final CompoundTag tag) {
        tag.putByteArray(OUTPUT_TAG_NAME, output);
        final CompoundTag bundledTag = new CompoundTag();
        for (final Direction dir : Direction.values()) {
            bundledTag.putByteArray(dir.getName(), bundledOutput[dir.get3DDataValue()]);
        }
        tag.put(BUNDLED_TAG_NAME, bundledTag);
    }

    public void loadAdditional(final CompoundTag tag) {
        final byte[] serializedOutput = tag.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(
                serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));

        final CompoundTag bundledTag = tag.getCompound(BUNDLED_TAG_NAME);
        for (final Direction dir : Direction.values()) {
            final byte[] serializedBundled = bundledTag.getByteArray(dir.getName());
            final byte[] dest = bundledOutput[dir.get3DDataValue()];
            System.arraycopy(
                    serializedBundled, 0, dest, 0, Math.min(serializedBundled.length, dest.length));
        }
    }

    public int getOutputForDirection(final BlockState blockState, final Direction direction) {
        final Direction localDirection = HorizontalBlockUtils.toLocal(blockState, direction);
        assert localDirection != null;
        return output[localDirection.get3DDataValue()];
    }

    public byte getOutput(final int index) {
        return output[index];
    }

    public void setOutput(final int index, final byte value) {
        output[index] = value;
    }

    public byte[] getBundledOutput(final int index) {
        return bundledOutput[index];
    }

    @Nullable
    public byte[] getBundledSignal(final Direction direction) {
        return bundledOutput[direction.get3DDataValue()];
    }
}