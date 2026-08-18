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

        final int srcIndex = firstLine * Terminal.WIDTH;
        final int charCount = (lastLine + 1) * Terminal.WIDTH - srcIndex;
        final int dstIndex = srcIndex + count * Terminal.WIDTH;
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

        final int clearCount = Math.abs(count * Terminal.WIDTH);
        Arrays.fill(terminal.altBuffer, shiftUpOrDown, shiftUpOrDown + clearCount, ' ');
        Arrays.fill(
                terminal.altColors,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_COLORS.Copy());
        Arrays.fill(
                terminal.altColorsBackground,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                c.Copy());
        Arrays.fill(
                terminal.altStyles,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_STYLE);

        int dirtyLinesMask = 0;
        final int dirtyStart = Math.min(firstLine, firstLine + count);
        final int dirtyEnd = Math.max(lastLine, lastLine + count);
        for (int i = dirtyStart; i <= dirtyEnd; i++) {
            dirtyLinesMask |= 1 << i;
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

        final int clearCount = Math.abs(count * Terminal.WIDTH);
        Arrays.fill(terminal.buffer, shiftUpOrDown, shiftUpOrDown + clearCount, ' ');
        Arrays.fill(
                terminal.colors,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_COLORS.Copy());
        Arrays.fill(
                terminal.colorsBackground, shiftUpOrDown, shiftUpOrDown + clearCount, c.Copy());
        Arrays.fill(
                terminal.styles,
                shiftUpOrDown,
                shiftUpOrDown + clearCount,
                TerminalColors.DEFAULT_STYLE);

        int dirtyLinesMask = 0;
        final int dirtyStart = Math.min(firstLine, firstLine + count);
        final int dirtyEnd = Math.max(lastLine, lastLine + count);
        for (int i = dirtyStart; i <= dirtyEnd; i++) {
            int localI = i + Terminal.HEIGHT - terminal.lastRowToDisplay;
            if (localI >= 0 && localI < Terminal.HEIGHT) {
                dirtyLinesMask |= 1 << localI;
            }
        }
        terminal.markDirty(dirtyLinesMask);
    }
}
