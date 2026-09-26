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
    void hangGrowsWithLengthAndIsCapped() {
        double previous = WireGeometry.hang(0);
        for (double length = 1; length <= 100; length += 1) {
            final double hang = WireGeometry.hang(length);
            assertTrue(hang >= previous, "hang must not shrink at " + length);
            previous = hang;
        }
        assertEquals(3.0, WireGeometry.hang(1000), 1e-9);
    }

    @Test
    void segmentCountIsBounded() {
        assertEquals(8, WireGeometry.segments(1));
        assertEquals(48, WireGeometry.segments(500));
        assertTrue(WireGeometry.segments(20) > WireGeometry.segments(6));
    }

    @Test
    void opticalIsThinnestGoldThickest() {
        assertTrue(WireGeometry.thickness(WireType.OPTICAL) < WireGeometry.thickness(WireType.COPPER));
        assertTrue(WireGeometry.thickness(WireType.GOLD) > WireGeometry.thickness(WireType.COPPER));
    }
}
