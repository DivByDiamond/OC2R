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
        final ColorData c = terminal.currentBackgroundColor();
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            Arrays.fill(terminal.altBuffer, ' ');
            Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.altColorsBackground, c.copy());
            Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
        } else {
            int startIndex = (terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
            int endIndex = startIndex + (terminal.height * terminal.width);
            Arrays.fill(terminal.buffer, startIndex, endIndex, ' ');
            Arrays.fill(
                    terminal.colors, startIndex, endIndex, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.colorsBackground, startIndex, endIndex, c.copy());
            Arrays.fill(terminal.styles, startIndex, endIndex, TerminalColors.DEFAULT_STYLE);
        }
        terminal.markAllDirty();
    }

    public void clearAlt() {
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(terminal.altColorsBackground, terminal.currentBackgroundColor().copy());
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
    }

    public void clearLine(final int y) {
        clearLine(y, 0, terminal.width);
    }

    public void clearLine(final int y, final int fromIndex, final int toIndex) {
        clearChars(y, fromIndex, toIndex - fromIndex);
    }

    /**
     * Erase {@code count} characters starting at column {@code x} on line {@code y}, filling with
     * blanks. Does not shift surrounding characters.
     */
    public void clearChars(final int y, final int x, final int count) {
        final int n = Math.clamp(count, 0, terminal.width - x);
        if (n == 0) return;
        final ColorData c = terminal.currentBackgroundColor();
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
        final int n = Math.clamp(count, 0, terminal.width - x);
        if (n == 0) return;
        final int remaining = terminal.width - x - n;
        if (remaining <= 0) {
            clearChars(y, x, terminal.width - x);
            return;
        }
        final ColorData c = terminal.currentBackgroundColor();
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
        final int n = Math.clamp(count, 0, terminal.width - x);
        if (n == 0) return;
        final int remaining = terminal.width - x - n;
        if (remaining <= 0) {
            clearChars(y, x, terminal.width - x);
            return;
        }
        final ColorData c = terminal.currentBackgroundColor();
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
            return y * terminal.width + x;
        }
        return (y + terminal.lastRowToDisplayMax - terminal.height) * terminal.width + x;
    }

    private void markDirty(final int y) {
        // Map the screen row to its dirty bit via getDirtyRow, mirroring setChar, so a char
        // op on row y marks the screen row where that buffer row currently renders — including
        // the scrollback offset (lastRowToDisplayMax - lastRowToDisplay). Plain 1 << y would
        // mark the wrong visible row when the view is scrolled back into scrollback.
        TerminalBufferWriter.markDirtyLine(terminal, TerminalBufferWriter.getDirtyRow(terminal, y));
    }

    public void incrementLastLineToDisplay() {
        scrolling.incrementLastLineToDisplay();
    }

    public void incrementLastLineToDisplay(boolean growWindow) {
        scrolling.incrementLastLineToDisplay(growWindow);
    }

    public void decrementLastLineToDisplay() {
        scrolling.decrementLastLineToDisplay();
    }

    /**
     * Scroll the active scroll region up by {@code count} rows (see
     * {@link TerminalBufferScrolling#shiftUp} for the scrollback window semantics).
     */
    public void shiftUp(final int count) {
        scrolling.shiftUp(count);
    }

    /**
     * Scroll the active scroll region down by {@code count} rows (see
     * {@link TerminalBufferScrolling#shiftDown}).
     */
    public void shiftDown(final int count) {
        scrolling.shiftDown(count);
    }

    public void shiftUpOne() {
        scrolling.shiftUpOne();
    }

    public void shiftDownOne() {
        scrolling.shiftDownOne();
    }

    /**
     * Raw shift of an absolute buffer-row span, clipped to {@code [floor, ceiling]}: rows pushed
     * past either bound are discarded (scrolled off), never an out-of-bounds access. Callers own
     * scroll-region containment (IL/DL clamp their line counts and pass their region bounds).
     */
    public void shiftLines(
            final int firstLine, final int lastLine, final int count, final int floor, final int ceiling) {
        scrolling.shiftLines(firstLine, lastLine, count, floor, ceiling);
    }

    /**
     * Pure replay of one resolved wire shift operation against the MAIN buffer — the
     * client-side half of the shift recording in {@link #shiftLines}. No dirty marks, no sink
     * recording: the diff application marks everything itself afterwards.
     */
    public void applyResolvedShift(
            final int copySrcRow,
            final int copyDstRow,
            final int copyRows,
            final int blankStartRow,
            final int blankRows) {
        scrolling.applyResolvedShift(copySrcRow, copyDstRow, copyRows, blankStartRow, blankRows);
    }
}
