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

    /**
     * Slide the view/write window down one row. With {@code growWindow == false} (the
     * linefeed path) the write window grows into new territory first, capped at absolute
     * capacity. With {@code growWindow == true} (the scrollback mouse wheel) the view only
     * slides down through EXISTING rows and no-ops once glued — the wheel must never create
     * write-window rows.
     */
    public void incrementLastLineToDisplay(boolean growWindow) {
        if (terminal.scrollFirst != 0 || terminal.scrollLast != terminal.height - 1) return;
        final boolean originallyEqual = terminal.lastRowToDisplayMax == terminal.lastRowToDisplay;
        if (!growWindow) {
            terminal.lastRowToDisplayMax =
                    Math.min(
                            terminal.lastRowToDisplayMax + 1,
                            terminal.height * Terminal.SCROLL_BACK_COUNT);
        } else if (originallyEqual) {
            return;
        }
        if (originallyEqual) {
            terminal.lastRowToDisplay = terminal.lastRowToDisplayMax;
        } else {
            terminal.lastRowToDisplay =
                    Math.min(terminal.lastRowToDisplay + 1, terminal.lastRowToDisplayMax);
        }
        markAllRowsDirty();
    }

    public void decrementLastLineToDisplay() {
        if (terminal.scrollFirst != 0 || terminal.scrollLast != terminal.height - 1) return;
        terminal.lastRowToDisplay = Math.max(terminal.lastRowToDisplay - 1, terminal.height);
        markAllRowsDirty();
    }

    /**
     * Scroll the active scroll region up by {@code count} rows ({@code count <= 0} is a no-op).
     * There is no count clamp: the shift's floor/ceiling clip discards exactly the rows that
     * {@code count} per-row shifts would, so a count beyond the region empties it.
     *
     * <p>With full-page margins each row first grows the scrollback window while the VIEW has
     * room ({@code lastRowToDisplay < capacity}), batched here into one window update and one
     * dirty mark: a glued view gains a blank row at the bottom of the screen, while a
     * scrolled-back view at capacity merely catches up (no content moves). Only once the view
     * has reached absolute capacity does the rest of the count physically shift the whole
     * buffer, discarding the oldest scrollback rows off the top.
     *
     * <p>With partial margins the shift span depends on {@code scrollFirst}: at
     * {@code scrollFirst > 0} it is contained to the region, but at {@code scrollFirst == 0}
     * the region top IS the scrollback boundary, so the span extends to buffer row 1 and the
     * entire scrollback rides along — the scrolled-off line lands in scrollback and the oldest
     * lines drop off the buffer top. That is deliberate (it is what per-row scrolling did);
     * do not "fix" the floor to {@code scrollFirst + off} uniformly.
     */
    public void shiftUp(int count) {
        if (count <= 0) return;
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(
                    terminal.scrollFirst + 1,
                    terminal.scrollLast,
                    -count,
                    terminal.scrollFirst,
                    terminal.scrollLast);
            return;
        }
        final int off = terminal.lastRowToDisplayMax - terminal.height;
        if (terminal.scrollFirst == 0 && terminal.scrollLast == terminal.height - 1) {
            final int capacity = terminal.height * Terminal.SCROLL_BACK_COUNT;
            // Growth phase: one row of scrollback window per requested row while the VIEW has
            // room. Equivalent to per-row incrementLastLineToDisplay: a glued view stays glued
            // (each row exposes a new blank row at the bottom), while a scrolled-back view's
            // trailing distance shrinks each row once lrdMax is capacity-capped (pure view
            // catch-up — no content moves). Window fields and dirty state update once instead
            // of per row.
            final int grow = Math.min(count, capacity - terminal.lastRowToDisplay);
            if (grow > 0) {
                final boolean glued = terminal.lastRowToDisplay == terminal.lastRowToDisplayMax;
                terminal.lastRowToDisplayMax =
                        Math.min(terminal.lastRowToDisplayMax + grow, capacity);
                terminal.lastRowToDisplay = glued
                        ? terminal.lastRowToDisplayMax
                        : Math.min(terminal.lastRowToDisplay + grow, terminal.lastRowToDisplayMax);
                markAllRowsDirty();
            }
            // At capacity: one physical whole-buffer shift. The copy source starts at buffer
            // row {@code remaining} — the {@code remaining} oldest rows (scrollback included)
            // are discarded off the top and the bottom {@code remaining} rows blank, which is
            // exactly {@code remaining} per-row shifts.
            final int remaining = count - grow;
            if (remaining > 0) {
                shiftLines(1, capacity - 1, -remaining, 0, capacity - 1);
            }
        } else {
            // With scrollFirst == 0 the region top IS the scrollback boundary, so the shift
            // floor extends to buffer row 0 and the scrolled-off line lands in scrollback.
            // No count clamp: the floor clip discards exactly the rows n per-row shifts would.
            final int floor = terminal.scrollFirst == 0 ? 0 : terminal.scrollFirst + off;
            final int last = terminal.scrollLast + off;
            shiftLines(floor + 1, last, -count, floor, last);
        }
    }

    /**
     * Scroll the active scroll region down by {@code count} rows ({@code count <= 0} is a
     * no-op). Never touches the scrollback window: content pushed past the region bottom is
     * discarded. The count is clamped to the region height.
     */
    public void shiftDown(int count) {
        if (count <= 0) return;
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(
                    terminal.scrollFirst,
                    terminal.scrollLast,
                    count,
                    terminal.scrollFirst,
                    terminal.scrollLast);
            return;
        }
        final int off = terminal.lastRowToDisplayMax - terminal.height;
        if (terminal.scrollFirst == 0 && terminal.scrollLast == terminal.height - 1) {
            shiftLines(
                    terminal.lastRowToDisplayMax - terminal.height,
                    terminal.lastRowToDisplayMax - 1,
                    count,
                    terminal.lastRowToDisplayMax - terminal.height,
                    terminal.lastRowToDisplayMax - 1);
        } else {
            // No count clamp: the ceiling clip discards exactly the rows n per-row shifts
            // would (content pushed past the region bottom is discarded, never parked in the
            // unused tail).
            shiftLines(
                    terminal.scrollFirst + off,
                    terminal.scrollLast + off,
                    count,
                    terminal.scrollFirst + off,
                    terminal.scrollLast + off);
        }
    }

    /**
     * Pure one-row PHYSICAL shift of the scroll region — no scrollback window interaction.
     * IND/NEL pair this with {@link #incrementLastLineToDisplay()}, which owns the window
     * update; with full margins and view room this is deliberately a no-op (the window slide
     * IS the scroll — a physical shift here would double it). CH8's SU instead uses
     * {@link #shiftUp(int)}, which owns the growth phase itself.
     */
    public void shiftUpOne() {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            shiftLines(
                    terminal.scrollFirst + 1,
                    terminal.scrollLast,
                    -1,
                    terminal.scrollFirst,
                    terminal.scrollLast);
            return;
        }
        final int off = terminal.lastRowToDisplayMax - terminal.height;
        if (terminal.scrollFirst == 0 && terminal.scrollLast == terminal.height - 1) {
            if (terminal.lastRowToDisplay != terminal.height * Terminal.SCROLL_BACK_COUNT) {
                return;
            }
            shiftLines(1, terminal.height * Terminal.SCROLL_BACK_COUNT - 1, -1, 0,
                    terminal.height * Terminal.SCROLL_BACK_COUNT - 1);
        } else {
            final int floor = terminal.scrollFirst == 0 ? 0 : terminal.scrollFirst + off;
            shiftLines(floor + 1, terminal.scrollLast + off, -1, floor, terminal.scrollLast + off);
        }
    }

    public void shiftDownOne() {
        shiftDown(1);
    }

    /**
     * Raw shift of an absolute buffer-row span, clipped to {@code [floor, ceiling]}: rows pushed
     * past either bound are discarded (scrolled off), never an out-of-bounds access. Callers own
     * scroll-region containment (IL/DL clamp their line counts and pass their region bounds).
     */
    public void shiftLines(
            final int firstLine, final int lastLine, final int count, final int floor, final int ceiling) {
        TerminalLineShifter.shiftLines(terminal, firstLine, lastLine, count, floor, ceiling);
    }

    /**
     * Pure replay of one resolved wire shift operation against the MAIN buffer — the
     * client-side half of {@link #shiftLines}'s network recording. No dirty marks, no sink
     * recording: the diff application marks everything itself afterwards.
     */
    public void applyResolvedShift(
            final int copySrcRow,
            final int copyDstRow,
            final int copyRows,
            final int blankStartRow,
            final int blankRows) {
        final var geometry = new TerminalLineShifter.ShiftGeometry(
                copySrcRow, copyDstRow, copyRows, blankStartRow, blankRows, copyDstRow, copyDstRow + copyRows);
        TerminalLineShifter.applyResolved(terminal, false, geometry);
    }

    private void markAllRowsDirty() {
        // Loop, not (1L << height) - 1: Java masks shift counts to 0..63, so at
        // height 64 that idiom degenerates to 0 instead of all-ones.
        long dirtyLinesMask = 0;
        for (int i = 0; i < terminal.height; i++) {
            dirtyLinesMask |= 1L << i;
        }
        terminal.markDirty(dirtyLinesMask);
    }
}
