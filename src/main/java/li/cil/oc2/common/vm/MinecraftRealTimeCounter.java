/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.sedna.api.device.rtc.RealTimeCounter;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class MinecraftRealTimeCounter implements RealTimeCounter {
    private static final int TICKS_PER_DAY = 24000;
    private static final int FREQUENCY = TICKS_PER_DAY;

    ///////////////////////////////////////////////////////////////////

    private Level level;

    ///////////////////////////////////////////////////////////////////

    public void setLevel(@Nullable final Level level) {
        this.level = level;
    }

    @Override
    public long getTime() {
        final long days = level != null ? level.getGameTime() : 0; // / TICKS_PER_DAY
        final long hours = days * 24;
        final long minutes = hours * 60;
        return minutes * 60; // * FREQUENCY
    }

    @Override
    public int getFrequency() {
        return FREQUENCY;
    }
}
