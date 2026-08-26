package li.cil.oc2.common.network.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerRateLimitsTest {
    @Test
    void throttledAllowsOncePerInterval() throws Exception {
        final Object key = new Object();
        assertTrue(PlayerRateLimits.allowThrottled(key, 100));
        assertFalse(PlayerRateLimits.allowThrottled(key, 100));
        Thread.sleep(120);
        assertTrue(PlayerRateLimits.allowThrottled(key, 100));
    }

    @Test
    void throttledKeysAreIndependent() {
        final Object first = new Object();
        final Object second = new Object();
        assertTrue(PlayerRateLimits.allowThrottled(first, 10_000));
        assertTrue(PlayerRateLimits.allowThrottled(second, 10_000));
        assertFalse(PlayerRateLimits.allowThrottled(first, 10_000));
    }

    @Test
    void eventWindowDropsExcessAndResets() throws Exception {
        final Object key = new Object();
        assertTrue(PlayerRateLimits.allowEvents(key, 2));
        assertTrue(PlayerRateLimits.allowEvents(key, 2));
        assertFalse(PlayerRateLimits.allowEvents(key, 2), "third event within the window must be dropped");
        Thread.sleep(1100);
        assertTrue(PlayerRateLimits.allowEvents(key, 2), "a fresh window must allow events again");
    }

    @Test
    void eventWindowsAreIndependent() {
        final Object first = new Object();
        final Object second = new Object();
        assertTrue(PlayerRateLimits.allowEvents(first, 1));
        assertFalse(PlayerRateLimits.allowEvents(first, 1));
        assertTrue(PlayerRateLimits.allowEvents(second, 1));
    }
}
