package li.cil.oc2.common.util;

import javax.annotation.Nullable;
import li.cil.oc2.common.config.Config;

/** The client-wide "capture input" toggle used by terminals configured for global capture. */
public final class GlobalInputCapture {
    @Nullable private static Boolean captureInputState;

    private GlobalInputCapture() {}

    /** Whether input is currently captured, starting from the configured default. */
    public static boolean isCaptured() {
        if (captureInputState == null) {
            captureInputState = Config.captureInputDefaultState;
        }
        return captureInputState;
    }

    public static void set(final boolean value) {
        captureInputState = value;
    }
}
