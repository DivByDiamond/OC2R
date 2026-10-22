package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.*;

import li.cil.oc2.manual.client.util.ManualGraphics;
import org.junit.jupiter.api.Test;

class ManualAnimationTest {
    @Test
    void slowFramesCannotOvershootTheScrollTargetOrTabPosition() {
        float scroll = -27;
        for (int frame = 0; frame < 20; frame++) {
            scroll = ManualGraphics.approach(scroll, 0, 10);
            assertTrue(scroll >= -27 && scroll <= 0, "scroll escaped document: " + scroll);
        }
        assertEquals(0, scroll);
        assertEquals(20, ManualGraphics.approach(4, 20, 10));
    }

    @Test
    void ordinaryFramesStillAnimateTowardTheTarget() {
        final float first = ManualGraphics.approach(-27, 0, 0.25f);
        final float second = ManualGraphics.approach(first, 0, 0.25f);
        assertTrue(-27 < first && first < second && second < 0);
    }
}
