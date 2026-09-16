package li.cil.oc2.common.vm.terminal.buffer;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

/**
 * Row-shifting primitive for the terminal buffers. Total for any input: rows pushed past
 * {@code floor} (shifting up) or {@code ceiling} (shifting down) are DISCARDED — scrolled off
 * the edge — never wrapped onto an unrelated row and never an out-of-bounds access. Region
 * containment is the caller's job (see {@link TerminalBufferScrolling}); this class only
 * guarantees the shift stays inside the physical array.
 */
final class TerminalLineShifter {
    private TerminalLineShifter() {}

    /**
     * Shift the inclusive row span {@code [firstLine, lastLine]} by {@code count} rows
     * ({@code >0} down, {@code <0} up), blanking the vacated rows with the current SGR
     * background. {@code floor}/{@code ceiling} bound the rows the shift may touch; content
     * that would land beyond them is discarded.
     */
    static void shiftLines(
            final Terminal terminal,
            final int firstLine,
            final int lastLine,
            final int count,
            final int floor,
            final int ceiling) {
        if (count == 0) return;

        final ColorData c = terminal.currentBackgroundColor();

        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shift(
                    terminal.altBuffer,
                    terminal.altColors,
                    terminal.altColorsBackground,
                    terminal.altStyles,
                    terminal, firstLine, lastLine, count, floor, ceiling, c);
        } else {
            shift(
                    terminal.buffer,
                    terminal.colors,
                    terminal.colorsBackground,
                    terminal.styles,
                    terminal, firstLine, lastLine, count, floor, ceiling, c);
        }
    }

    @SuppressWarnings("PMD.ExcessiveParameterList") // the four parallel buffers + the shift geometry; splitting would pass a holder object per call on a hot path
    private static void shift(
            final int[] chars,
            final ColorData[] colors,
            final ColorData[] colorsBackground,
            final byte[] styles,
            final Terminal terminal,
            final int firstLine,
            final int lastLine,
            final int count,
            final int floor,
            final int ceiling,
            final ColorData background) {
        final int width = terminal.width;

        final int copySrcRow;
        final int copyDstRow;
        final int copyRows;
        final int blankStartRow;
        final int blankRows;
        final int dirtyTop;
        final int dirtyBottom;
        if (count < 0) { // up: content moves toward row 0, rows scrolled off the top are discarded
            final int k = -count;
            final int dstTop = Math.max(floor, firstLine - k);
            copySrcRow = dstTop + k;
            copyDstRow = dstTop;
            copyRows = Math.max(0, lastLine + 1 - copySrcRow);
            blankStartRow = dstTop + copyRows;
            blankRows = lastLine + 1 - blankStartRow;
            dirtyTop = dstTop;
            dirtyBottom = lastLine;
        } else { // down: content moves toward the ceiling, rows pushed past it are discarded
            final int k = count;
            final int dstBottom = Math.min(ceiling, lastLine + k);
            copyRows = Math.max(0, dstBottom - k + 1 - firstLine);
            copySrcRow = firstLine;
            copyDstRow = firstLine + k;
            blankStartRow = firstLine;
            blankRows = Math.min(k, dstBottom - firstLine + 1);
            dirtyTop = firstLine;
            dirtyBottom = dstBottom;
        }

        if (copyRows > 0) {
            System.arraycopy(chars, copySrcRow * width, chars, copyDstRow * width, copyRows * width);
            System.arraycopy(colors, copySrcRow * width, colors, copyDstRow * width, copyRows * width);
            System.arraycopy(
                    colorsBackground,
                    copySrcRow * width,
                    colorsBackground,
                    copyDstRow * width,
                    copyRows * width);
            System.arraycopy(styles, copySrcRow * width, styles, copyDstRow * width, copyRows * width);
        }
        if (blankRows > 0) {
            Arrays.fill(chars, blankStartRow * width, (blankStartRow + blankRows) * width, ' ');
            Arrays.fill(
                    colors,
                    blankStartRow * width,
                    (blankStartRow + blankRows) * width,
                    TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(
                    colorsBackground,
                    blankStartRow * width,
                    (blankStartRow + blankRows) * width,
                    background.copy());
            Arrays.fill(
                    styles,
                    blankStartRow * width,
                    (blankStartRow + blankRows) * width,
                    TerminalColors.DEFAULT_STYLE);
        }

        markDirty(terminal, dirtyTop, dirtyBottom);
    }

    /**
     * Mark the touched buffer rows dirty. Buffer row {@code b} renders at screen row
     * {@code b + height - lastRowToDisplay} on the main buffer (the view is anchored at
     * {@code lastRowToDisplay}); alt-buffer rows are screen rows directly.
     */
    private static void markDirty(final Terminal terminal, final int top, final int bottom) {
        long dirtyLinesMask = 0;
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        for (int i = Math.max(top, 0); i <= bottom; i++) {
            if (alt) {
                if (i < terminal.height) {
                    dirtyLinesMask |= 1L << i;
                }
            } else {
                final int row = i + terminal.height - terminal.lastRowToDisplay;
                if (row >= 0 && row < terminal.height) {
                    dirtyLinesMask |= 1L << row;
                }
            }
        }
        if (dirtyLinesMask != 0) {
            terminal.markDirty(dirtyLinesMask);
        }
    }
}
