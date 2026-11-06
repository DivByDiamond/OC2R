package li.cil.oc2.common.radio;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Suppresses looped broadcasts. The radio floods a frame to every tower, so a wired loop between
 * two access points would otherwise make a frame circulate until its hop TTL runs out. Every hop,
 * wired or radio, lowers the TTL, so a frame that comes back is byte-identical to one seen shortly
 * before but carries a strictly lower TTL. Such a frame is dropped. A byte-identical frame with the
 * same or a higher TTL is a deliberate resend and passes. State is bounded in size and lifetime;
 * when it is full the oldest entry is forgotten first. Tick-thread only.
 */
@SuppressWarnings("PMD.UseConcurrentHashMap") // Tick thread only, see above.
public final class RadioFrameFilter {
    /** Long enough to span a delivery round trip (the delivery cap is 2 ticks), short enough for resends. */
    public static final long WINDOW_TICKS = 5;
    public static final int CAPACITY = 4096;

    private record Key(UUID network, ByteBuffer digest) {}

    private record Entry(long expiry, int ttl) {}

    private final Map<Key, Entry> seen = new LinkedHashMap<>();

    /** True when the frame is not a looped copy of a frame that entered this network's radio recently. */
    public boolean admit(final UUID network, final byte[] frame, final int ttl, final long tick) {
        final Iterator<Entry> entries = seen.values().iterator();
        while (entries.hasNext() && entries.next().expiry() <= tick) entries.remove();
        final Key key = new Key(network, digest(frame));
        final Entry previous = seen.get(key);
        if (previous != null && ttl < previous.ttl()) return false;
        if (previous == null && seen.size() >= CAPACITY) {
            final Iterator<Key> oldest = seen.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        // A resend refreshes the entry; removing first keeps insertion order equal to expiry order.
        seen.remove(key);
        seen.put(key, new Entry(tick + WINDOW_TICKS, previous == null ? ttl : Math.max(ttl, previous.ttl())));
        return true;
    }

    public int size() {
        return seen.size();
    }

    private static ByteBuffer digest(final byte[] frame) {
        try {
            return ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest(frame));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
