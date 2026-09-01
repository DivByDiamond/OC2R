package li.cil.oc2.common.vm.terminal.buffer;

import li.cil.oc2.common.vm.terminal.Terminal;

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
                            Terminal.HEIGHT * Terminal.SCROLL_BACK_COUNT);
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
        terminal.markDirty(dirtyLinesMask);
    }

    public void decrementLastLineToDisplay() {
        if (terminal.scrollFirst != 0 || terminal.scrollLast != Terminal.HEIGHT - 1) return;
        terminal.lastRowToDisplay = Math.max(terminal.lastRowToDisplay - 1, 24);
        int dirtyLinesMask = 0;
        for (int i = 0; i <= 23; i++) {
            dirtyLinesMask |= 1 << i;
        }
        terminal.markDirty(dirtyLinesMask);
    }

    public void shiftUp(int count) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(terminal.scrollFirst + 1, terminal.scrollLast, -count);
        } else {
            if (terminal.lastRowToDisplay == Terminal.HEIGHT * Terminal.SCROLL_BACK_COUNT
                    || terminal.scrollLast != Terminal.HEIGHT - 1
                    || terminal.scrollFirst != 0) {
                shiftLines(
                        terminal.scrollFirst != 0
                                ? terminal.scrollFirst
                                        + terminal.lastRowToDisplayMax
                                        - Terminal.HEIGHT
                                        + 1
                                : 1,
                        terminal.scrollLast != Terminal.HEIGHT - 1
                                ? terminal.scrollLast + terminal.lastRowToDisplayMax - Terminal.HEIGHT
                                : (Terminal.HEIGHT * Terminal.SCROLL_BACK_COUNT) - 1,
                        -count);
            }
        }
    }

    public void shiftDown(int countParam) {
        // Shifting more than the visible height blanks the whole window either way;
        // clamping keeps the index arithmetic below in valid range.
        final int count = Math.min(countParam, Terminal.HEIGHT);
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(terminal.scrollFirst, terminal.scrollLast - 1, count);
        } else if (terminal.scrollFirst == 0 && terminal.scrollLast == Terminal.HEIGHT - 1) {
            // Shift within the physical window at the bottom of the scrollback
            // (lastRowToDisplayMax, like every other main-buffer path): lines pushed off
            // the bottom are discarded, top lines become blank. Passing lastLine reduced
            // by count shrinks the copied region so arraycopy never writes past the
            // buffer end when the window sits at the absolute buffer bottom.
            shiftLines(
                    terminal.lastRowToDisplayMax - Terminal.HEIGHT,
                    terminal.lastRowToDisplayMax - 1 - count,
                    count);
        } else {
            shiftLines(
                    terminal.scrollFirst + terminal.lastRowToDisplayMax - Terminal.HEIGHT,
                    terminal.scrollLast + terminal.lastRowToDisplayMax - Terminal.HEIGHT - 1,
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
        TerminalLineShifter.shiftLines(terminal, firstLine, lastLine, count);
    }
}