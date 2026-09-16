package li.cil.oc2.common.vm.terminal;

import static li.cil.oc2.common.vm.terminal.EscapeLiterals.CSI;
import static li.cil.oc2.common.vm.terminal.EscapeLiterals.ESC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import org.junit.jupiter.api.Test;

/**
 * The n-ary scroll primitives ({@code shiftUp(n)}/{@code shiftDown(n)}) must be exactly
 * equivalent to n per-row scrolls — that equivalence IS the spec: the per-row loop was the
 * only user-visible semantics before batching, and CH8/CH9 delegate to the batched forms.
 *
 * <p>The oracle below is the corrected per-row loop: each row does a window-slide (while the
 * view has room) XOR a physical shift (at capacity), never both. The pre-batching CH8 loop
 * ran an unconditional {@code shiftUpOne()} after the conditional slide, so the iteration
 * that crossed the capacity boundary performed BOTH — n+1 visual scrolls for n requested,
 * discarding one extra scrollback line.
 * {@code shiftUpCrossingCapacityBoundaryScrollsExactlyNRows} pins the fix through the real
 * CSI path; the matrix proves the batched forms match the corrected loop everywhere else.
 */
@SuppressWarnings("PMD.CyclomaticComplexity") // property matrix: margin × window-state × count is the point
class TerminalScrollingNaryTest {
    private static final int CAPACITY = Terminal.HEIGHT * Terminal.SCROLL_BACK_COUNT;

    // scrollFirst/scrollLast pairs: full page, top-anchored, bottom-anchored, interior, degenerate.
    private static final int[][] MARGINS = {{0, 23}, {0, 9}, {5, 23}, {5, 15}, {11, 11}};
    // fillLines = window growth (lrdMax), backRows = view scrolled back (lrd = lrdMax - backRows).
    private static final int[][] WINDOW_STATES = {
        {Terminal.HEIGHT, 0}, // fresh, glued
        {60, 0}, // mid scrollback, glued
        {60, 15}, // mid scrollback, view parked 15 rows back
        {478, 0}, // one growth row below capacity: the boundary class
        {478, 2}, // boundary, view slightly back
        {CAPACITY, 0}, // at absolute capacity, glued
        {CAPACITY, 20}, // at capacity, view scrolled back
    };
    private static final int[] COUNTS = {1, 2, 3, 5, 24, 25, 30};

    @Test
    void shiftUpBatchedMatchesPerRowScrolling() {
        for (final int[] margins : MARGINS) {
            for (final int[] windowState : WINDOW_STATES) {
                for (final int count : COUNTS) {
                    assertShiftUpMatchesPerRow(margins, windowState, count);
                }
            }
        }
    }

    private void assertShiftUpMatchesPerRow(
            final int[] margins, final int[] windowState, final int count) {
        final Terminal batched = scenario(windowState[0], windowState[1], margins[0], margins[1]);
        final Terminal looped = scenario(windowState[0], windowState[1], margins[0], margins[1]);

        // Corrected per-row loop: with full margins each row slides the window while the view
        // has room, else physically shifts; partial margins never touch the window.
        final boolean fullMargins =
                looped.scrollFirst == 0 && looped.scrollLast == looped.height - 1;
        for (int i = 0; i < count; i++) {
            if (fullMargins && looped.lastRowToDisplay < CAPACITY) {
                looped.bufferManager.incrementLastLineToDisplay();
            } else {
                looped.bufferManager.shiftUpOne();
            }
        }
        batched.bufferManager.shiftUp(count);

        assertSameState(batched, looped, margins, windowState, count, "shiftUp");
    }

    @Test
    void shiftDownBatchedMatchesPerRowScrolling() {
        for (final int[] margins : MARGINS) {
            for (final int[] windowState : WINDOW_STATES) {
                for (final int count : COUNTS) {
                    final Terminal batched =
                            scenario(windowState[0], windowState[1], margins[0], margins[1]);
                    final Terminal looped =
                            scenario(windowState[0], windowState[1], margins[0], margins[1]);

                    for (int i = 0; i < count; i++) {
                        looped.bufferManager.shiftDownOne();
                    }
                    batched.bufferManager.shiftDown(count);

                    assertSameState(batched, looped, margins, windowState, count, "shiftDown");
                }
            }
        }
    }

    @Test
    void shiftUpCrossingCapacityBoundaryScrollsExactlyNRows() {
        // CSI 2S from a glued view one growth-row below capacity: exactly 2 scrolls. The old
        // loop did a slide AND a physical shift on the crossing iteration, which discarded the
        // oldest scrollback line (buffer row 0) one row early — n+1 scrolls for n requested.
        final Terminal terminal = scenario(CAPACITY - 2, 0, 0, Terminal.HEIGHT - 1);
        assertEquals('A', (char) terminal.buffer[0], "precondition: row 0 marker in place");

        write(terminal, CSI + "2S");

        assertEquals(CAPACITY, terminal.lastRowToDisplayMax, "window grew by 2, reaching capacity");
        assertEquals(CAPACITY, terminal.lastRowToDisplay, "view glued to the window bottom");
        assertEquals('A', (char) terminal.buffer[0], "no physical shift: oldest scrollback row intact");
        // The screen scrolled exactly 2: screen row 0 now shows the buffer row that sat two
        // below the old view top.
        final int viewTop = terminal.lastRowToDisplay - terminal.height;
        assertEquals((char) ('A' + viewTop % 26), (char) terminal.buffer[viewTop * terminal.width],
                "screen row 0 shows the row two below the old view top");
    }

    @Test
    void shiftUpAtCapacityDiscardsOldestScrollback() {
        final Terminal terminal = scenario(CAPACITY, 0, 0, Terminal.HEIGHT - 1);
        write(terminal, CSI + "3S");
        // Three oldest rows discarded off the top; three bottom rows blank.
        assertEquals((char) ('A' + 3), (char) terminal.buffer[0], "row 0 now holds old row 3");
        for (int r = CAPACITY - 3; r < CAPACITY; r++) {
            for (int x = 0; x < terminal.width; x++) {
                assertEquals(' ', (char) terminal.buffer[x + r * terminal.width],
                        "bottom rows blank after scrolling past capacity");
            }
        }
    }

    @Test
    void shiftDownKeepsUnusedTailBlank() {
        // SD never writes below the write window: with scrollback room remaining, rows past
        // lrdMax are future scrollback and must stay blank (content pushed off the region
        // bottom is discarded, not parked in the tail).
        final Terminal terminal = scenario(100, 0, 0, Terminal.HEIGHT - 1);
        write(terminal, CSI + "3T");
        for (int r = 100; r < 110; r++) {
            assertEquals(' ', (char) terminal.buffer[r * terminal.width],
                    "tail row " + r + " must stay blank after SD");
        }
    }

    @Test
    void multiRowScrollStaysInsidePartialMargins() {
        // Region rows 5..15; a 3-row scroll must not touch rows outside it.
        final Terminal terminal = scenario(60, 0, 5, 15);
        final int off = terminal.lastRowToDisplayMax - terminal.height;
        final int[] outsideTop = new int[terminal.width];
        final int[] outsideBottom = new int[terminal.width];
        for (int x = 0; x < terminal.width; x++) {
            outsideTop[x] = terminal.buffer[x + (4 + off) * terminal.width];
            outsideBottom[x] = terminal.buffer[x + (16 + off) * terminal.width];
        }

        write(terminal, CSI + "3S");
        for (int x = 0; x < terminal.width; x++) {
            assertEquals(outsideTop[x], terminal.buffer[x + (4 + off) * terminal.width],
                    "row above the region untouched by SU 3");
            assertEquals(outsideBottom[x], terminal.buffer[x + (16 + off) * terminal.width],
                    "row below the region untouched by SU 3");
        }

        write(terminal, CSI + "3T");
        for (int x = 0; x < terminal.width; x++) {
            assertEquals(outsideTop[x], terminal.buffer[x + (4 + off) * terminal.width],
                    "row above the region untouched by SD 3");
            assertEquals(outsideBottom[x], terminal.buffer[x + (16 + off) * terminal.width],
                    "row below the region untouched by SD 3");
        }
    }

    @Test
    void hugeDirectCountDoesNotThrow() {
        // The raw n-ary API is total: any count degrades to "discard what scrolled off",
        // never an out-of-bounds access (the pre-batching count>1 algebra threw AIOOBE at
        // capacity — the landmine this rework removes).
        final Terminal atCapacity = scenario(CAPACITY, 0, 0, Terminal.HEIGHT - 1);
        assertDoesNotThrow(() -> atCapacity.bufferManager.shiftUp(100_000));
        final int viewTop = atCapacity.lastRowToDisplay - Terminal.HEIGHT;
        for (int y = 0; y < Terminal.HEIGHT; y++) {
            for (int x = 0; x < atCapacity.width; x++) {
                assertEquals(' ', (char) atCapacity.buffer[x + (viewTop + y) * atCapacity.width],
                        "screen fully blank after SU beyond buffer size");
            }
        }
        final Terminal fresh = scenario(Terminal.HEIGHT, 0, 0, Terminal.HEIGHT - 1);
        assertDoesNotThrow(() -> fresh.bufferManager.shiftDown(100_000));
        assertDoesNotThrow(() -> fresh.bufferManager.shiftUp(0));
        assertDoesNotThrow(() -> fresh.bufferManager.shiftUp(-5));
    }

    @Test
    void altBufferBatchedMatchesPerRowScrolling() {
        // Alt buffer: no growth phase — the batched form must equal n pure per-row shifts.
        // Pins the guard ordering (alt checked before any window interaction).
        for (final int[] margins : MARGINS) {
            for (final int count : COUNTS) {
                assertAltShiftMatches(margins, count, true);
                assertAltShiftMatches(margins, count, false);
            }
        }
    }

    private void assertAltShiftMatches(
            final int[] margins, final int count, final boolean up) {
        final Terminal batched = altScenario(margins[0], margins[1]);
        final Terminal looped = altScenario(margins[0], margins[1]);

        for (int i = 0; i < count; i++) {
            if (up) {
                looped.bufferManager.shiftUpOne();
            } else {
                looped.bufferManager.shiftDownOne();
            }
        }
        if (up) {
            batched.bufferManager.shiftUp(count);
        } else {
            batched.bufferManager.shiftDown(count);
        }

        assertAltSameState(batched, looped, margins, count, up ? "shiftUp" : "shiftDown");
    }

    private Terminal altScenario(final int scrollFirst, final int scrollLast) {
        final Terminal terminal = scenario(Terminal.HEIGHT, 0, scrollFirst, scrollLast);
        write(terminal, CSI + "?47h"); // switch to alt buffer (clears it; home)
        for (int r = 0; r < terminal.height; r++) {
            final char ch = (char) ('A' + r % 26);
            Arrays.fill(terminal.altBuffer, r * terminal.width, (r + 1) * terminal.width, ch);
            Arrays.fill(terminal.altStyles, r * terminal.width, (r + 1) * terminal.width, (byte) (r % 7));
        }
        return terminal;
    }

    private void assertAltSameState(
            final Terminal batched, final Terminal looped, final int[] margins, final int count,
            final String direction) {
        final String label = direction + " alt m=" + margins[0] + ".." + margins[1] + " n=" + count;
        assertEquals(looped.x, batched.x, "cursor x: " + label);
        assertEquals(looped.y, batched.y, "cursor y: " + label);
        assertTrue(Arrays.equals(looped.altBuffer, batched.altBuffer), "alt chars: " + label);
        assertTrue(Arrays.equals(looped.altStyles, batched.altStyles), "alt styles: " + label);
    }

    private Terminal scenario(
            final int windowRows, final int backRows, final int scrollFirst, final int scrollLast) {
        final Terminal terminal = new Terminal();
        for (int i = Terminal.HEIGHT; i < windowRows; i++) {
            terminal.bufferManager.incrementLastLineToDisplay();
        }
        for (int i = 0; i < backRows; i++) {
            terminal.bufferManager.decrementLastLineToDisplay();
        }
        if (scrollFirst == scrollLast) {
            // A 1-row region is unreachable through DECSTBM (xterm-410 charproc.c:4779 also
            // requires bot > top) — set the fields directly: this column covers the shifter's
            // degenerate-span totality guards, not a reachable CSI state.
            terminal.scrollFirst = scrollFirst;
            terminal.scrollLast = scrollLast;
        } else if (scrollFirst != 0 || scrollLast != terminal.height - 1) {
            write(terminal, ESC + "[" + (scrollFirst + 1) + ";" + (scrollLast + 1) + "r");
        }
        // Row-identifying content across the written span so any row movement or array desync
        // is observable. Rows past windowRows are the never-written future-scrollback tail and
        // stay default-blank. Chars cycle A..Z by row; styles and colors follow.
        for (int r = 0; r < windowRows; r++) {
            final char ch = (char) ('A' + r % 26);
            Arrays.fill(terminal.buffer, r * terminal.width, (r + 1) * terminal.width, ch);
            Arrays.fill(terminal.styles, r * terminal.width, (r + 1) * terminal.width, (byte) (r % 7));
            for (int x = 0; x < terminal.width; x++) {
                terminal.colors[x + r * terminal.width] =
                        new TerminalColors.ColorData(ch, ch, ch, TerminalColors.ColorMode.TRUE_COLOR);
                terminal.colorsBackground[x + r * terminal.width] =
                        new TerminalColors.ColorData(1, 2, r % 256, TerminalColors.ColorMode.TRUE_COLOR);
            }
        }
        return terminal;
    }

    private void assertSameState(
            final Terminal batched,
            final Terminal looped,
            final int[] margins,
            final int[] windowState,
            final int count,
            final String direction) {
        final String label = direction + " m=" + margins[0] + ".." + margins[1]
                + " fill=" + windowState[0] + " back=" + windowState[1] + " n=" + count;
        assertEquals(looped.lastRowToDisplay, batched.lastRowToDisplay, "lrd: " + label);
        assertEquals(looped.lastRowToDisplayMax, batched.lastRowToDisplayMax, "lrdMax: " + label);
        assertEquals(looped.x, batched.x, "cursor x: " + label);
        assertEquals(looped.y, batched.y, "cursor y: " + label);
        assertTrue(Arrays.equals(looped.buffer, batched.buffer), "chars: " + label);
        assertTrue(Arrays.equals(looped.styles, batched.styles), "styles: " + label);
        assertColorsEqual(looped.colors, batched.colors, "fg colors: " + label);
        assertColorsEqual(looped.colorsBackground, batched.colorsBackground, "bg colors: " + label);
    }

    private static void assertColorsEqual(
            final TerminalColors.ColorData[] expected,
            final TerminalColors.ColorData[] actual,
            final String label) {
        for (int i = 0; i < expected.length; i++) {
            if (expected[i].toInt() != actual[i].toInt()
                    || expected[i].mode != actual[i].mode) {
                throw new AssertionError(label + " mismatch at cell " + i
                        + ": expected " + expected[i].toInt() + "/" + expected[i].mode
                        + " got " + actual[i].toInt() + "/" + actual[i].mode);
            }
        }
    }

    private void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }
}
