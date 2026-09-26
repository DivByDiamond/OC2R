package li.cil.oc2.network.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WireTypeTest {
    @Test
    void idsRoundTrip() {
        for (final WireType type : WireType.values()) {
            assertEquals(type, WireType.fromId(type.id()).orElseThrow());
        }
    }

    @Test
    void unknownOrMissingIdIsCopper() {
        assertEquals(WireType.COPPER, WireType.fromIdOrDefault(""));
        assertEquals(WireType.COPPER, WireType.fromIdOrDefault("bogus"));
        assertTrue(WireType.fromId("bogus").isEmpty());
    }

    @Test
    void higherTiersReachFurtherAndCarryMore() {
        assertTrue(WireType.GOLD.maxRange() > WireType.COPPER.maxRange());
        assertTrue(WireType.OPTICAL.maxRange() > WireType.GOLD.maxRange());
        assertTrue(WireType.OPTICAL.bandwidthFactor() > WireType.COPPER.bandwidthFactor());
    }

    @Test
    void configuredRangeCapsTheWire() {
        assertEquals(8, WireType.linkRange(WireType.OPTICAL, 8));
        assertEquals(16, WireType.linkRange(WireType.COPPER, 100));
        assertEquals(1, WireType.linkRange(WireType.COPPER, 0));
    }
}
