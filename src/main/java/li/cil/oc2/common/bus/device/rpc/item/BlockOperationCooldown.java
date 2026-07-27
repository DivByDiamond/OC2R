package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.common.util.TickUtils;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.time.Duration;

class BlockOperationCooldown {
    private static final String LAST_OPERATION_TAG_NAME = "cooldown";
    private static final int COOLDOWN = TickUtils.toTicks(Duration.ofSeconds(1));

    private long lastOperation;

    void beginCooldown(final Level level) {
        lastOperation = level.getGameTime();
    }

    boolean isOnCooldown(final Level level) {
        return level.getGameTime() - lastOperation < COOLDOWN;
    }

    CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(LAST_OPERATION_TAG_NAME, lastOperation);
        return tag;
    }

    void deserializeNBT(
            final HolderLookup.Provider provider, final CompoundTag tag, final Level level) {
        lastOperation = Mth.clamp(tag.getLong(LAST_OPERATION_TAG_NAME), 0, level.getGameTime());
    }
}
