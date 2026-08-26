package li.cil.oc2.common.network.util;

import java.util.WeakHashMap;

/**
 * Per-player rate limits for client-to-server messages. Keys are weakly referenced so
 * disconnected players are collected; state is only ever an approximation, erring on
 * the side of dropping a legitimate message rather than letting spam through.
 */
public final class PlayerRateLimits {
    private static final long THROTTLE_WINDOW_MS = 1000;
    private static final Object LOCK = new Object();
    private static final WeakHashMap<Object, Long> LAST_ALLOWED_AT = new WeakHashMap<>();
    private static final WeakHashMap<Object, EventWindow> EVENT_WINDOWS = new WeakHashMap<>();

    private PlayerRateLimits() {}

    /**
     * Returns true at most once per {@code minIntervalMs} for the given key; further
     * calls within the interval are dropped.
     */
    public static boolean allowThrottled(final Object key, final long minIntervalMs) {
        synchronized (LOCK) {
            final long now = System.currentTimeMillis();
            final Long last = LAST_ALLOWED_AT.get(key);
            if (last != null && now - last < minIntervalMs) {
                return false;
            }
            LAST_ALLOWED_AT.put(key, now);
            return true;
        }
    }

    /**
     * Returns true while the given key stays within {@code maxPerSecond} events per
     * rolling one-second window; excess events are dropped.
     */
    public static boolean allowEvents(final Object key, final int maxPerSecond) {
        synchronized (LOCK) {
            final long now = System.currentTimeMillis();
            final EventWindow window = EVENT_WINDOWS.get(key);
            if (window == null || now - window.startMs >= THROTTLE_WINDOW_MS) {
                final EventWindow fresh = new EventWindow(now);
                fresh.count = 1;
                EVENT_WINDOWS.put(key, fresh);
                return true;
            }
            return ++window.count <= maxPerSecond;
        }
    }

    private static final class EventWindow {
        final long startMs;
        int count;

        EventWindow(final long startMs) {
            this.startMs = startMs;
        }
    }
}
