package li.cil.oc2.network.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WireGeometryTest {
    @Test
    void shortWiresKeepTheOriginalLook() {
        assertEquals(0.1, WireGeometry.hang(0), 1e-9);
        assertEquals(0.5, WireGeometry.hang(8), 1e-9);
    }

    @Test
    void hangGrowsWithLength() {
        double previous = WireGeometry.hang(0);
        for (int length = 1; length <= 16; length++) {
            final double hang = WireGeometry.hang(length);
            assertTrue(hang >= previous, "hang must not shrink at " + length);
            previous = hang;
        }
        // Eight blocks of ramp, then the fixed rate over the maximum link distance of 16 blocks.
        assertEquals(0.9, WireGeometry.hang(16), 1e-9);
    }

    @Test
    void segmentCountGrowsWithLength() {
        assertEquals(8, WireGeometry.segments(1));
        assertEquals(24, WireGeometry.segments(16));
        assertTrue(WireGeometry.segments(12) > WireGeometry.segments(6));
    }
}
