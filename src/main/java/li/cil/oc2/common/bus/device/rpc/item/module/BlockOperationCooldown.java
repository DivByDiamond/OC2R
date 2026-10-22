package li.cil.oc2.common.bus.device.rpc.item.module;

import java.time.Duration;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Cooldown of the block operations module.
 *
 * <p>The deadline is an absolute timestamp in milliseconds since the epoch instead of the
 * level's game time: the module is an item and may be carried into another dimension or
 * world, whose game time counters are unrelated, which would leave it on cooldown forever.
 */
class BlockOperationCooldown {
    private static final String READY_AT_TAG_NAME = "cooldown";
    /**
     * Deadlines written before this cooldown used the level's game time, which never reaches
     * this magnitude. Such values are treated as "ready now" so that items saved by an older
     * version stay usable instead of being stuck on cooldown.
     */
    private static final long LEGACY_DEADLINE_LIMIT = 1_000_000_000_000L;

    private final long durationMillis;
    private long readyAt;

    BlockOperationCooldown(final Duration duration) {
        durationMillis = duration.toMillis();
    }

    void beginCooldown() {
        readyAt = System.currentTimeMillis() + durationMillis;
    }

    boolean isOnCooldown() {
        return System.currentTimeMillis() < readyAt;
    }

    CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(READY_AT_TAG_NAME, readyAt);
        return tag;
    }

    void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag tag) {
        //? if >=26.1 {
        /*final long stored = tag.getLongOr(READY_AT_TAG_NAME, 0L);
        *///?} else {
        final long stored = tag.getLong(READY_AT_TAG_NAME);
        //?}
        readyAt = stored < LEGACY_DEADLINE_LIMIT ? 0L : stored;
    }
}
