package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.csi.SGRColorParser.SGRColorResult;

public class SGR extends CSISequenceHandler {
    public SGR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {0};
    }

    @Override
    public void execute(final int[] args, final int argCount, final CSIState state) {
        int i = 0;
        final int count = Math.max(1, argCount);
        while (i < count) {
            final int code = args[i];

            if (code == 38 || code == 48) {
                /* Extended color: 38;5;N (256-color) or 38;2;R;G;B (true color).
                   Same sub-arg format for 48 (background). */
                final SGRColorResult result = SGRColorParser.parse(args, i + 1, argCount);
                if (result.isValid()) {
                    applyExtendedColor(terminal, code, result);
                    i += 1 + result.consumed();
                } else {
                    i = skipMalformedExtendedColor(args, i, count);
                }
                continue;
            }

            SGRStyleDispatch.apply(terminal, code);
            i++;
        }
    }

    /**
     * Recovery for a malformed 38/48 extended-color group at {@code args[selectorIndex]} — two
     * distinct cases:
     * <ul>
     *   <li>the mode byte isn't a recognized selector (not 5 or 2): the malformed group is
     *       exactly {@code [selector, mode byte]}; skip just those two and keep reading the rest
     *       of the argument list as independent top-level SGR codes (e.g. {@code 38;7;1} — 7
     *       isn't a color mode, so 1 still applies as bold).
     *   <li>the mode byte IS 5 or 2, but this CSI ran out of arguments before supplying the full
     *       spec (5 needs 1 more, 2 needs 3 more): every remaining argument is part of that
     *       truncated attempt — there's nothing left after it to be an independent code — so
     *       skip to the end of the list instead of guessing a fixed count. Otherwise a leftover
     *       byte from the incomplete color spec (e.g. the {@code 1} in {@code 38;2;1}) gets
     *       misread as an unrelated style change.
     * </ul>
     */
    private static int skipMalformedExtendedColor(final int[] args, final int selectorIndex, final int count) {
        final boolean hasModeByte = selectorIndex + 1 < count;
        final boolean recognizedMode =
                hasModeByte && (args[selectorIndex + 1] == 5 || args[selectorIndex + 1] == 2);
        if (recognizedMode) {
            return count;
        }
        return selectorIndex + (hasModeByte ? 2 : 1);
    }

    private static void applyExtendedColor(final Terminal terminal, final int selector,
            final SGRColorResult result) {
        if (selector == 38) {
            /* Foreground */
            terminal.currentForegroundColorMode = result.mode();
            if (result.mode() == TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR) {
                terminal.twoFiftySixColor.r = result.color().r;
            } else {
                terminal.foregroundColor = result.color();
            }
        } else {
            /* Background (48) */
            terminal.currentBackgroundColorMode = result.mode();
            if (result.mode() == TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR) {
                terminal.twoFiftySixColor.g = result.color().r;
            } else {
                terminal.backgroundColor = result.color();
            }
        }
    }
}
