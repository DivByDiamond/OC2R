package li.cil.oc2.common.config.common;

import li.cil.oc2.common.config.Config;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.ModConfigSpec;

public class GameplaySpec {
    public final ModConfigSpec.EnumValue<Tiers> blockOperationsModuleToolTier;
    public final ModConfigSpec.LongValue soundCardCoolDownSeconds;

    GameplaySpec(ModConfigSpec.Builder builder) {
        blockOperationsModuleToolTier = builder.comment(
            "The mining tool equivalent of the block operations module"
        ).defineEnum("blockOperationsModuleToolTier", Tiers.DIAMOND);

        soundCardCoolDownSeconds = builder.comment(
            "The number of seconds between sound card uses, to prevent spam/abuse"
        ).defineInRange("soundCardCoolDownSeconds", 2, 1, Long.MAX_VALUE);
    }

    public void loadValues() {
        Config.blockOperationsModuleToolTier = blockOperationsModuleToolTier.get().name();
        Config.soundCardCoolDownSeconds = soundCardCoolDownSeconds.get();
    }
}
