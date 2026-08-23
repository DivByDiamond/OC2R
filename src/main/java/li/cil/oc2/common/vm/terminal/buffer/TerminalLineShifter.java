package li.cil.oc2.common.vm.terminal.buffer;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

final class TerminalLineShifter {
    static void shiftLines(
            final Terminal terminal,
            final int firstLine,
            final int lastLine,
            final int count) {
        if (count == 0) return;

        final int srcIndex = firstLine * terminal.width;
        final int charCount = (lastLine + 1) * terminal.width - srcIndex;
        final int dstIndex = srcIndex + count * terminal.width;
        ColorData c;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> c = terminal.sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
            case TRUE_COLOR -> c = terminal.backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
            default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
        }
        final int shiftUpOrDown = count > 0 ? srcIndex : (dstIndex + charCount);
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftAltBuffer(terminal, srcIndex, dstIndex, charCount, shiftUpOrDown, count, c, firstLine, lastLine);
        } else {
            shiftMainBuffer(terminal, srcIndex, dstIndex, charCount, shiftUpOrDown, count, c, firstLine, lastLine);
        }
    }

    private static void shiftAltBuffer(
            final Terminal terminal,
            final int srcIndex,
            final int dstIndex,
            final int charCount,
            final int shiftUpOrDown,
            final int count,
            final ColorData c,
            final int firstLine,
            final int lastLine) {
        System.arraycopy(terminal.altBuffer, srcIndex, terminal.altBuffer, dstIndex, charCount);
        System.arraycopy(terminal.altColors, srcIndex, terminal.altColors, dstIndex, charCount);
        System.arraycopy(
                terminal.altColorsBackground,
                srcIndex,
                terminal.altColorsBackground,
                dstIndex,
                charCount);
        System.arraycopy(terminal.altStyles, srcIndex, terminal.altStyles, dstIndex, charCount);

        final int clearCount = Math.abs(count * terminal.width);
        Arrays.fill(terminal.altBuffer, shiftUpOrDown, shiftUpOrDown + clearCount, ' ');
        Arrays.fill(
                terminal.altColors,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(
                terminal.altColorsBackground,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                c.copy());
        Arrays.fill(
                terminal.altStyles,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_STYLE);

        int dirtyLinesMask = 0;
        final int dirtyStart = Math.min(firstLine, firstLine + count);
        final int dirtyEnd = Math.max(lastLine, lastLine + count);
        for (int i = dirtyStart; i <= dirtyEnd; i++) {
            // Alt callers pass screen-row indices (terminal.y / scrollLast, no scrollback
            // offset), so the dirty row is i itself; guard the shift against out-of-range rows.
            if (i >= 0 && i < Terminal.HEIGHT) {
                dirtyLinesMask |= 1 << i;
            }
        }
        terminal.markDirty(dirtyLinesMask);
    }

    private static void shiftMainBuffer(
            final Terminal terminal,
            final int srcIndex,
            final int dstIndex,
            final int charCount,
            final int shiftUpOrDown,
            final int count,
            final ColorData c,
            final int firstLine,
            final int lastLine) {
        System.arraycopy(terminal.buffer, srcIndex, terminal.buffer, dstIndex, charCount);
        System.arraycopy(terminal.colors, srcIndex, terminal.colors, dstIndex, charCount);
        System.arraycopy(
                terminal.colorsBackground,
                srcIndex,
                terminal.colorsBackground,
                dstIndex,
                charCount);
        System.arraycopy(terminal.styles, srcIndex, terminal.styles, dstIndex, charCount);

        final int clearCount = Math.abs(count * terminal.width);
        Arrays.fill(terminal.buffer, shiftUpOrDown, shiftUpOrDown + clearCount, ' ');
        Arrays.fill(
                terminal.colors,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(
                terminal.colorsBackground, shiftUpOrDown, shiftUpOrDown + clearCount, c.copy());
        Arrays.fill(
                terminal.styles,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_STYLE);

        int dirtyLinesMask = 0;
        final int dirtyStart = Math.min(firstLine, firstLine + count);
        final int dirtyEnd = Math.max(lastLine, lastLine + count);
        for (int i = dirtyStart; i <= dirtyEnd; i++) {
            // firstLine/lastLine are buffer-row indices on the main buffer (callers offset by
            // lastRowToDisplayMax - HEIGHT); invert the renderer's screen->buffer map
            // (bufferRow = screenRow + lastRowToDisplay - HEIGHT) to recover the screen row.
            final int row = i + Terminal.HEIGHT - terminal.lastRowToDisplay;
            if (row >= 0 && row < Terminal.HEIGHT) {
                dirtyLinesMask |= 1 << row;
            }
        }
        terminal.markDirty(dirtyLinesMask);
    }
}
