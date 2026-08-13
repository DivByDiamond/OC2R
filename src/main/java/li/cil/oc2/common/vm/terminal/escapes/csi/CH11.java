package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;

public class CH11 extends CSISequenceHandler { // Combined Handler 10 (ICH and SL)
    public CH11(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        int chars = Math.max(args[0], 1);
        if (state.space) { // SL
            chars = Math.min(chars, Terminal.WIDTH);
            for (int i = terminal.scrollFirst; i <= terminal.scrollLast; i++) {
                shiftLeft(chars, i);
            }
        } else { // ICH
            chars = Math.min(chars, Terminal.WIDTH - terminal.x);
            shiftRight(chars);
        }
    }

    private void shiftLeft(int chars, int y) {
        int startIndex =
                ((terminal.currentPrivateModeState.isAltBufferEnabled())
                        ? y * Terminal.WIDTH
                        : (y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT)) * Terminal.WIDTH);
        int count = (Terminal.WIDTH) - chars;
        int endIndex = startIndex + count;
        TerminalColors.ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
        }
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            System.arraycopy(
                    terminal.altBuffer, startIndex + chars, terminal.altBuffer, startIndex, count);
            System.arraycopy(
                    terminal.altColors, startIndex + chars, terminal.altColors, startIndex, count);
            System.arraycopy(
                    terminal.altColorsBackground,
                    startIndex + chars,
                    terminal.altColorsBackground,
                    startIndex,
                    count);
            System.arraycopy(
                    terminal.altStyles, startIndex + chars, terminal.altStyles, startIndex, count);

            Arrays.fill(terminal.altBuffer, endIndex, endIndex + chars, ' ');
            Arrays.fill(
                    terminal.altColors,
                    endIndex,
                    endIndex + chars,
                    TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.altColorsBackground, endIndex, endIndex + chars, c.Copy());
            Arrays.fill(
                    terminal.altStyles, endIndex, endIndex + chars, TerminalColors.DEFAULT_STYLE);
        } else {
            System.arraycopy(
                    terminal.buffer, startIndex + chars, terminal.buffer, startIndex, count);
            System.arraycopy(
                    terminal.colors, startIndex + chars, terminal.colors, startIndex, count);
            System.arraycopy(
                    terminal.colorsBackground,
                    startIndex + chars,
                    terminal.colorsBackground,
                    startIndex,
                    count);
            System.arraycopy(
                    terminal.styles, startIndex + chars, terminal.styles, startIndex, count);

            Arrays.fill(terminal.buffer, endIndex, endIndex + chars, ' ');
            Arrays.fill(
                    terminal.colors,
                    endIndex,
                    endIndex + chars,
                    TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.colorsBackground, endIndex, endIndex + chars, c.Copy());
            Arrays.fill(terminal.styles, endIndex, endIndex + chars, TerminalColors.DEFAULT_STYLE);
        }

        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(1 << y, (left, right) -> left | right));
    }

    private void shiftRight(int chars) {
        int startIndex =
                ((terminal.currentPrivateModeState.isAltBufferEnabled())
                                ? terminal.y * Terminal.WIDTH
                                : (terminal.y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT))
                                        * Terminal.WIDTH)
                        + terminal.x;
        int count = (Terminal.WIDTH - terminal.x) - chars;
        TerminalColors.ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
        }
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            System.arraycopy(
                    terminal.altBuffer, startIndex, terminal.altBuffer, startIndex + chars, count);
            System.arraycopy(
                    terminal.altColors, startIndex, terminal.altColors, startIndex + chars, count);
            System.arraycopy(
                    terminal.altColorsBackground,
                    startIndex,
                    terminal.altColorsBackground,
                    startIndex + chars,
                    count);
            System.arraycopy(
                    terminal.altStyles, startIndex, terminal.altStyles, startIndex + chars, count);
            Arrays.fill(terminal.altBuffer, startIndex, startIndex + chars, ' ');
            Arrays.fill(
                    terminal.altColors,
                    startIndex,
                    startIndex + chars,
                    TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.altColorsBackground, startIndex, startIndex + chars, c.Copy());
            Arrays.fill(
                    terminal.altStyles,
                    startIndex,
                    startIndex + chars,
                    TerminalColors.DEFAULT_STYLE);
        } else {
            System.arraycopy(
                    terminal.buffer, startIndex, terminal.buffer, startIndex + chars, count);
            System.arraycopy(
                    terminal.colors, startIndex, terminal.colors, startIndex + chars, count);
            System.arraycopy(
                    terminal.colorsBackground,
                    startIndex,
                    terminal.colorsBackground,
                    startIndex + chars,
                    count);
            System.arraycopy(
                    terminal.styles, startIndex, terminal.styles, startIndex + chars, count);
            Arrays.fill(terminal.buffer, startIndex, startIndex + chars, ' ');
            Arrays.fill(
                    terminal.colors,
                    startIndex,
                    startIndex + chars,
                    TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.colorsBackground, startIndex, startIndex + chars, c.Copy());
            Arrays.fill(
                    terminal.styles, startIndex, startIndex + chars, TerminalColors.DEFAULT_STYLE);
        }

        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(1 << terminal.y, (left, right) -> left | right));
    }
}