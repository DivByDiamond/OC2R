package li.cil.oc2.common.vm.terminal.buffer;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

public class TerminalBuffer {
    private final Terminal terminal;
    private final TerminalBufferScrolling scrolling;

    public TerminalBuffer(final Terminal terminal) {
        this.terminal = terminal;
        this.scrolling = new TerminalBufferScrolling(terminal);
    }

    public void clear() {
        ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
        }
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            Arrays.fill(terminal.altBuffer, ' ');
            Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.altColorsBackground, c.Copy());
            Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
        } else {
            int startIndex = (terminal.lastRowToDisplayMax - Terminal.HEIGHT) * Terminal.WIDTH;
            int endIndex = startIndex + (Terminal.HEIGHT * Terminal.WIDTH);
            Arrays.fill(terminal.buffer, startIndex, endIndex, ' ');
            Arrays.fill(
                    terminal.colors, startIndex, endIndex, TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(terminal.colorsBackground, startIndex, endIndex, c.Copy());
            Arrays.fill(terminal.styles, startIndex, endIndex, TerminalColors.DEFAULT_STYLE);
        }
        terminal.setCursorPos(0, 0);
        terminal.renderers.forEach(model -> model.getDirtyMask().set(-1));
    }

    public void clearAlt() {
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_COLORS.Copy());
        ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_COLORS.Copy();
        }
        Arrays.fill(terminal.altColorsBackground, c.Copy());
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
    }

    public void clearLine(final int y) {
        clearLine(y, 0, Terminal.WIDTH);
    }

    public void clearLine(final int y, final int fromIndex, final int toIndex) {
        terminal.currentForegroundColorMode = ColorMode.SIXTEEN_COLOR;
        ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
        }
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            Arrays.fill(
                    terminal.altBuffer,
                    y * Terminal.WIDTH + fromIndex,
                    y * Terminal.WIDTH + toIndex,
                    ' ');
            Arrays.fill(
                    terminal.altColors,
                    y * Terminal.WIDTH + fromIndex,
                    y * Terminal.WIDTH + toIndex,
                    TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(
                    terminal.altColorsBackground,
                    y * Terminal.WIDTH + fromIndex,
                    y * Terminal.WIDTH + toIndex,
                    c.Copy());
            Arrays.fill(
                    terminal.altStyles,
                    y * Terminal.WIDTH + fromIndex,
                    y * Terminal.WIDTH + toIndex,
                    TerminalColors.DEFAULT_STYLE);
        } else {
            int correctedY = (y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT));
            Arrays.fill(
                    terminal.buffer,
                    correctedY * Terminal.WIDTH + fromIndex,
                    correctedY * Terminal.WIDTH + toIndex,
                    ' ');
            Arrays.fill(
                    terminal.colors,
                    correctedY * Terminal.WIDTH + fromIndex,
                    correctedY * Terminal.WIDTH + toIndex,
                    TerminalColors.DEFAULT_COLORS.Copy());
            Arrays.fill(
                    terminal.colorsBackground,
                    correctedY * Terminal.WIDTH + fromIndex,
                    correctedY * Terminal.WIDTH + toIndex,
                    c.Copy());
            Arrays.fill(
                    terminal.styles,
                    correctedY * Terminal.WIDTH + fromIndex,
                    correctedY * Terminal.WIDTH + toIndex,
                    TerminalColors.DEFAULT_STYLE);
        }
        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask().accumulateAndGet(1 << y, (prev, next) -> prev | next));
    }

    public void incrementLastLineToDisplay() {
        scrolling.incrementLastLineToDisplay();
    }

    public void incrementLastLineToDisplay(boolean scroll) {
        scrolling.incrementLastLineToDisplay(scroll);
    }

    public void decrementLastLineToDisplay() {
        scrolling.decrementLastLineToDisplay();
    }

    public void shiftUp(int count) {
        scrolling.shiftUp(count);
    }

    public void shiftDown(int count) {
        scrolling.shiftDown(count);
    }

    public void shiftUpOne() {
        scrolling.shiftUpOne();
    }

    public void shiftDownOne() {
        scrolling.shiftDownOne();
    }

    public void shiftLines(final int firstLine, final int lastLine, final int count) {
        scrolling.shiftLines(firstLine, lastLine, count);
    }
}