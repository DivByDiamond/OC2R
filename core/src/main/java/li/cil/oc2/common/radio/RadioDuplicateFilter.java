package li.cil.oc2.common.radio;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded, expiring memory of recently seen keys, used to drop duplicate deliveries, looped
 * broadcasts and repeated request IDs. Every entry lives for the same number of ticks, so the
 * insertion order is also the expiry order. When the capacity is reached the oldest entry is
 * forgotten first; a duplicate that arrives after its entry was evicted is no longer recognised,
 * which is the price of bounded state. Tick-thread only.
 */
@SuppressWarnings("PMD.UseConcurrentHashMap") // Tick thread only, see above.
public final class RadioDuplicateFilter<K> {
    private final int capacity;
    private final long lifetimeTicks;
    private final Map<K, Long> seen = new LinkedHashMap<>();

    public RadioDuplicateFilter(final int capacity, final long lifetimeTicks) {
        if (capacity < 1 || lifetimeTicks < 1) throw new IllegalArgumentException("Invalid duplicate filter limits");
        this.capacity = capacity;
        this.lifetimeTicks = lifetimeTicks;
    }

    /**
     * Records the key and returns true when it is new, or false when it was already seen within the
     * lifetime. A duplicate does not extend the lifetime of the first sighting.
     */
    public boolean firstSeen(final K key, final long tick) {
        Objects.requireNonNull(key);
        expire(tick);
        if (seen.containsKey(key)) return false;
        if (seen.size() >= capacity) {
            final Iterator<K> oldest = seen.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        seen.put(key, tick > Long.MAX_VALUE - lifetimeTicks ? Long.MAX_VALUE : tick + lifetimeTicks);
        return true;
    }

    /** Whether the key is currently remembered, without recording it. */
    public boolean contains(final K key, final long tick) {
        expire(tick);
        return seen.containsKey(key);
    }

    public void clear() {
        seen.clear();
    }

    public int size() {
        return seen.size();
    }

    private void expire(final long tick) {
        final Iterator<Long> expiries = seen.values().iterator();
        while (expiries.hasNext() && expiries.next() <= tick) expiries.remove();
    }
}
