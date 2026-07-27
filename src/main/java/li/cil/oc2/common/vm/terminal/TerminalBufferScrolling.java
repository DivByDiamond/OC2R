package li.cil.oc2.common.vm.terminal;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorData;

class TerminalBufferScrolling {
    private final Terminal terminal;

    TerminalBufferScrolling(final Terminal terminal) {
        this.terminal = terminal;
    }

    public void incrementLastLineToDisplay() {
        incrementLastLineToDisplay(false);
    }

    public void incrementLastLineToDisplay(boolean scroll) {
        if (terminal.scrollFirst != 0 || terminal.scrollLast != Terminal.HEIGHT - 1) return;
        boolean originallyEqual = terminal.lastRowToDisplayMax == terminal.lastRowToDisplay;
        if (!scroll) {
            terminal.lastRowToDisplayMax =
                    Math.min(
                            terminal.lastRowToDisplayMax + 1,
                            (Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT));
        } else if (terminal.lastRowToDisplay == terminal.lastRowToDisplayMax) {
            return;
        }

        if (originallyEqual) {
            terminal.lastRowToDisplay = terminal.lastRowToDisplayMax;
        } else {
            terminal.lastRowToDisplay =
                    Math.min(terminal.lastRowToDisplay + 1, terminal.lastRowToDisplayMax);
        }

        int dirtyLinesMask = 0;
        for (int i = 0; i <= 23; i++) {
            dirtyLinesMask |= 1 << i;
        }
        final int finalDirtyLinesMask = dirtyLinesMask;
        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(
                                        finalDirtyLinesMask, (left, right) -> left | right));
    }

    public void decrementLastLineToDisplay() {
        if (terminal.scrollFirst != 0 || terminal.scrollLast != Terminal.HEIGHT - 1) return;
        terminal.lastRowToDisplay = Math.max(terminal.lastRowToDisplay - 1, 24);
        int dirtyLinesMask = 0;
        for (int i = 0; i <= 23; i++) {
            dirtyLinesMask |= 1 << i;
        }
        final int finalDirtyLinesMask = dirtyLinesMask;
        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(
                                        finalDirtyLinesMask, (left, right) -> left | right));
    }

    public void shiftUp(int count) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(terminal.scrollFirst + 1, terminal.scrollLast, -count);
        } else {
            if (terminal.lastRowToDisplay == Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT
                    || terminal.scrollLast != Terminal.HEIGHT - 1
                    || terminal.scrollFirst != 0) {
                shiftLines(
                        terminal.scrollFirst != 0
                                ? (terminal.scrollFirst
                                                + (terminal.lastRowToDisplayMax - Terminal.HEIGHT))
                                        + 1
                                : 1,
                        terminal.scrollLast != Terminal.HEIGHT - 1
                                ? terminal.scrollLast
                                        + (terminal.lastRowToDisplayMax - Terminal.HEIGHT)
                                : (Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT) - 1,
                        -count);
            }
        }
    }

    public void shiftDown(int count) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(terminal.scrollFirst, terminal.scrollLast - 1, count);
        } else {
            shiftLines(
                    terminal.scrollFirst != 0
                            ? (terminal.scrollFirst
                                    + (terminal.lastRowToDisplayMax - Terminal.HEIGHT))
                            : 0,
                    terminal.scrollLast != Terminal.HEIGHT - 1
                            ? terminal.scrollLast
                                    + (terminal.lastRowToDisplayMax - Terminal.HEIGHT)
                                    - 1
                            : ((Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT) - 1) - 1,
                    count);
        }
    }

    public void shiftUpOne() {
        shiftUp(1);
    }

    public void shiftDownOne() {
        shiftDown(1);
    }

    public void shiftLines(final int firstLine, final int lastLine, final int count) {
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
            final int finalDirtyLinesMask = dirtyLinesMask;
            terminal.renderers.forEach(
                    model ->
                            model.getDirtyMask()
                                    .accumulateAndGet(
                                            finalDirtyLinesMask, (left, right) -> left | right));
        } else {
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
                int globalI = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - i);
                int localI = Terminal.HEIGHT + (globalI - terminal.lastRowToDisplay);
                dirtyLinesMask |= 1 << localI;
            }
            final int finalDirtyLinesMask = dirtyLinesMask;
            terminal.renderers.forEach(
                    model ->
                            model.getDirtyMask()
                                    .accumulateAndGet(
                                            finalDirtyLinesMask, (left, right) -> left | right));
        }
    }
}