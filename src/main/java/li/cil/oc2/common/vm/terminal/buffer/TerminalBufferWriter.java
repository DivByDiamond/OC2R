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
            insertChar();
        }

        setChar(terminal.x, terminal.y, ch);
        terminal.x++;
    }

    private void insertChar() {
        // Insert mode: shift line right from cursor position, then place char
        final int charsToInsert = 1;
        final int startIndex =
                (terminal.currentPrivateModeState.isAltBufferEnabled()
                                ? terminal.y * Terminal.WIDTH
                                : (terminal.y + terminal.lastRowToDisplayMax - Terminal.HEIGHT)
                                        * Terminal.WIDTH)
                        + terminal.x;
        final int count = Terminal.WIDTH - terminal.x - charsToInsert;
        if (count <= 0) {
            return;
        }

        final TerminalColors.ColorData c = getBackgroundColor();
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            System.arraycopy(terminal.altBuffer, startIndex, terminal.altBuffer, startIndex + charsToInsert, count);
            System.arraycopy(terminal.altColors, startIndex, terminal.altColors, startIndex + charsToInsert, count);
            System.arraycopy(terminal.altColorsBackground, startIndex, terminal.altColorsBackground, startIndex + charsToInsert, count);
            System.arraycopy(terminal.altStyles, startIndex, terminal.altStyles, startIndex + charsToInsert, count);
            Arrays.fill(terminal.altBuffer, startIndex, startIndex + charsToInsert, ' ');
            Arrays.fill(terminal.altColors, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_COLORS.copy());
            Arrays.fill(terminal.altColorsBackground, startIndex, startIndex + charsToInsert, c.copy());
            Arrays.fill(terminal.altStyles, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_STYLE);
        } else {
            System.arraycopy(terminal.buffer, startIndex, terminal.buffer, startIndex + charsToInsert, count);
            System.arraycopy(terminal.colors, startIndex, terminal.colors, startIndex + charsToInsert, count);
            System.arraycopy(terminal.colorsBackground, startIndex, terminal.colorsBackground, startIndex + charsToInsert, count);
            System.arraycopy(terminal.styles, startIndex, terminal.styles, startIndex + charsToInsert, count);
            Arrays.fill(terminal.buffer, startIndex, startIndex + charsToInsert, ' ');
            Arrays.fill(terminal.colors, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_COLORS.copy());
            Arrays.fill(terminal.colorsBackground, startIndex, startIndex + charsToInsert, c.copy());
            Arrays.fill(terminal.styles, startIndex, startIndex + charsToInsert, TerminalColors.DEFAULT_STYLE);
        }
    }

    private TerminalColors.ColorData getBackgroundColor() {
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> {
                return terminal.sixteenColor;
            }
            case TWO_FIFTY_SIX_COLOR -> {
                return terminal.twoFiftySixColor;
            }
            case TRUE_COLOR -> {
                return terminal.backgroundColor;
            }
            case SIXTEEN_COLOR_BRIGHT -> {
                return terminal.sixteenColorBright;
            }
            case DEFAULT_BACKGROUND -> {
                return TerminalColors.DEFAULT_BACKGROUND_COLOR;
            }
            default -> {
                return TerminalColors.DEFAULT_BACKGROUND_COLOR;
            }
        }
    }

    public void setChar(final int x, final int y, final int ch) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            final int index = x + y * Terminal.WIDTH;

            terminal.altBuffer[index] = ch;
            setForegroundColor(terminal.altColors, index);
            setBackgroundColor(terminal.altColorsBackground, index);
            terminal.altStyles[index] = terminal.style;
            terminal.markDirty(1 << y);
        } else {
            int correctedY = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
            final int index = x + correctedY * Terminal.WIDTH;

            terminal.buffer[index] = ch;
            setForegroundColor(terminal.colors, index);
            setBackgroundColor(terminal.colorsBackground, index);
            terminal.styles[index] = terminal.style;
            int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y);
            int localY = Terminal.HEIGHT + globalY - terminal.lastRowToDisplay;
            terminal.markDirty(1 << localY);
        }
    }

    private void setForegroundColor(final TerminalColors.ColorData[] colors, final int index) {
        switch (terminal.currentForegroundColorMode) {
            case SIXTEEN_COLOR -> colors[index] = terminal.sixteenColor.copy();
            case TWO_FIFTY_SIX_COLOR -> colors[index] = terminal.twoFiftySixColor.copy();
            case TRUE_COLOR -> colors[index] = terminal.foregroundColor.copy();
            case SIXTEEN_COLOR_BRIGHT -> colors[index] = terminal.sixteenColorBright.copy();
            default -> {}
        }
    }

    private void setBackgroundColor(final TerminalColors.ColorData[] colors, final int index) {
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> colors[index] = terminal.sixteenColor.copy();
            case TWO_FIFTY_SIX_COLOR -> colors[index] = terminal.twoFiftySixColor.copy();
            case TRUE_COLOR -> colors[index] = terminal.backgroundColor.copy();
            case SIXTEEN_COLOR_BRIGHT -> colors[index] = terminal.sixteenColorBright.copy();
            case DEFAULT_BACKGROUND ->
                    colors[index] = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();
            default -> {}
        }
    }

    public static int getDirtyRow(final Terminal terminal, final int y) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            return y;
        }
        int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y);
        return Terminal.HEIGHT + globalY - terminal.lastRowToDisplay;
    }
}
