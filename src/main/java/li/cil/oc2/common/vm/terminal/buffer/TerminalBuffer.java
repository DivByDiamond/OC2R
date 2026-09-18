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
            if (terminal.altLineAttrs != null) {
                Arrays.fill(terminal.altLineAttrs, Terminal.LINE_ATTR_SINGLE);
            }
        } else {
            int startIndex = (terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
            int endIndex = startIndex + (terminal.height * terminal.width);
            Arrays.fill(terminal.buffer, startIndex, endIndex, ' ');
            Arrays.fill(
                    terminal.colors, startIndex, endIndex, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(terminal.colorsBackground, startIndex, endIndex, c.copy());
            Arrays.fill(terminal.styles, startIndex, endIndex, TerminalColors.DEFAULT_STYLE);
            if (terminal.lineAttrs != null) {
                final int startRow = terminal.lastRowToDisplayMax - terminal.height;
                final int endRow = startRow + terminal.height;
                Arrays.fill(terminal.lineAttrs, startRow, endRow, Terminal.LINE_ATTR_SINGLE);
            }
        }
        terminal.markAllDirty();
    }

    /**
     * Erase scrollback (ED 3 J, xterm's E3 — erase saved lines). Clears the scrollback buffer
     * above the visible window and resets the window to the bottom, keeping the visible screen
     * content. No-op on alt buffer or when scrollback is already empty.
     */
    public void clearScrollback() {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) return;
        if (terminal.lastRowToDisplayMax <= terminal.height) return;
        final int w = terminal.width;
        final int h = terminal.height;
        final int visibleStart = terminal.lastRowToDisplayMax - h;
        final int capacityRows = h * Terminal.SCROLL_BACK_COUNT;
        // Move the visible window to the front of the buffer.
        if (visibleStart != 0) {
            final int visibleCells = h * w;
            System.arraycopy(terminal.buffer, visibleStart * w, terminal.buffer, 0, visibleCells);
            System.arraycopy(terminal.colors, visibleStart * w, terminal.colors, 0, visibleCells);
            System.arraycopy(
                    terminal.colorsBackground, visibleStart * w, terminal.colorsBackground, 0, visibleCells);
            System.arraycopy(terminal.styles, visibleStart * w, terminal.styles, 0, visibleCells);
            if (terminal.lineAttrs != null) {
                System.arraycopy(terminal.lineAttrs, visibleStart, terminal.lineAttrs, 0, h);
            }
        }
        // Clear the tail (old scrollback + freed tail) to spaces.
        final int start = h * w;
        final int end = capacityRows * w;
        Arrays.fill(terminal.buffer, start, end, ' ');
        Arrays.fill(
                terminal.colors,
                start,
                end,
                TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(
                terminal.colorsBackground,
                start,
                end,
                terminal.currentBackgroundColor().copy());
        Arrays.fill(terminal.styles, start, end, TerminalColors.DEFAULT_STYLE);
        if (terminal.lineAttrs != null) {
            Arrays.fill(terminal.lineAttrs, h, capacityRows, Terminal.LINE_ATTR_SINGLE);
        }
        terminal.lastRowToDisplayMax = h;
        terminal.lastRowToDisplay = h;
        terminal.markAllBufferRowsDirty();
    }

    public void clearAlt() {
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(terminal.altColorsBackground, terminal.currentBackgroundColor().copy());
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
        if (terminal.altLineAttrs != null) {
            Arrays.fill(terminal.altLineAttrs, Terminal.LINE_ATTR_SINGLE);
        }
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
     * The column ICH/DCH/DL etc. shift up to, when clamping a count: the right margin if the
     * cursor sits inside the horizontal DECSLRM margins, otherwise the physical edge — xterm only
     * bounds these operations by DECSLRM when the cursor started inside it (util.c margin checks).
     */
    private int rightEditBoundary(final int x) {
        if (x >= terminal.scrollColFirst && x <= terminal.scrollColLast) {
            return terminal.scrollColLast + 1;
        }
        return terminal.width;
    }

    /**
     * Delete {@code count} characters at column {@code x} on line {@code y}, shifting remaining
     * characters left and filling blanks at the end.
     */
    public void deleteChars(final int y, final int x, final int count) {
        final int n = Math.clamp(count, 0, rightEditBoundary(x) - x);
        if (n == 0) return;
        final int remaining = rightEditBoundary(x) - x - n;
        if (remaining <= 0) {
            clearChars(y, x, rightEditBoundary(x) - x);
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
        final int n = Math.clamp(count, 0, rightEditBoundary(x) - x);
        if (n == 0) return;
        final int remaining = rightEditBoundary(x) - x - n;
        if (remaining <= 0) {
            clearChars(y, x, rightEditBoundary(x) - x);
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

    /**
     * Copy {@code count} columns starting at {@code x} from row {@code srcY} into the same column
     * range of row {@code dstY} — screen-row addressed (0..height-1), like {@link #clearChars}.
     * Used for DECSLRM-bounded IL/DL (§44, {@code IL}/{@code DL}): those insert/delete whole
     * lines, but when horizontal margins are active only the margin columns move, so unlike
     * {@link #shiftLines} (which swaps entire buffer rows, including through scrollback) this
     * moves a column sub-range one row at a time and never touches scrollback.
     */
    public void copyRowRange(final int srcY, final int dstY, final int x, final int count) {
        // Defensive bounds: callers (IL/DL) already compute correct row/column ranges, but this
        // method is public within the package, so guard the screen-row bounds and the horizontal
        // DECSLRM margin here too instead of trusting every future caller to get it right.
        if (srcY < 0 || srcY >= terminal.height || dstY < 0 || dstY >= terminal.height) return;
        final int marginLimit = x <= terminal.scrollColLast
                ? terminal.scrollColLast - x + 1
                : 0; // outside the DECSLRM margins: hard boundary, copy nothing
        final int maxCols = Math.max(0, Math.min(marginLimit, terminal.width - x));
        final int n = Math.clamp(count, 0, maxCols);
        if (n == 0) return;
        final int srcIndex = getLinearIndex(srcY, x);
        final int dstIndex = getLinearIndex(dstY, x);
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            System.arraycopy(terminal.altBuffer, srcIndex, terminal.altBuffer, dstIndex, n);
            System.arraycopy(terminal.altColors, srcIndex, terminal.altColors, dstIndex, n);
            System.arraycopy(
                    terminal.altColorsBackground, srcIndex, terminal.altColorsBackground, dstIndex, n);
            System.arraycopy(terminal.altStyles, srcIndex, terminal.altStyles, dstIndex, n);
        } else {
            System.arraycopy(terminal.buffer, srcIndex, terminal.buffer, dstIndex, n);
            System.arraycopy(terminal.colors, srcIndex, terminal.colors, dstIndex, n);
            System.arraycopy(
                    terminal.colorsBackground, srcIndex, terminal.colorsBackground, dstIndex, n);
            System.arraycopy(terminal.styles, srcIndex, terminal.styles, dstIndex, n);
        }
        markDirty(dstY);
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
