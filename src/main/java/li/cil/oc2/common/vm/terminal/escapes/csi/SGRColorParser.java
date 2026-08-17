package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;

/**
 * Parses SGR extended color sequences (38/48) from a CSI argument stream.
 * Returns the parsed color result and how many sub-args were consumed.
 *
 * Format:
 *   38;5;N  → 256-color foreground (consumes 2 sub-args)
 *   48;5;N  → 256-color background (consumes 2 sub-args)
 *   38;2;R;G;B → true-color foreground (consumes 4 sub-args)
 *   48;2;R;G;B → true-color background (consumes 4 sub-args)
 */
public final class SGRColorParser {

    private SGRColorParser() {}

    /**
     * Result of parsing an extended color sequence.
     *
     * @param mode     Color mode to set on the terminal.
     * @param color    Parsed color data, or null if malformed.
     * @param consumed Number of sub-args consumed (0 if malformed, 2 for 256-color, 4 for true-color).
     */
    public record SGRColorResult(ColorMode mode, ColorData color, int consumed) {
        public boolean isValid() {
            return color != null && consumed > 0;
        }
    }

    /**
     * Parse an extended color sequence starting at args[offset].
     *
     * @param args   The full CSI argument array.
     * @param offset Index of the first sub-arg (the mode selector: 5 or 2).
     * @param limit  Total number of valid args (argCount).
     * @return Parse result with consumed count.
     */
    public static SGRColorResult parse(final int[] args, final int offset, final int limit) {
        if (offset >= limit) {
            return new SGRColorResult(null, null, 0);
        }

        int mode = args[offset];

        if (mode == 5 && offset + 1 < limit) {
            /* 256-color: 5;N → N is the color index */
            int index = args[offset + 1];
            return new SGRColorResult(
                    ColorMode.TWO_FIFTY_SIX_COLOR,
                    new ColorData(index, 0, 0, ColorMode.TWO_FIFTY_SIX_COLOR),
                    2);
        }

        if (mode == 2 && offset + 3 < limit) {
            /* True color: 2;R;G;B */
            int r = args[offset + 1];
            int g = args[offset + 2];
            int b = args[offset + 3];
            return new SGRColorResult(
                    ColorMode.TRUE_COLOR,
                    new ColorData(r, g, b, ColorMode.TRUE_COLOR),
                    4);
        }

        /* Malformed — no recognizable sub-args or not enough args */
        return new SGRColorResult(null, null, 0);
    }
}
