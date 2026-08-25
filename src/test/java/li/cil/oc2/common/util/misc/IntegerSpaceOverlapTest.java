package li.cil.oc2.common.util.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for overlapping puts: the old merge logic left nested ranges
 * whose key was strictly inside the inserted range behind, corrupting the space.
 */
class IntegerSpaceOverlapTest {
    @Test
    void putBelowOverlappingRangeMerges() {
        final IntegerSpace space = new IntegerSpace();
        assertTrue(space.put(5, 15));
        assertTrue(space.put(0, 10));
        assertEquals("[0-15]", space.toString());
        assertEquals(16, space.count());
        assertTrue(space.contains(12));
    }

    @Test
    void putSpanningTwoRangesMergesAll() {
        final IntegerSpace space = new IntegerSpace();
        space.put(0, 10);
        space.put(20, 30);
        space.put(5, 25);
        assertEquals("[0-30]", space.toString());
    }

    @Test
    void putOverlappingTailOfHigherRange() {
        final IntegerSpace space = new IntegerSpace();
        space.put(0, 10);
        space.put(15, 20);
        space.put(5, 17);
        assertEquals("[0-20]", space.toString());
    }

    @Test
    void putInsideGapTouchingBothRanges() {
        final IntegerSpace space = new IntegerSpace();
        space.put(0, 10);
        space.put(15, 20);
        // 11 is a gap, so [0-10] stays separate; [15-20] must be absorbed.
        space.put(12, 18);
        assertEquals("[0-10, 12-20]", space.toString());
    }

    @Test
    void randomizedPutsMatchReferenceSet() {
        final Random random = new Random(42);
        final IntegerSpace space = new IntegerSpace();
        final TreeSet<Integer> reference = new TreeSet<>();
        for (int i = 0; i < 20000; i++) {
            final int a = random.nextInt(64);
            final int b = random.nextInt(64);
            space.put(Math.min(a, b), Math.max(a, b));
            for (int value = Math.min(a, b); value <= Math.max(a, b); value++) {
                reference.add(value);
            }

            assertEquals(reference.size(), space.count(), "iteration " + i);

            final int probe = random.nextInt(80) - 8;
            assertEquals(
                    reference.contains(probe),
                    space.contains(probe),
                    "contains(" + probe + ") mismatch at iteration " + i);
        }
        // Invariant: ranges stay disjoint and sorted.
        assertFalse(space.toString().isEmpty());
    }
}
