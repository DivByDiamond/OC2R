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
            Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.altColorsBackground, c.copy());
            Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
        } else {
            int startIndex = (terminal.lastRowToDisplayMax - Terminal.HEIGHT) * Terminal.WIDTH;
            int endIndex = startIndex + (Terminal.HEIGHT * Terminal.WIDTH);
            Arrays.fill(terminal.buffer, startIndex, endIndex, ' ');
            Arrays.fill(
                    terminal.colors, startIndex, endIndex, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.colorsBackground, startIndex, endIndex, c.copy());
            Arrays.fill(terminal.styles, startIndex, endIndex, TerminalColors.DEFAULT_STYLE);
        }
        terminal.renderers.forEach(model -> model.getDirtyMask().set(-1));
    }

    public void clearAlt() {
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();
        }
        Arrays.fill(terminal.altColorsBackground, c.copy());
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
    }

    public void clearLine(final int y) {
        clearLine(y, 0, Terminal.WIDTH);
    }

    public void clearLine(final int y, final int fromIndex, final int toIndex) {
        clearChars(y, fromIndex, toIndex - fromIndex);
    }

    /**
     * Erase {@code count} characters starting at column {@code x} on line {@code y}, filling with
     * blanks. Does not shift surrounding characters.
     */
    public void clearChars(final int y, final int x, final int count) {
        final int n = Math.max(Math.min(count, Terminal.WIDTH - x), 0);
        if (n == 0) return;
        final ColorData c = getCurrentBackgroundColor();
        final int from = getLinearIndex(y, x);
        final int to = from + n;
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            Arrays.fill(terminal.altBuffer, from, to, ' ');
            Arrays.fill(terminal.altColors, from, to, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.altColorsBackground, from, to, c.copy());
            Arrays.fill(terminal.altStyles, from, to, TerminalColors.DEFAULT_STYLE);
        } else {
            Arrays.fill(terminal.buffer, from, to, ' ');
            Arrays.fill(terminal.colors, from, to, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.colorsBackground, from, to, c.copy());
            Arrays.fill(terminal.styles, from, to, TerminalColors.DEFAULT_STYLE);
        }
        markDirty(y);
    }

    /**
     * Delete {@code count} characters at column {@code x} on line {@code y}, shifting remaining
     * characters left and filling blanks at the end.
     */
    public void deleteChars(final int y, final int x, final int count) {
        final int n = Math.max(Math.min(count, Terminal.WIDTH - x), 0);
        if (n == 0) return;
        final int remaining = Terminal.WIDTH - x - n;
        if (remaining <= 0) {
            clearChars(y, x, Terminal.WIDTH - x);
            return;
        }
        final ColorData c = getCurrentBackgroundColor();
        final int index = getLinearIndex(y, x);
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            System.arraycopy(
                    terminal.altBuffer, index + n, terminal.altBuffer, index, remaining);
            System.arraycopy(
                    terminal.altColors, index + n, terminal.altColors, index, remaining);
            System.arraycopy(
                    terminal.altColorsBackground,
                    index + n,
                    terminal.altColorsBackground,
                    index,
                    remaining);
            System.arraycopy(
                    terminal.altStyles, index + n, terminal.altStyles, index, remaining);
            Arrays.fill(terminal.altBuffer, index + remaining, index + remaining + n, ' ');
            Arrays.fill(
                    terminal.altColors,
                    index + remaining,
                    index + remaining + n,
                    TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(
                    terminal.altColorsBackground,
                    index + remaining,
                    index + remaining + n,
                    c.copy());
            Arrays.fill(
                    terminal.altStyles,
                    index + remaining,
                    index + remaining + n,
                    TerminalColors.DEFAULT_STYLE);
        } else {
            System.arraycopy(terminal.buffer, index + n, terminal.buffer, index, remaining);
            System.arraycopy(terminal.colors, index + n, terminal.colors, index, remaining);
            System.arraycopy(
                    terminal.colorsBackground,
                    index + n,
                    terminal.colorsBackground,
                    index,
                    remaining);
            System.arraycopy(terminal.styles, index + n, terminal.styles, index, remaining);
            Arrays.fill(terminal.buffer, index + remaining, index + remaining + n, ' ');
            Arrays.fill(
                    terminal.colors,
                    index + remaining,
                    index + remaining + n,
                    TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(
                    terminal.colorsBackground,
                    index + remaining,
                    index + remaining + n,
                    c.copy());
            Arrays.fill(
                    terminal.styles,
                    index + remaining,
                    index + remaining + n,
                    TerminalColors.DEFAULT_STYLE);
        }
        markDirty(y);
    }

    /**
     * Insert {@code count} blank characters at column {@code x} on line {@code y}, shifting
     * existing characters right. Characters pushed past the line width are lost.
     */
    public void insertChars(final int y, final int x, final int count) {
        final int n = Math.max(Math.min(count, Terminal.WIDTH - x), 0);
        if (n == 0) return;
        final int remaining = Terminal.WIDTH - x - n;
        if (remaining <= 0) {
            clearChars(y, x, Terminal.WIDTH - x);
            return;
        }
        final ColorData c = getCurrentBackgroundColor();
        final int index = getLinearIndex(y, x);
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            System.arraycopy(
                    terminal.altBuffer, index, terminal.altBuffer, index + n, remaining);
            System.arraycopy(
                    terminal.altColors, index, terminal.altColors, index + n, remaining);
            System.arraycopy(
                    terminal.altColorsBackground,
                    index,
                    terminal.altColorsBackground,
                    index + n,
                    remaining);
            System.arraycopy(
                    terminal.altStyles, index, terminal.altStyles, index + n, remaining);
            Arrays.fill(terminal.altBuffer, index, index + n, ' ');
            Arrays.fill(
                    terminal.altColors, index, index + n, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.altColorsBackground, index, index + n, c.copy());
            Arrays.fill(terminal.altStyles, index, index + n, TerminalColors.DEFAULT_STYLE);
        } else {
            System.arraycopy(terminal.buffer, index, terminal.buffer, index + n, remaining);
            System.arraycopy(terminal.colors, index, terminal.colors, index + n, remaining);
            System.arraycopy(
                    terminal.colorsBackground,
                    index,
                    terminal.colorsBackground,
                    index + n,
                    remaining);
            System.arraycopy(terminal.styles, index, terminal.styles, index + n, remaining);
            Arrays.fill(terminal.buffer, index, index + n, ' ');
            Arrays.fill(
                    terminal.colors, index, index + n, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.colorsBackground, index, index + n, c.copy());
            Arrays.fill(terminal.styles, index, index + n, TerminalColors.DEFAULT_STYLE);
        }
        markDirty(y);
    }

    private int getLinearIndex(final int y, final int x) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            return y * Terminal.WIDTH + x;
        }
        return (y + terminal.lastRowToDisplayMax - Terminal.HEIGHT) * Terminal.WIDTH + x;
    }

    private ColorData getCurrentBackgroundColor() {
        return switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> terminal.twoFiftySixColor;
            case TRUE_COLOR -> terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> terminal.sixteenColorBright;
            default -> TerminalColors.DEFAULT_BACKGROUND_COLOR;
        };
    }

    private void markDirty(final int y) {
        // Map the screen row to its dirty bit via getDirtyRow, mirroring setChar, so a char
        // op on row y marks the screen row where that buffer row currently renders — including
        // the scrollback offset (lastRowToDisplayMax - lastRowToDisplay). Plain 1 << y would
        // mark the wrong visible row when the view is scrolled back into scrollback.
        final int dirtyBit = 1 << TerminalBufferWriter.getDirtyRow(terminal, y);
        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(dirtyBit, (left, right) -> left | right));
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
