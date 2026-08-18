package li.cil.oc2.common.vm.terminal.buffer;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.index.NEL;

public class TerminalBufferWriter {
    private final Terminal terminal;

    public TerminalBufferWriter(final Terminal terminal) {
        this.terminal = terminal;
    }

    public void putChar(final int ch) {
        if (Character.isISOControl(ch)) return;

        if (terminal.x >= Terminal.WIDTH) {
            if (terminal.currentPrivateModeState.DECAWM) {
                NEL.execute(terminal);
            } else {
                terminal.setCursorPos(Terminal.WIDTH - 1, terminal.y);
            }
        }

        if (terminal.currentModeState.IRM) {
            // Insert mode: shift line right from cursor position, then place char
            int charsToInsert = 1;
            int startIndex =
                    ((terminal.currentPrivateModeState.isAltBufferEnabled())
                                    ? terminal.y * Terminal.WIDTH
                                    : (terminal.y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT))
                                            * Terminal.WIDTH)
                            + terminal.x;
            int count = Terminal.WIDTH - terminal.x - charsToInsert;
            if (count > 0) {
                TerminalColors.ColorData c;
                switch (terminal.currentBackgroundColorMode) {
                    case SIXTEEN_COLOR -> c = terminal.sixteenColor;
                    case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
                    case TRUE_COLOR -> c = terminal.backgroundColor;
                    case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
                    case DEFAULT_BACKGROUND -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
                    default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
                }
                if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                    System.arraycopy(terminal.altBuffer, startIndex, terminal.altBuffer, startIndex + charsToInsert, count);
                    System.arraycopy(terminal.altColors, startIndex, terminal.altColors, startIndex + charsToInsert, count);
                    System.arraycopy(terminal.altColorsBackground, startIndex, terminal.altColorsBackground, startIndex + charsToInsert, count);
                    System.arraycopy(terminal.altStyles, startIndex, terminal.altStyles, startIndex + charsToInsert, count);
                    Arrays.fill(terminal.altBuffer, startIndex, startIndex + charsToInsert, ' ');
                    Arrays.fill(terminal.altColors, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_COLORS.Copy());
                    Arrays.fill(terminal.altColorsBackground, startIndex, startIndex + charsToInsert, c.Copy());
                    Arrays.fill(terminal.altStyles, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_STYLE);
                } else {
                    System.arraycopy(terminal.buffer, startIndex, terminal.buffer, startIndex + charsToInsert, count);
                    System.arraycopy(terminal.colors, startIndex, terminal.colors, startIndex + charsToInsert, count);
                    System.arraycopy(terminal.colorsBackground, startIndex, terminal.colorsBackground, startIndex + charsToInsert, count);
                    System.arraycopy(terminal.styles, startIndex, terminal.styles, startIndex + charsToInsert, count);
                    Arrays.fill(terminal.buffer, startIndex, startIndex + charsToInsert, ' ');
                    Arrays.fill(terminal.colors, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_COLORS.Copy());
                    Arrays.fill(terminal.colorsBackground, startIndex, startIndex + charsToInsert, c.Copy());
                    Arrays.fill(terminal.styles, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_STYLE);
                }
            }
        }

        setChar(terminal.x, terminal.y, ch);
        terminal.x++;
    }

    public void setChar(final int x, final int y, final int ch) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            final int index = x + y * Terminal.WIDTH;

            terminal.altBuffer[index] = ch;

            switch (terminal.currentForegroundColorMode) {
                case SIXTEEN_COLOR -> terminal.altColors[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.altColors[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR -> terminal.altColors[index] = terminal.foregroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.altColors[index] = terminal.sixteenColorBright.Copy();
                default -> {}
            }

            switch (terminal.currentBackgroundColorMode) {
                case SIXTEEN_COLOR ->
                        terminal.altColorsBackground[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.altColorsBackground[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR ->
                        terminal.altColorsBackground[index] = terminal.backgroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.altColorsBackground[index] = terminal.sixteenColorBright.Copy();
                case DEFAULT_BACKGROUND ->
                        terminal.altColorsBackground[index] = TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy();
                default -> {}
            }

            terminal.altStyles[index] = terminal.style;
            terminal.markDirty(1 << (y));
        } else {
            int correctedY = (y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT));
            final int index = x + correctedY * Terminal.WIDTH;

            terminal.buffer[index] = ch;

            switch (terminal.currentForegroundColorMode) {
                case SIXTEEN_COLOR -> terminal.colors[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.colors[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR -> terminal.colors[index] = terminal.foregroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.colors[index] = terminal.sixteenColorBright.Copy();
                default -> {}
            }

            switch (terminal.currentBackgroundColorMode) {
                case SIXTEEN_COLOR ->
                        terminal.colorsBackground[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.colorsBackground[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR ->
                        terminal.colorsBackground[index] = terminal.backgroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.colorsBackground[index] = terminal.sixteenColorBright.Copy();
                case DEFAULT_BACKGROUND ->
                        terminal.colorsBackground[index] = TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy();
                default -> {}
            }

            terminal.styles[index] = terminal.style;
            int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y);
            int localY = Terminal.HEIGHT + (globalY - terminal.lastRowToDisplay);
            terminal.markDirty(1 << (localY));
        }
    }

    public static int getDirtyRow(final Terminal terminal, final int y) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            return y;
        }
        int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y);
        return Terminal.HEIGHT + (globalY - terminal.lastRowToDisplay);
    }
}
