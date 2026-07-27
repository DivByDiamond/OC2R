package li.cil.oc2.common.util.tick;

import java.time.Duration;
import li.cil.oc2.common.Constants;

public final class TickUtils {
    public static int toTicks(final Duration duration) {
        return (int) (duration.getSeconds() * Constants.SECONDS_TO_TICKS);
    }
}