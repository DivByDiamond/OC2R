package li.cil.oc2.common.util.sound;

import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.world.level.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;

public final class ComputerPost {

    public static void playBootError(
            final LevelAccessor level, final BlockPos pos, final Component error) {
        final SoundEvent sound = resolveSound(error);
        if (sound != null) {
            LevelUtils.playSound(level, pos, sound, SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }

    @Nullable
    private static SoundEvent resolveSound(final Component error) {
        final ComponentContents contents = error.getContents();
        final String key =
                contents instanceof final TranslatableContents translatable
                        ? translatable.getKey()
                        : Constants.COMPUTER_ERROR_UNKNOWN;
        if (Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY.equals(key)) {
            return SoundEvents.POST_BEEP_ENERGY.get();
        } else if (Constants.COMPUTER_ERROR_MISSING_FIRMWARE.equals(key)) {
            return SoundEvents.POST_BEEP_FIRMWARE.get();
        } else if (Constants.COMPUTER_ERROR_MISSING_CPU.equals(key)) {
            return SoundEvents.POST_BEEP_CPU.get();
        } else if (Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY.equals(key)) {
            return SoundEvents.POST_BEEP_MEMORY.get();
        }
        return SoundEvents.POST_BEEP_UNKNOWN.get();
    }
}