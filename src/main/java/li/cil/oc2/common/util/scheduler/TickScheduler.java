package li.cil.oc2.common.util.scheduler;

import java.util.PriorityQueue;
import java.util.Queue;

class TickScheduler {
    private final Queue<ScheduledRunnable> queue = new PriorityQueue<>();
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