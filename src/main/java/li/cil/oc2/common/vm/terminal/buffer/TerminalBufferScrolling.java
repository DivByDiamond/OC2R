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
        } else if (terminal.scrollFirst == 0 && terminal.scrollLast == Terminal.HEIGHT - 1) {
            shiftLines(
                    terminal.lastRowToDisplay - Terminal.HEIGHT,
                    terminal.lastRowToDisplay - 1,
                    count);
        } else {
            shiftLines(
                    terminal.scrollFirst + (terminal.lastRowToDisplayMax - Terminal.HEIGHT),
                    terminal.scrollLast + (terminal.lastRowToDisplayMax - Terminal.HEIGHT) - 1,
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