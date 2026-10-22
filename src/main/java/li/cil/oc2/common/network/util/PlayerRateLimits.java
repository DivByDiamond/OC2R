package li.cil.oc2.common.network.util;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-player rate limits for client-to-server messages. Keys are weakly referenced so
 * disconnected players are collected; state is only ever an approximation, erring on
 * the side of dropping a legitimate message rather than letting spam through.
 */
public final class PlayerRateLimits {
    private static final long THROTTLE_WINDOW_MS = 1000;
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final WeakHashMap<Object, Long> LAST_ALLOWED_AT = new WeakHashMap<>();
    private static final WeakHashMap<Object, WeakHashMap<Object, Long>> DEVICE_LAST_ALLOWED_AT =
            new WeakHashMap<>();
    private static final WeakHashMap<Object, EventWindow> EVENT_WINDOWS = new WeakHashMap<>();

    private PlayerRateLimits() {}

    /**
     * Returns true at most once per {@code minIntervalMs} for the given key; further
     * calls within the interval are dropped.
     */
    public static boolean allowThrottled(final Object key, final long minIntervalMs) {
        LOCK.lock();
        try {
            final long now = System.currentTimeMillis();
            final Long last = LAST_ALLOWED_AT.get(key);
            if (last != null && now - last < minIntervalMs) {
                return false;
            }
            LAST_ALLOWED_AT.put(key, now);
            return true;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Limits requests independently for each player/device pair. Both keys are weak:
     * a disconnected player or an unloaded block entity must not be kept alive here.
     */
    public static boolean allowThrottled(
            final Object player, final Object device, final long minIntervalMs) {
        LOCK.lock();
        try {
            final long now = System.currentTimeMillis();
            final WeakHashMap<Object, Long> devices =
                    DEVICE_LAST_ALLOWED_AT.computeIfAbsent(player, ignored -> new WeakHashMap<>());
            final Long last = devices.get(device);
            if (last != null && now - last < minIntervalMs) {
                return false;
            }
            devices.put(device, now);
            return true;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Returns true while the given key stays within {@code maxPerSecond} events per
     * rolling one-second window; excess events are dropped.
     */
    public static boolean allowEvents(final Object key, final int maxPerSecond) {
        LOCK.lock();
        try {
            final long now = System.currentTimeMillis();
            final EventWindow window = EVENT_WINDOWS.get(key);
            if (window == null || now - window.startMs >= THROTTLE_WINDOW_MS) {
                final EventWindow fresh = new EventWindow(now);
                fresh.count = 1;
                EVENT_WINDOWS.put(key, fresh);
                return true;
            }
            return ++window.count <= maxPerSecond;
        } finally {
            LOCK.unlock();
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
