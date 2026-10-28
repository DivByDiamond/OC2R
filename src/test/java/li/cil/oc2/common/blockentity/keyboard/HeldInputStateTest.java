package li.cil.oc2.common.blockentity.keyboard;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.common.blockentity.keyboard.link.HeldInputState;
import org.junit.jupiter.api.Test;

class HeldInputStateTest {
    private record Event(int code, boolean down) {}

    @Test
    void focusOrCaptureExitReleasesModifiersMovementAndEveryMouseButtonExactlyOnce() {
        final HeldInputState state = new HeldInputState();
        final List<Event> events = new ArrayList<>();
        final java.util.function.BiConsumer<Integer, Boolean> output =
                (code, down) -> events.add(new Event(code, down));
        final int[] keys = {29, 42, 17, 0x110, 0x111, 0x112, 0x113, 0x114, 0x115, 0x116, 0x117};
        for (final int key : keys) {
            state.update(key, true, output);
            state.update(key, true, output);
        }
        state.release(output);
        state.release(output);
        assertEquals(keys.length * 2, events.size());
        for (final int key : keys) {
            assertEquals(1, events.stream().filter(event -> event.code() == key && event.down()).count());
            assertEquals(1, events.stream().filter(event -> event.code() == key && !event.down()).count());
        }
        events.clear();
        state.update(1, true, output);
        state.update(1, false, output);
        state.release(output);
        assertEquals(List.of(new Event(1, true), new Event(1, false)), events);
    }
}
