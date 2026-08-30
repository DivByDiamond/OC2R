package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Isolated unit tests for {@link SGRColorParser}.
 *
 * <p>Calls {@link SGRColorParser#parse(int[], int, int)} directly — no {@code Terminal}, no parser
 * state machine. Pins down the refactored parser's contract: valid 256-color and true-color
 * parsing, the {@code Math.clamp} bounds on indices/components, and the malformed/short-arg cases
 * that the orchestrator relies on to decide how many args to skip.
 */
public class SGRColorParserTest {

    @Test
    void parses256Color() {
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {5, 196}, 0, 2);
        assertTrue(result.isValid());
        assertEquals(ColorMode.TWO_FIFTY_SIX_COLOR, result.mode());
        assertEquals(196, result.color().r);
        assertEquals(2, result.consumed());
    }

    @Test
    void parsesTrueColor() {
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {2, 10, 20, 30}, 0, 4);
        assertTrue(result.isValid());
        assertEquals(ColorMode.TRUE_COLOR, result.mode());
        assertEquals(10, result.color().r);
        assertEquals(20, result.color().g);
        assertEquals(30, result.color().b);
        assertEquals(4, result.consumed());
    }

    @Test
    void clamps256ColorIndexTo255() {
        // Out-of-range index must clamp, not produce an out-of-bounds palette lookup.
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {5, 300}, 0, 2);
        assertTrue(result.isValid());
        assertEquals(255, result.color().r);
    }

    @Test
    void clampsTrueColorComponentsToRange() {
        // Each component clamps independently to [0, 255]; negatives → 0, over-range → 255.
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {2, 300, -1, 999}, 0, 4);
        assertTrue(result.isValid());
        assertEquals(255, result.color().r);
        assertEquals(0, result.color().g);
        assertEquals(255, result.color().b);
    }

    @Test
    void rejects256ColorWithMissingIndex() {
        // 5; with no following index → invalid (not silently treated as index 0).
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {5}, 0, 1);
        assertFalse(result.isValid());
        assertEquals(0, result.consumed());
    }

    @Test
    void rejectsTrueColorWithMissingComponents() {
        // 2;R;G with no B → invalid.
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {2, 1, 2}, 0, 3);
        assertFalse(result.isValid());
    }

    @Test
    void rejectsUnrecognizedModeByte() {
        // 7 is not a color-mode selector → invalid. (The orchestrator then skips the 38;7 pair so
        // 7 is not re-applied as SGR 7 — see SGRTest#sgrMalformedExtendedColorSkipsModeByte.)
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {7}, 0, 1);
        assertFalse(result.isValid());
    }

    @Test
    void rejectsEmptyWhenOffsetAtLimit() {
        // No sub-args at all (e.g. a bare trailing 38) → invalid.
        final SGRColorParser.SGRColorResult result = SGRColorParser.parse(new int[] {38}, 1, 1);
        assertFalse(result.isValid());
    }
}
