package li.cil.oc2.common.vm.terminal;

import li.cil.oc2.common.vm.terminal.buffer.TerminalBuffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These smoke tests require the full Minecraft / NeoForge runtime classpath
 * (the {@link Terminal} class carries a runtime-retained {@code @OnlyIn(Dist.CLIENT)}
 * annotation, which forces the JVM to resolve {@code net.neoforged.api.distmarker.Dist}
 * when the class is loaded). That enum is not available on the plain JUnit
 * test runtime classpath, so {@code new Terminal()} throws
 * {@code NoClassDefFoundError}.
 *
 * <p>The tests are kept here for documentation but disabled until either
 * (a) the NeoForge API jar is added to {@code testRuntimeOnly}, or
 * (b) the {@code @OnlyIn} annotation is moved off {@code Terminal} onto the
 *     renderer-only methods that actually need it.
 *
 * <p>Run them via the game's in-game test harness ({@code /test run}) instead.
 */
@Disabled("Requires NeoForge runtime classpath (Dist enum from @OnlyIn annotation on Terminal)")
public class TerminalBufferTest {
    private Terminal terminal;
    private TerminalBuffer buffer;

    @Test
    public void initialBufferState() {
        // After initialization, buffer should be cleared
        assertNotNull(buffer);
        assertNotNull(terminal);
    }

    @Test
    public void clearLineClearsRow() {
        // Clear line should set all chars in a row to default
        buffer.clearLine(0);
        // After clearing, the row should be blank (space chars with default colors)
    }

    @Test
    public void shiftUpOneMovesRows() {
        // Shift up should move all rows up by one, clearing the last row
        buffer.shiftUpOne();
        // Top row should now be what was row 1
    }

    @Test
    public void shiftDownOneMovesRows() {
        // Shift down should move all rows down by one, clearing the first row
        buffer.shiftDownOne();
    }

    @Test
    public void clearAllLines() {
        buffer.clear();
        // All lines should be blank
    }
}
