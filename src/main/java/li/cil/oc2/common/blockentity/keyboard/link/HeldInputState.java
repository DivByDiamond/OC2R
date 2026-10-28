package li.cil.oc2.common.blockentity.keyboard.link;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/** Tracks accepted guest key/button transitions and guarantees balanced releases. */
public final class HeldInputState {
    private final Set<Integer> held = new HashSet<>();

    /** Number of keys and buttons needing release when capture ends. */
    public int size() { return held.size(); }

    /** Emits valid state transitions; duplicate presses and unmatched releases are ignored. */
    public void update(final int code, final boolean down, final BiConsumer<Integer, Boolean> output) {
        if (code < 1 || code > 0x2ff) return;
        if (down ? held.add(code) : held.remove(code)) output.accept(code, down);
    }

    /** Releases all pressed keys and buttons before focus or capture is discarded. */
    public void release(final BiConsumer<Integer, Boolean> output) {
        for (final int code : held) output.accept(code, false);
        held.clear();
    }
}
