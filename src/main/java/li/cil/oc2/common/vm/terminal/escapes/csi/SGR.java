package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.csi.SGRColorParser.SGRColorResult;

public class SGR extends CSISequenceHandler {
    public SGR(final Terminal terminal) {
        super(terminal);
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
                    /* Malformed — skip the mode selector and move on */
                    i += 1;
                }
                continue;
            }

            SGRStyleDispatch.apply(terminal, code);
            i++;
        }
    }

    private static void applyExtendedColor(final Terminal terminal, final int selector,
            final SGRColorResult result) {
        if (selector == 38) {
            /* Foreground */
            terminal.currentForegroundColorMode = result.mode();
            if (result.mode() == TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR) {
                terminal.twoFiftySixColor.R = result.color().R;
            } else {
                terminal.foregroundColor = result.color();
            }
        } else {
            /* Background (48) */
            terminal.currentBackgroundColorMode = result.mode();
            if (result.mode() == TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR) {
                terminal.twoFiftySixColor.G = result.color().R;
            } else {
                terminal.backgroundColor = result.color();
            }
        }
    }
}
