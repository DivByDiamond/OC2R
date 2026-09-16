package li.cil.oc2.common.vm.terminal.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import li.cil.oc2.common.vm.terminal.Terminal;
import org.junit.jupiter.api.Test;

/**
 * §36 M4: the geometry seqlock — a capture racing concurrent resizes must NEVER mix geometry
 * with buffer arrays (a torn frame's structural class). The resizer hammers the two resize
 * paths while the capture loop checks every accepted frame for internal consistency; rejected
 * captures (null, mid-commit or version-moved) are the designed degradation and are counted
 * but not tears. Runs for a fixed wall time: one resize pair costs milliseconds (the color
 * fills dominate), so iteration-count bounds would exit before the resizer warms up.
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"}) // the capture loop's inline consistency checks ARE the test; extracting them would hide what is being pinned
class FrameStateTest {
    private static final long TEST_DURATION_NANOS = 1_000_000_000L;

    @Test
    void captureUnderConcurrentResizeIsAlwaysConsistent() throws InterruptedException {
        final Terminal terminal = new Terminal();
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicInteger resizes = new AtomicInteger();
        final AtomicInteger captures = new AtomicInteger();
        final AtomicInteger skipped = new AtomicInteger();
        final AtomicInteger torn = new AtomicInteger();
        final AtomicReference<Throwable> resizerFailure = new AtomicReference<>();

        final Thread resizer = new Thread(() -> {
            try {
                int w = Terminal.WIDTH;
                int h = Terminal.HEIGHT;
                while (!stop.get()) {
                    w = w == Terminal.WIDTH ? 132 : Terminal.WIDTH;
                    h = h == Terminal.HEIGHT ? 48 : Terminal.HEIGHT;
                    terminal.resizeWidth(w);
                    terminal.resizeHeight(h);
                    resizes.incrementAndGet();
                }
            } catch (final Throwable t) {
                resizerFailure.set(t);
                stop.set(true);
            }
        });
        resizer.start();

        try {
            final long deadline = System.nanoTime() + TEST_DURATION_NANOS;
            while (System.nanoTime() < deadline && !stop.get() && resizerFailure.get() == null) {
                final FrameState frame = FrameState.capture(terminal);
                if (frame == null) {
                    skipped.incrementAndGet(); // mid-commit or version moved: designed retry
                    continue;
                }
                captures.incrementAndGet();
                // Consistency: the captured arrays must match the captured geometry exactly.
                final int mainRows = frame.height() * Terminal.SCROLL_BACK_COUNT;
                if (frame.buffer().length != frame.width() * mainRows
                        || frame.styles().length != frame.width() * mainRows
                        || frame.colors().length != frame.width() * mainRows
                        || frame.altBuffer().length != frame.width() * frame.height()) {
                    torn.incrementAndGet();
                }
            }
        } finally {
            stop.set(true);
            resizer.join();
        }

        if (resizerFailure.get() != null) {
            throw new AssertionError("resizer thread failed", resizerFailure.get());
        }
        assertTrue(resizes.get() > 10, "precondition: the resizer actually raced ("
                + resizes.get() + " resizes, " + captures.get() + " captures, " + skipped.get() + " skipped)");
        assertTrue(captures.get() > 100, "precondition: enough accepted captures ("
                + captures.get() + ", " + skipped.get() + " skipped)");
        assertEquals(0, torn.get(), "no capture may mix geometry with buffer arrays");
    }

    @Test
    void captureRetryingUnderConcurrentResizeNeverReturnsTornFrame() throws InterruptedException {
        // The renderer's contract (Kimi gate F1): captureRetrying yields a consistent frame
        // or null, and the caller DROPS null frames — there is no torn-frame fallback. Same
        // race as above, through the retry helper the renderer actually calls.
        final Terminal terminal = new Terminal();
        assertNotNull(FrameState.captureRetrying(terminal, 2), "quiescent terminal: first attempt succeeds");

        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicInteger captures = new AtomicInteger();
        final AtomicInteger skipped = new AtomicInteger();
        final AtomicInteger torn = new AtomicInteger();
        final AtomicReference<Throwable> resizerFailure = new AtomicReference<>();

        final Thread resizer = new Thread(() -> {
            try {
                int w = Terminal.WIDTH;
                int h = Terminal.HEIGHT;
                while (!stop.get()) {
                    w = w == Terminal.WIDTH ? 132 : Terminal.WIDTH;
                    h = h == Terminal.HEIGHT ? 48 : Terminal.HEIGHT;
                    terminal.resizeWidth(w);
                    terminal.resizeHeight(h);
                }
            } catch (final Throwable t) {
                resizerFailure.set(t);
                stop.set(true);
            }
        });
        resizer.start();

        try {
            final long deadline = System.nanoTime() + TEST_DURATION_NANOS;
            while (System.nanoTime() < deadline && !stop.get() && resizerFailure.get() == null) {
                final FrameState frame = FrameState.captureRetrying(terminal, 2);
                if (frame == null) {
                    skipped.incrementAndGet(); // both attempts hit commit stretches: designed drop
                    continue;
                }
                captures.incrementAndGet();
                final int mainRows = frame.height() * Terminal.SCROLL_BACK_COUNT;
                if (frame.buffer().length != frame.width() * mainRows
                        || frame.styles().length != frame.width() * mainRows
                        || frame.colors().length != frame.width() * mainRows
                        || frame.altBuffer().length != frame.width() * frame.height()) {
                    torn.incrementAndGet();
                }
            }
        } finally {
            stop.set(true);
            resizer.join();
        }

        if (resizerFailure.get() != null) {
            throw new AssertionError("resizer thread failed", resizerFailure.get());
        }
        assertTrue(captures.get() > 100, "precondition: enough accepted captures ("
                + captures.get() + ", " + skipped.get() + " skipped)");
        assertEquals(0, torn.get(), "no retrying capture may mix geometry with buffer arrays");
    }
}
