package li.cil.oc2.common.tablet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TabletCheckpointTest {
    private static final int PERIOD = TabletManager.CHECKPOINT_TICKS;

    @Test
    void aFreshlySavedMachineIsNotWrittenAgainBeforeThePeriodEnds() {
        assertFalse(TabletManager.checkpointDue(1000, 1000, 0));
        assertFalse(TabletManager.checkpointDue(1000 + PERIOD - 1, 1000, 0));
    }

    @Test
    void aMachineIsWrittenOnceThePeriodHasPassed() {
        assertTrue(TabletManager.checkpointDue(1000 + PERIOD, 1000, 0));
        assertTrue(TabletManager.checkpointDue(1000 + PERIOD * 3L, 1000, 0));
    }

    @Test
    void aFailedWriteIsNotRetriedBeforeItsBackoffEnds() {
        final long now = 1000 + PERIOD;
        assertFalse(TabletManager.checkpointDue(now, 1000, now + 200));
        assertTrue(TabletManager.checkpointDue(now + 200, 1000, now + 200));
    }
}
