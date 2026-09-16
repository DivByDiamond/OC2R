package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import li.cil.oc2.common.vm.terminal.render.RendererModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static li.cil.oc2.common.vm.terminal.EscapeLiterals.*;
import static org.junit.jupiter.api.Assertions.*;

public class TerminalRegressionsTest {
    private Terminal terminal;
    private DummyRenderer renderer;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
        renderer = new DummyRenderer();
        terminal.renderers.add(renderer);
    }

    // --- UTF-8 ---

    @Test
    void utf8SplitAcrossWrites() {
        // U+00E9 (e-acute) as 2-byte UTF-8 split across two ByteBuffers
        byte[] part1 = {(byte) 0xC3};
        byte[] part2 = {(byte) 0xA9};
        terminal.io.putOutput(ByteBuffer.wrap(part1));
        assertEquals(' ', charAt(0, 0), "incomplete sequence must not yet render");
        terminal.io.putOutput(ByteBuffer.wrap(part2));
        assertEquals(0x00E9, charAt(0, 0), "split UTF-8 must reassemble across writes");
        assertEquals(1, terminal.x, "cursor advances by one cell");
    }

    @Test
    void invalidUtf8Resync() {
        // 0xFF is never valid in UTF-8 - decoder must resync and next char renders
        byte[] bytes = {(byte) 0xFF, 'A'};
        terminal.io.putOutput(ByteBuffer.wrap(bytes));
        // Invalid byte is replaced/skipped - 'A' should appear at col 0
        assertEquals('A', charAt(0, 0), "invalid byte must not block following char");
    }

    @Test
    void wideCharAtEolDoesNotCorrupt() {
        // Wide char at last column - current engine stores one codepoint per cell, so wide
        // occupies one cell. Test that it does not overflow or corrupt next row.
        String wide = "\u3042"; // hiragana 'a', 3 bytes in UTF-8, single cell in our model
        terminal.setCursorPos(Terminal.WIDTH - 1, 0);
        write(wide);
        assertEquals(0x3042, charAt(Terminal.WIDTH - 1, 0), "wide char at EOL stored");
        assertEquals(' ', charAt(0, 1), "next row not corrupted by wide at EOL");
    }

    // --- Resize ---

    @Test
    void resizeInAltBufferPreservesMain() {
        write("Hello");
        write(CSI + "?1049h"); // alt buffer on
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        write("Alt");
        assertEquals('A', charAt(0, 0), "alt has Alt");
        terminal.resizeWidth(40);
        assertEquals(40, terminal.width, "width changed");
        // alt still active - check alt content survived
        assertEquals('A', charAt(0, 0), "alt preserved across resize");
        write(CSI + "?1049l"); // back to main
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals('H', charAt(0, 0), "main preserved across resize in alt");
        assertEquals('e', charAt(1, 0));
    }

    @Test
    void resizeInsideScrollRegionPreservesMargins() {
        write(CSI + "5;10r"); // scroll region rows 5-10
        assertEquals(4, terminal.scrollFirst);
        assertEquals(9, terminal.scrollLast);
        terminal.resizeHeight(12);
        // Per Terminal.resizeHeight javadoc, margins reset to full page on resize (xterm)
        assertEquals(0, terminal.scrollFirst, "margins reset on height resize");
        assertEquals(11, terminal.scrollLast);
        assertEquals(12, terminal.height);
        assertEquals(12, terminal.lastRowToDisplay, "view stays at bottom after shrink");
        assertEquals(12, terminal.lastRowToDisplayMax);
    }

    @Test
    void decscpp132to80PreservesSurvivingColumns() {
        // Fill row 0 with distinct chars, then shrink 80->40 and check that first 40 survive
        String row = "A".repeat(Terminal.WIDTH);
        write(row);
        assertEquals('A', charAt(0, 0));
        assertEquals('A', charAt(Terminal.WIDTH - 1, 0));
        terminal.resizeWidth(40);
        assertEquals(40, terminal.width);
        assertEquals('A', charAt(0, 0), "leftmost 40 must survive 80->40");
        assertEquals('A', charAt(39, 0));
        // Expand back to 80 - new columns should be default blanks
        terminal.resizeWidth(80);
        assertEquals(80, terminal.width);
        assertEquals('A', charAt(0, 0), "original left part still there after 40->80");
        assertEquals(' ', charAt(40, 0), "new columns default to blank");
    }

    // --- helpers ---

    private void write(String s) {
        terminal.io.putOutput(ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)));
    }

    private void write(byte[] bytes) {
        terminal.io.putOutput(ByteBuffer.wrap(bytes));
    }

    private int charAt(int x, int y) {
        int idx = x + (y + terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            idx = x + y * terminal.width;
            return terminal.altBuffer[idx];
        }
        return terminal.buffer[idx];
    }

    private static class DummyRenderer implements RendererModel {
        private final AtomicLong dirtyMask = new AtomicLong(-1L);
        @Override public AtomicLong getDirtyMask() { return dirtyMask; }
        @Override public void close() { dirtyMask.set(0L); }
    }
}
