package li.cil.oc2.common.util.scheduler;

record ScheduledRunnable(int tick, Runnable runnable) implements Comparable<ScheduledRunnable> {
    @Override
    public int compareTo(final ScheduledRunnable o) {
        return Integer.compare(tick, o.tick);
    }
}
