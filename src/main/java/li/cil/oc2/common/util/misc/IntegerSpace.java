package li.cil.oc2.common.util.misc;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A set of integers that is more effective with ranges of integers. */
public class IntegerSpace {
    private final NavigableMap<Integer, Integer> ranges = new TreeMap<>();

    public final boolean put(final int element) {
        return put(element, element);
    }

    public final boolean put(final int begin, final int end) {
        if (end < begin) {
            return put(end, begin);
        }

        final Map.Entry<Integer, Integer> floor = ranges.floorEntry(begin);
        if (floor != null && floor.getKey() <= begin && floor.getValue() >= end) {
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
        if (floor != null && (long) floor.getValue() + 1 >= begin) {
            mergedBegin = floor.getKey();
            mergedEnd = Math.max(mergedEnd, floor.getValue());
            ranges.remove(floor.getKey());
        }

        // Absorb all following ranges that touch or overlap the merged one.
        final Iterator<Map.Entry<Integer, Integer>> iterator =
                ranges.tailMap(mergedBegin, false).entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Integer, Integer> range = iterator.next();
            if ((long) range.getKey() - 1 > mergedEnd) {
                break;
            }
            mergedEnd = Math.max(mergedEnd, range.getValue());
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
                && element >= floorRange.getKey()
                && element <= floorRange.getValue();
    }

    public final boolean isEmpty() {
        return ranges.isEmpty();
    }

    public final int rangeCount() {
        return ranges.size();
    }

    public final int count() {
        return ranges.entrySet().stream()
                .map(range -> range.getValue() - range.getKey() + 1)
                .reduce(0, Integer::sum);
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