package li.cil.oc2.common.blockentity.misc.redstone;

import javax.annotation.Nullable;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.misc.redstone.state.RedstoneInterfaceState;
import li.cil.oc2.common.integration.util.BundledRedstone;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

final class BundledRedstoneCallbacks {
    @Nullable
    static byte[] getBundledInput(
            @Nullable final Level level, final BlockPos pos, final Side side) {
        final BundledRedstone bundledRedstone = BundledRedstone.getInstance();
        if (bundledRedstone.isAvailable()) {
            return bundledRedstone.getBundledInput(level, pos, side.getDirection().getOpposite());
        }
        return new byte[Constants.BLOCK_FACE_COUNT];
    }

    static byte[] getBundledOutput(final Side side, final RedstoneInterfaceState state) {
        return state.getBundledOutput(side.getDirection().get3DDataValue());
    }

    static boolean setBundledOutput(
            final Side side, final int value, final int color, final RedstoneInterfaceState state) {
        final int index = side.getDirection().getOpposite().get3DDataValue();
        final byte clampedValue = (byte) Mth.clamp(value, 0, 255);
        final byte clampedColor = (byte) Mth.clamp(color, 0, 15);

        if (state.getBundledOutput(index)[clampedColor] != clampedValue) {
            state.getBundledOutput(index)[clampedColor] = clampedValue;
            return true;
        }
        return false;
    }

    static boolean setBundledOutputs(
            final Side side, final int[] values, final RedstoneInterfaceState state) {
        boolean changed = false;
        final int index = side.getDirection().getOpposite().get3DDataValue();
        final byte[] output = state.getBundledOutput(index);
        for (int i = 0; i < values.length; i++) {
            final byte clampedValue = (byte) Mth.clamp(values[i], 0, 255);
            if (clampedValue != output[i]) {
                output[i] = clampedValue;
                changed = true;
            }
        }
        return changed;
    }
}
