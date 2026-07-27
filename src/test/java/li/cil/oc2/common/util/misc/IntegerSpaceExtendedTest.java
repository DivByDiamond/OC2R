package li.cil.oc2.common.util.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntegerSpaceExtendedTest {
    @Test
    public void emptySpaceContainsNothing() {
        final IntegerSpace space = new IntegerSpace();
        assertEquals(0, space.count());
        assertFalse(space.contains(0));
        assertFalse(space.contains(1));
        assertFalse(space.contains(100));
    }

    @Test
    public void duplicatePutReturnsFalse() {
        final IntegerSpace space = new IntegerSpace();
        assertTrue(space.put(42));
        assertFalse(space.put(42));
        assertEquals(1, space.count());
    }

    @Test
    public void contiguousRangeMerges() {
        final IntegerSpace space = new IntegerSpace();
        space.put(1);
        space.put(2);
        space.put(3);
        assertEquals(1, space.rangeCount());
        assertEquals(3, space.count());
        assertEquals("[1-3]", space.toString());
    }

    @Test
    public void gapPreventsMerge() {
        final IntegerSpace space = new IntegerSpace();
        space.put(1);
        space.put(3);
        assertEquals(2, space.rangeCount());
        assertFalse(space.contains(2));
    }

    @Test
    public void rangePut() {
        final IntegerSpace space = new IntegerSpace();
        assertTrue(space.put(10, 20));
        assertEquals(11, space.count());
        assertEquals(1, space.rangeCount());
        for (int i = 10; i <= 20; i++) {
            assertTrue(space.contains(i));
        }
        assertFalse(space.contains(9));
        assertFalse(space.contains(21));
    }

    @Test
    public void overlappingRangePut() {
        final IntegerSpace space = new IntegerSpace();
        space.put(10, 15);
        assertTrue(space.put(13, 20));
        assertEquals(11, space.count());
        assertEquals("[10-20]", space.toString());
    }

    @Test
    public void multipleGapsThenFill() {
        final IntegerSpace space = new IntegerSpace();
        space.put(1);
        space.put(3);
        space.put(5);
        assertEquals(3, space.rangeCount());

        space.put(2);
        space.put(4);
        assertEquals(1, space.rangeCount());
        assertEquals("[1-5]", space.toString());
    }

    @Test
    public void toStringFormat() {
        final IntegerSpace space = new IntegerSpace();
        space.put(1);
        space.put(3, 5);
        space.put(10);
        assertEquals("[1, 3-5, 10]", space.toString());
    }
}
