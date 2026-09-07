package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static li.cil.oc2.common.vm.terminal.EscapeLiterals.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DECSTR (Soft Terminal Reset, {@code CSI ! p}) integration tests.
 *
 * <p>Feeds real escape sequences through the {@link Terminal} parser (the same path the VM firmware
 * uses) via {@code putOutput}, then asserts on terminal state. DECSTR is the gentler counterpart to
 * RIS: it resets mode tables, rendition, charsets, scroll margins, and the saved cursor to defaults
 * WITHOUT clearing the screen, resetting tab stops, moving the cursor, changing column width,
 * dropping the input queue, or resetting the OSC 4 palette. Semantics verified against DEC
 * VT510-RM Table 5-9 and xterm-410 {@code VTReset(full=false)}.
 *
 * <p>Split out of {@link TerminalBufferTest} so the soft-reset feature has a dedicated home, mirroring
 * the {@code SGRTest} precedent. The RIS-contract tests it is contrasted against live in
 * {@code TerminalBufferTest} ({@code risResets*}, {@code risClearsInput}).
 */
public class DecstrTest {
    private static final String DECSTR = CSI + "!p";
    private static final String RED_RGB = "rgb:ff/00/00";
    private static final String OSC4_SET_16_RED = OSC + "4;16;" + RED_RGB + BEL;

    private Terminal terminal;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
    }

    @Test
    void decstrPreservesScreenContentsAndCursor() {
        // Revert-and-fail: RIS clears the screen and homes the cursor; DECSTR preserves both.
        write(terminal, "Hello");
        write(terminal, CSI + "3;5H"); // cursor to row 3, col 5 -> x=4, y=2
        assertEquals('H', charAt(0, 0), "precondition: content written at (0,0)");
        assertEquals(4, terminal.x, "precondition: cursor x");
        assertEquals(2, terminal.y, "precondition: cursor y");

        write(terminal, DECSTR);

        assertEquals('H', charAt(0, 0), "DECSTR preserves screen contents");
        assertEquals(4, terminal.x, "DECSTR preserves the active cursor x");
        assertEquals(2, terminal.y, "DECSTR preserves the active cursor y");
    }

    @Test
    void decstrResetsSgrRenditionButPreservesOsc4Palette() {
        // Revert-and-fail: RIS resets the OSC 4 palette too; DECSTR resets only SGR rendition.
        write(terminal, OSC4_SET_16_RED);  // palette256[16] = red (OSC 4)
        write(terminal, CSI + "1;31m");    // bold + red foreground (SGR)
        assertEquals(0xff0000, terminal.palette256[16], "precondition: OSC 4 set entry 16 to red");
        assertNotEquals(TerminalColors.getDefaultPalette256()[16], terminal.palette256[16],
            "precondition: entry 16 differs from the default");
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.style, "precondition: bold");

        write(terminal, DECSTR);

        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.style, "DECSTR resets SGR style to default");
        assertEquals(TerminalColors.ColorMode.DEFAULT_FOREGROUND, terminal.currentForegroundColorMode,
            "DECSTR resets the foreground color mode to default");
        assertEquals(0xff0000, terminal.palette256[16], "DECSTR preserves the OSC 4 palette (entry 16)");
        assertNotEquals(TerminalColors.getDefaultPalette256()[16], terminal.palette256[16],
            "DECSTR does not reset the OSC 4 palette to default");
    }

    @Test
    void decstrResetsOriginInsertAndAutowrapModes() {
        write(terminal, CSI + "?6h"); // DECOM on (origin mode)
        write(terminal, CSI + "?7l"); // DECAWM off (autowrap)
        write(terminal, CSI + "4h");  // IRM on (insert mode)
        assertTrue(terminal.currentPrivateModeState.DECOM, "precondition: DECOM on");
        assertFalse(terminal.currentPrivateModeState.DECAWM, "precondition: DECAWM off");
        assertTrue(terminal.currentModeState.IRM, "precondition: IRM on");

        write(terminal, DECSTR);

        assertFalse(terminal.currentPrivateModeState.DECOM, "DECSTR resets DECOM to absolute");
        assertTrue(terminal.currentPrivateModeState.DECAWM, "DECSTR resets DECAWM to default (on)");
        assertFalse(terminal.currentModeState.IRM, "DECSTR resets IRM to replace");
    }

    @Test
    void decstrResetsScrollMarginsToFull() {
        // Corrects OC2R todo.md, which claims DECSTR preserves scroll margins: DEC VT510-RM
        // Table 5-9 and xterm-410 both reset DECSTBM to the full page.
        write(terminal, CSI + "5;10r"); // DECSTBM: top margin 5, bottom margin 10
        assertEquals(4, terminal.scrollFirst, "precondition: top margin");
        assertEquals(9, terminal.scrollLast, "precondition: bottom margin");

        write(terminal, DECSTR);

        assertEquals(0, terminal.scrollFirst, "DECSTR resets the top margin to 0");
        assertEquals(Terminal.HEIGHT - 1, terminal.scrollLast, "DECSTR resets the bottom margin to full page");
    }

    @Test
    void decstrResetsCharsets() {
        write(terminal, ESC + "(0"); // designate G0 = DEC special graphics
        assertEquals(TerminalColors.DrawingMode.SPECIAL_GRAPHICS, terminal.drawingModeG0,
            "precondition: G0 is special graphics");

        write(terminal, DECSTR);

        assertEquals(TerminalColors.DrawingMode.ASCII, terminal.drawingModeG0, "DECSTR resets G0 to ASCII");
        assertEquals(TerminalColors.DrawingMode.ASCII, terminal.drawingModeG1, "DECSTR resets G1 to ASCII");
        assertTrue(terminal.useG0, "DECSTR resets useG0 to true");
    }

    @Test
    void decstrResetsSavedCursorToHome() {
        write(terminal, CSI + "5;5H"); // cursor to (4,4)
        write(terminal, CSI + "1m");   // bold
        write(terminal, ESC + "7");    // DECSC: save cursor + rendition
        assertEquals(4, terminal.savedX, "precondition: saved cursor x");
        assertEquals(4, terminal.savedY, "precondition: saved cursor y");
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.savedStyle, "precondition: saved bold style");

        write(terminal, DECSTR);

        assertEquals(0, terminal.savedX, "DECSTR resets the saved cursor x to home");
        assertEquals(0, terminal.savedY, "DECSTR resets the saved cursor y to home");
        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.savedStyle, "DECSTR resets the saved rendition to default");
    }

    @Test
    void decstrPreservesTabStops() {
        // Revert-and-fail: RIS rebuilds default tab stops (every TAB_WIDTH); DECSTR preserves
        // user-set stops. Column 3 is not a default tab position, so a stop there is unambiguous.
        write(terminal, CSI + "4G"); // CHA: cursor to column 4 (x=3)
        write(terminal, ESC + "H");  // HTS: set a tab stop at the cursor column
        assertTrue(terminal.tabs[3], "precondition: custom tab stop at column 3");

        write(terminal, DECSTR);

        assertTrue(terminal.tabs[3], "DECSTR preserves user-set tab stops");
    }

    @Test
    void decstrPreservesColumnWidthAndDeccolmFlag() {
        // The width/DECCOLM invariant: DECSTR must preserve width AND keep the DECCOLM flag in
        // agreement with it. Revert-and-fail: a soft reset that took a fresh PrivateModeState
        // without re-applying DECCOLM would leave the flag false while width stayed 132.
        write(terminal, CSI + "?3h"); // DECCOLM on -> 132 columns (destructive, like RIS)
        assertEquals(132, terminal.getTerminalWidth(), "precondition: 132 columns");
        assertTrue(terminal.currentPrivateModeState.DECCOLM, "precondition: DECCOLM flag set");

        write(terminal, DECSTR);

        assertEquals(132, terminal.getTerminalWidth(), "DECSTR preserves column width");
        assertTrue(terminal.currentPrivateModeState.DECCOLM,
            "DECSTR preserves the DECCOLM flag in agreement with the preserved width");
    }

    @Test
    void decstrPreservesInputQueue() {
        // Revert-and-fail: RIS clears the input queue; DECSTR preserves it.
        terminal.io.putInput((byte) 'A');
        terminal.io.putInput((byte) 'B');

        write(terminal, DECSTR);

        assertNotEquals(-1, terminal.io.readInput(), "DECSTR preserves the input queue (A)");
        assertNotEquals(-1, terminal.io.readInput(), "DECSTR preserves the input queue (B)");
        assertEquals(-1, terminal.io.readInput(), "input queue is drained after the two bytes");
    }

    @Test
    void decstrResetsSavedPrivateModeState() {
        write(terminal, CSI + "?7l"); // DECAWM off
        write(terminal, CSI + "?6h"); // DECOM on
        write(terminal, CSI + "?7s"); // XTSAVE mode 7 (DECAWM)
        write(terminal, CSI + "?6s"); // XTSAVE mode 6 (DECOM)
        assertFalse(terminal.savePrivateModeState.DECAWM, "precondition: DECAWM saved as off");
        assertTrue(terminal.savePrivateModeState.DECOM, "precondition: DECOM saved as on");

        write(terminal, DECSTR);

        assertTrue(terminal.savePrivateModeState.DECAWM, "DECSTR resets saved DECAWM to default (on)");
        assertFalse(terminal.savePrivateModeState.DECOM, "DECSTR resets saved DECOM to default (off)");
    }

    @Test
    void decstrResetsParserStateToNormal() {
        write(terminal, CSI + "3"); // partial CSI leaves the parser in CONTROL_SEQUENCE
        assertEquals(Terminal.State.CONTROL_SEQUENCE, terminal.state, "precondition: parser mid-sequence");

        write(terminal, DECSTR);

        assertEquals(Terminal.State.NORMAL, terminal.state, "DECSTR returns the parser to NORMAL");
    }

    private void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private char charAt(final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return (char) terminal.buffer[x + row * terminal.width];
    }
}
