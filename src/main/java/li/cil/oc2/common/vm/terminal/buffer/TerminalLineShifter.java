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
     * that would land beyond them is discarded. Main-buffer shifts are additionally recorded
     * in the network diff sink: a shift moves content at absolute indices that need not be
     * screen-visible, so row payloads alone cannot keep a client's scrolled-back scrollback
     * copy in sync — the operation itself crosses the wire and the client replays the same
     * memmove (see the shift-op replay in {@code TerminalDiff.apply}).
     */
    static void shiftLines(
            final Terminal terminal,
            final int firstLine,
            final int lastLine,
            final int count,
            final int floor,
            final int ceiling) {
        if (count == 0) return;

        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        final ShiftGeometry geometry = apply(terminal, alt, firstLine, lastLine, count, floor, ceiling);
        markDirty(terminal, geometry);
        if (!alt && geometry != null) {
            // Record the RESOLVED memmove geometry: the client replays exactly this, with no
            // re-derivation (and no dependence on the client's own floor/ceiling or background).
            terminal.recordNetworkShift(
                    geometry.copySrcRow(),
                    geometry.copyDstRow(),
                    geometry.copyRows(),
                    geometry.blankStartRow(),
                    geometry.blankRows());
        }
    }

    /**
     * The pure shift primitive: memmove the surviving rows and blank the vacated rows. Marks
     * nothing and records nothing — shared by {@link #shiftLines} and the client-side replay
     * of wire shift operations (TerminalDiff), which must not feed the local network sink.
     *
     * @return the resolved shift geometry, or null for a no-op.
     */
    static ShiftGeometry apply(
            final Terminal terminal,
            final boolean alt,
            final int firstLine,
            final int lastLine,
            final int count,
            final int floor,
            final int ceiling) {
        final ShiftGeometry geometry = geometry(firstLine, lastLine, count, floor, ceiling);
        if (geometry == null) {
            return null;
        }
        applyResolved(terminal, alt, geometry);
        return geometry;
    }

    /**
     * Replay a RESOLVED shift geometry against the buffer arrays. No marks, no recording —
     * the client-side half of {@link #shiftLines}'s network recording.
     */
    static void applyResolved(final Terminal terminal, final boolean alt, final ShiftGeometry g) {
        final int width = terminal.width;
        final int[] chars = alt ? terminal.altBuffer : terminal.buffer;
        final ColorData[] colors = alt ? terminal.altColors : terminal.colors;
        final ColorData[] colorsBackground = alt
                ? terminal.altColorsBackground : terminal.colorsBackground;
        final byte[] styles = alt ? terminal.altStyles : terminal.styles;

        if (g.copyRows() > 0) {
            System.arraycopy(chars, g.copySrcRow() * width, chars, g.copyDstRow() * width, g.copyRows() * width);
            System.arraycopy(colors, g.copySrcRow() * width, colors, g.copyDstRow() * width, g.copyRows() * width);
            System.arraycopy(
                    colorsBackground,
                    g.copySrcRow() * width,
                    colorsBackground,
                    g.copyDstRow() * width,
                    g.copyRows() * width);
            System.arraycopy(
                    styles,
                    g.copySrcRow() * width,
                    styles,
                    g.copyDstRow() * width,
                    g.copyRows() * width);
        }
        if (g.blankRows() > 0) {
            final ColorData c = terminal.currentBackgroundColor();
            Arrays.fill(
                    chars,
                    g.blankStartRow() * width,
                    (g.blankStartRow() + g.blankRows()) * width,
                    ' ');
            Arrays.fill(
                    colors,
                    g.blankStartRow() * width,
                    (g.blankStartRow() + g.blankRows()) * width,
                    TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
            Arrays.fill(
                    colorsBackground,
                    g.blankStartRow() * width,
                    (g.blankStartRow() + g.blankRows()) * width,
                    c.copy());
            Arrays.fill(
                    styles,
                    g.blankStartRow() * width,
                    (g.blankStartRow() + g.blankRows()) * width,
                    TerminalColors.DEFAULT_STYLE);
        }
    }

    /**
     * The resolved geometry of a shift: which rows memmove where ({@code copySrcRow} ->
     * {@code copyDstRow}, {@code copyRows} rows — the content delta is {@code copyDstRow -
     * copySrcRow}) and which rows blank ({@code blankStartRow}, {@code blankRows}). The
     * dirty-mark and wire-replay halves both derive from this, so they cannot drift apart.
     */
    record ShiftGeometry(
            int copySrcRow, int copyDstRow, int copyRows, int blankStartRow, int blankRows, int top, int bottom) {}

    private static ShiftGeometry geometry(
            final int firstLine, final int lastLine, final int count, final int floor, final int ceiling) {
        final int copySrcRow;
        final int copyDstRow;
        final int copyRows;
        final int blankStartRow;
        final int blankRows;
        final int top;
        final int bottom;
        if (count < 0) { // up: content moves toward row 0, rows scrolled off the top are discarded
            final int k = -count;
            final int dstTop = Math.max(floor, firstLine - k);
            copySrcRow = dstTop + k;
            copyDstRow = dstTop;
            copyRows = Math.max(0, lastLine + 1 - copySrcRow);
            blankStartRow = dstTop + copyRows;
            blankRows = lastLine + 1 - blankStartRow;
            top = dstTop;
            bottom = lastLine;
        } else { // down: content moves toward the ceiling, rows pushed past it are discarded
            final int k = count;
            final int dstBottom = Math.min(ceiling, lastLine + k);
            copyRows = Math.max(0, dstBottom - k + 1 - firstLine);
            copySrcRow = firstLine;
            copyDstRow = firstLine + k;
            blankStartRow = firstLine;
            blankRows = Math.min(k, dstBottom - firstLine + 1);
            top = firstLine;
            bottom = dstBottom;
        }
        if (copyRows <= 0 && blankRows <= 0) {
            return null;
        }
        return new ShiftGeometry(copySrcRow, copyDstRow, copyRows, blankStartRow, blankRows, top, bottom);
    }

    /**
     * Mark the touched buffer rows dirty. Buffer row {@code b} renders at screen row
     * {@code b + height - lastRowToDisplay} on the main buffer (the view is anchored at
     * {@code lastRowToDisplay}); alt-buffer rows are screen rows directly.
     */
    private static void markDirty(final Terminal terminal, final ShiftGeometry geometry) {
        if (geometry == null) {
            return;
        }
        long dirtyLinesMask = 0;
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        for (int i = Math.max(geometry.top(), 0); i <= geometry.bottom(); i++) {
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
