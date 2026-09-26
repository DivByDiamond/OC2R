package li.cil.oc2.common.blockentity.network.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FaceOverrideTest {
    @Test
    void roundTripsThroughSavedByte() {
        for (final FaceOverride value : FaceOverride.values()) {
            assertEquals(value, FaceOverride.fromByte(value.toByte()));
        }
    }

    @Test
    void unknownSavedValuesFallBackToAuto() {
        assertEquals(FaceOverride.AUTO, FaceOverride.fromByte((byte) -1));
        assertEquals(FaceOverride.AUTO, FaceOverride.fromByte((byte) 99));
    }

    @Test
    void onlyForcedOffBlocksConnections() {
        assertFalse(FaceOverride.AUTO.blocksConnection());
        assertFalse(FaceOverride.FORCED_ON.blocksConnection());
        assertTrue(FaceOverride.FORCED_OFF.blocksConnection());
    }
}
