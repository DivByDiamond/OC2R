package li.cil.oc2.common.util.scheduler.runnable;

/**
 * A task due at a scheduler tick. Tasks due on the same tick run in the order they were
 * scheduled ({@code sequence}); the radio link relies on this to deliver TLS records in order.
 */
public record ScheduledRunnable(int tick, long sequence, Runnable runnable) implements Comparable<ScheduledRunnable> {
    @Override
    public int compareTo(final ScheduledRunnable o) {
        final int byTick = Integer.compare(tick, o.tick);
        return byTick != 0 ? byTick : Long.compare(sequence, o.sequence);
    }
}
