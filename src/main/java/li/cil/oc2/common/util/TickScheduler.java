package li.cil.oc2.common.util;

import java.util.PriorityQueue;

class TickScheduler {
    private final PriorityQueue<ScheduledRunnable> queue = new PriorityQueue<>();
    private int currentTick;

    public void schedule(final Runnable runnable, final int afterTicks) {
        queue.add(new ScheduledRunnable(currentTick + afterTicks, runnable));
    }

    public void processQueue() {
        while (!queue.isEmpty() && queue.peek().tick() <= currentTick) {
            queue.poll().runnable().run();
        }
    }

    public void tick() {
        currentTick++;
    }

    public void clear() {
        currentTick = 0;
        queue.clear();
    }
}