package li.cil.oc2.common.bus.device.rpc.item.module;

import java.time.Duration;
import net.minecraft.nbt.CompoundTag;

/**
 * Cooldown of the scanner module.
 *
 * <p>The deadline is an absolute timestamp in milliseconds since the epoch instead of the
 * level's game time: the module is an item and may be carried into another dimension or
 * world, whose game time counters are unrelated, which would leave it recharging forever.
 */
final class ScannerCooldown {
    private static final String READY_AT_TAG_NAME = "ready_at";
    /**
     * Deadlines written before this cooldown used the level's game time, which never reaches
     * this magnitude. Such values are treated as "ready now" so that items saved by an older
     * version stay usable instead of being stuck in the recharging state.
     */
    private static final long LEGACY_DEADLINE_LIMIT = 1_000_000_000_000L;

    private final long durationMillis;
    private long readyAt;

    ScannerCooldown(final Duration duration) {
        durationMillis = duration.toMillis();
    }

    boolean isRecharging() {
        return System.currentTimeMillis() < readyAt;
    }

    void begin() {
        readyAt = System.currentTimeMillis() + durationMillis;
    }

    void serializeNBT(final CompoundTag nbt) {
        nbt.putLong(READY_AT_TAG_NAME, readyAt);
    }

    void deserializeNBT(final CompoundTag nbt) {
        //? if >=26.1 {
        /*final long stored = nbt.getLongOr(READY_AT_TAG_NAME, 0L);
        *///?} else {
        final long stored = nbt.getLong(READY_AT_TAG_NAME);
        //?}
        readyAt = stored < LEGACY_DEADLINE_LIMIT ? 0L : stored;
    }
}
