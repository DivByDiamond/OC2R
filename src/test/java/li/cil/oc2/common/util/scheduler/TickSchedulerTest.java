package li.cil.oc2.common.util.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TickSchedulerTest {
    @Test
    void tasksDueOnTheSameTickRunInSchedulingOrder() {
        final TickScheduler scheduler = new TickScheduler();
        final List<Integer> ran = new ArrayList<>();
        // Enough tasks that an unordered heap would reorder some of them.
        for (int i = 0; i < 64; i++) {
            final int index = i;
            scheduler.schedule(() -> ran.add(index), 2 - (i % 2));
        }
        scheduler.tick();
        scheduler.processQueue();
        scheduler.tick();
        scheduler.processQueue();
        final List<Integer> expected = new ArrayList<>();
        for (int i = 1; i < 64; i += 2) expected.add(i);
        for (int i = 0; i < 64; i += 2) expected.add(i);
        assertEquals(expected, ran);
    }

    @Test
    void tasksDoNotRunBeforeTheirTick() {
        final TickScheduler scheduler = new TickScheduler();
        final List<String> ran = new ArrayList<>();
        scheduler.schedule(() -> ran.add("later"), 3);
        scheduler.tick();
        scheduler.processQueue();
        assertTrue(ran.isEmpty());
        scheduler.tick();
        scheduler.tick();
        scheduler.processQueue();
        assertEquals(List.of("later"), ran);
    }
}
