package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class ECH extends CSISequenceHandler {
    public ECH(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        int chars = args[0];
        TerminalColors.ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
        }
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            int fromIndex = terminal.x + terminal.y * Terminal.WIDTH;
            int toIndex =
                    fromIndex
                            + Math.max(
                                    Math.min(Math.max(chars, 1), Terminal.WIDTH - terminal.x), 1);
            Arrays.fill(terminal.altBuffer, fromIndex, toIndex, ' ');
            Arrays.fill(
                    terminal.altColors, fromIndex, toIndex, TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.altColorsBackground, fromIndex, toIndex, c.Copy());
            Arrays.fill(terminal.altStyles, fromIndex, toIndex, TerminalColors.DEFAULT_STYLE);
        } else {
            int fromIndex =
                    terminal.x
                            + (terminal.y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT))
                                    * Terminal.WIDTH;
            int toIndex =
                    fromIndex
                            + Math.max(
                                    Math.min(Math.max(chars, 1), Terminal.WIDTH - terminal.x), 1);
            Arrays.fill(terminal.buffer, fromIndex, toIndex, ' ');
            Arrays.fill(terminal.colors, fromIndex, toIndex, TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.colorsBackground, fromIndex, toIndex, c.Copy());
            Arrays.fill(terminal.styles, fromIndex, toIndex, TerminalColors.DEFAULT_STYLE);
        }

        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(1 << terminal.y, (left, right) -> left | right));
    }
}