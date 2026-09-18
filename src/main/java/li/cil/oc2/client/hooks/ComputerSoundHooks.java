package li.cil.oc2.client.hooks;

import li.cil.oc2.client.audio.LoopingSoundManager;
import li.cil.oc2.common.util.sound.SoundEvents;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ComputerSoundHooks {
    private ComputerSoundHooks() {}

    public static void handleRunStateChange(final BlockEntity blockEntity, final li.cil.oc2.common.vm.VMRunState state, final Level level, final int maxRunningSoundDelay) {
        if (state == VMRunState.RUNNING) {
            if (!LoopingSoundManager.isPlaying(blockEntity) && level != null) {
                LoopingSoundManager.play(blockEntity, SoundEvents.COMPUTER_RUNNING.get(), level.getRandom().nextInt(maxRunningSoundDelay));
            }
        } else {
            LoopingSoundManager.stop(blockEntity);
        }
    }
}