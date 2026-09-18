package li.cil.oc2.common.util.misc;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A set of integers that is more effective with ranges of integers. */
public class IntegerSpace {
    private final NavigableMap<Integer, Integer> ranges = new TreeMap<>(Integer::compareUnsigned);

    public final boolean put(final int element) {
        return put(element, element);
    }

    public final boolean put(final int begin, final int end) {
        if (Integer.compareUnsigned(end, begin) < 0) {
            return put(end, begin);
        }

        final Map.Entry<Integer, Integer> floor = ranges.floorEntry(begin);
        if (floor != null && Integer.compareUnsigned(floor.getKey(), begin) <= 0 && Integer.compareUnsigned(floor.getValue(), end) >= 0) {
            // Already exists in the space
            // [---------]
            // [---------]
            //   [---]
            // [---------]
            return false;
        }

        int mergedBegin = begin;
        int mergedEnd = end;

        // Absorb the range below begin if it touches or overlaps the new one,
        // including ranges that start below and extend past end (the old strict
        // comparisons left such overlapping ranges behind forever).
        // Guard against unsigned wrap: 0xFFFFFFFF + 1 wraps to 0, must not merge
        // [255.255.255.255] with [0.0.0.0] - IP space is not a ring.
        if (floor != null
                && floor.getValue() != -1
                && Integer.toUnsignedLong(floor.getValue()) + 1 >= Integer.toUnsignedLong(begin)) {
            mergedBegin = floor.getKey();
            mergedEnd = Integer.compareUnsigned(mergedEnd, floor.getValue()) >= 0 ? mergedEnd : floor.getValue();
            ranges.remove(floor.getKey());
        }

        // Absorb all following ranges that touch or overlap the merged one.
        final Iterator<Map.Entry<Integer, Integer>> iterator =
                ranges.tailMap(mergedBegin, false).entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Integer, Integer> range = iterator.next();
            if (range.getKey() == 0) {
                // 0 cannot be adjacent from below via -1 (wrap from 0xFFFFFFFF),
                // so treat 0 as start of space - check containment only
                if (Integer.compareUnsigned(range.getKey(), mergedEnd) > 0) {
                    break;
                }
            } else if (Integer.toUnsignedLong(range.getKey()) - 1 > Integer.toUnsignedLong(mergedEnd)) {
                break;
            }
            mergedEnd = Integer.compareUnsigned(mergedEnd, range.getValue()) >= 0 ? mergedEnd : range.getValue();
            iterator.remove();
        }

        // New range in empty space, extended or merged with existing ones
        //          [---------]
        // [-----]      [------]
        ranges.put(mergedBegin, mergedEnd);
        return true;
    }

    public final boolean contains(final int element) {
        final Map.Entry<Integer, Integer> floorRange = ranges.floorEntry(element);
        return floorRange != null
                && Integer.compareUnsigned(element, floorRange.getKey()) >= 0
                && Integer.compareUnsigned(element, floorRange.getValue()) <= 0;
    }

    public final boolean isEmpty() {
        return ranges.isEmpty();
    }

    public final int rangeCount() {
        return ranges.size();
    }

    public final long countLong() {
        return ranges.entrySet().stream()
                .mapToLong(range -> Integer.toUnsignedLong(range.getValue()) - Integer.toUnsignedLong(range.getKey()) + 1)
                .sum();
    }

    public final int count() {
        final long c = countLong();
        return c > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c;
    }

    protected void elementToString(final StringBuilder builder, final int element) {
        builder.append(element);
    }

    private void appendRangeToString(
            final StringBuilder builder, final Map.Entry<Integer, Integer> range) {
        final int begin = range.getKey();
        final int end = range.getValue();
        elementToString(builder, begin);
        if (begin != end) {
            builder.append('-');
            elementToString(builder, range.getValue());
        }
    }

    @Override
    public String toString() {
        final Iterator<Map.Entry<Integer, Integer>> iterator = ranges.entrySet().iterator();
        if (iterator.hasNext()) {
            final StringBuilder builder = new StringBuilder();
            builder.append('[');
            final Map.Entry<Integer, Integer> first = iterator.next();
            appendRangeToString(builder, first);
            while (iterator.hasNext()) {
                builder.append(", ");
                final Map.Entry<Integer, Integer> range = iterator.next();
                appendRangeToString(builder, range);
            }
            builder.append(']');
            return builder.toString();
        } else {
            return "[]";
        }
    }
}