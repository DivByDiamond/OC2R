package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static li.cil.oc2.common.vm.terminal.EscapeLiterals.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ST-terminated string sequences (OSC/DCS/APC): termination + abort parity with xterm.
 *
 * <p>xterm handles ESC/ST/BEL/CAN/SUB for all string types in one place — the sos_table
 * (VTPrsTbl.c) — and OC2R mirrors that with a single {@code TerminalOutput.handleStringByte}.
 * These tests cover the behaviors the previous per-manager ESC handling got wrong or inconsistent:
 * ESC + backslash completes ST (regression); BEL terminates OSC only; ESC + a non-backslash byte
 * drops the held payload and re-dispatches that byte as a fresh escape (ESC [ mid-OSC starts a CSI,
 * not a buffered '['); CAN/SUB abort; ESC ESC holds the string until the resolving byte; and the
 * pre-fix swallow (OSC) and stuck-until-ST (DCS/APC) bugs stay fixed.
 *
 * <p>Escape bytes come from {@link EscapeLiterals} so the sequences read as named, composable parts.
 */
public class StringSequenceTest {
    private Terminal terminal;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
    }

    // Reusable OSC 4 payloads (entry 1). Green = the "set applies" cases; red = the "aborted set
    // must not apply" cases. Composed from the shared EscapeLiterals so the bytes stay single-sourced.
    private static final String OSC4_SET_1_GREEN = OSC + "4;1;rgb:00/ff/00";
    private static final String OSC4_SET_1_RED = OSC + "4;1;rgb:ff/00/00";

    @Test
    void oscStTerminatesAndProcessesPayload() {
        // Regression: ESC \ (ST) still terminates OSC and processes the payload.
        final int before = terminal.palette256[1];
        write(OSC4_SET_1_GREEN + ST);
        assertEquals(Terminal.State.NORMAL, terminal.state, "ST must return to ground");
        assertEquals(0x00FF00, terminal.palette256[1], "OSC 4 set via ST must apply");
        assertNotEquals(before, terminal.palette256[1]);
    }

    @Test
    void oscBelTerminatesAndProcessesPayload() {
        // Regression: BEL terminates OSC (xterm CASE_BELL, OSC path) and processes the payload.
        write(OSC4_SET_1_GREEN + BEL);
        assertEquals(Terminal.State.NORMAL, terminal.state, "BEL must terminate OSC");
        assertEquals(0x00FF00, terminal.palette256[1], "OSC 4 set via BEL must apply");
    }

    @Test
    void escBelMidOscTerminatesAndProcessesPayload() {
        // ESC BEL mid-OSC: the ESC is the transition to esc_table (string held), BEL is CASE_BELL,
        // which for OSC terminates and processes the payload (charproc.c:3687). The old per-manager
        // code processed this (its BEL arm was independent of the armed ESC); the centralized handler
        // must not regress it by dropping the payload on the armed ESC.
        final int before = terminal.palette256[1];
        write(OSC4_SET_1_GREEN + ESC + BEL);
        assertEquals(Terminal.State.NORMAL, terminal.state, "ESC BEL must terminate OSC");
        assertEquals(0x00FF00, terminal.palette256[1], "OSC 4 set via ESC BEL must apply");
        assertNotEquals(before, terminal.palette256[1]);
    }

    @Test
    void escBelMidDcsRingsAndStaysPending() {
        // ESC BEL mid-DCS: xterm's CASE_BELL non-OSC path rings and keeps the string held in
        // esc_table (charproc.c:3697) — it does NOT terminate. So the DCS is still pending: a
        // following ST (ESC \) completes it. Assert the bell rang, the state stayed DCS, and the
        // later ST resolves to NORMAL (proving the ESC stayed pending across the BEL).
        write(DCS + "some-dcs-data" + ESC + BEL);
        assertTrue(terminal.hasPendingBell, "ESC BEL mid-DCS must ring the bell");
        assertEquals(Terminal.State.DCS, terminal.state, "ESC BEL must not terminate DCS");
        write(ST); // the pending ESC now resolves via ST
        assertEquals(Terminal.State.NORMAL, terminal.state, "ST after ESC BEL must terminate the DCS");
    }

    @Test
    void escCanMidOscAbortsAndReturnsToGround() {
        // ESC CAN mid-OSC: xterm CASE_CAN aborts to ground. CAN is a cancel byte, not an escape
        // introducer, so it must abort directly (no re-dispatch, no "invalid escape" warning) and
        // leave the parser in ground for the following CSI.
        moveCursorToCol4Row4();
        final int before = terminal.palette256[1];
        write(OSC4_SET_1_RED + ESC + CAN + CSI + "H"); // ESC CAN aborts, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "ESC CAN must abort OSC to ground");
        assertEquals(before, terminal.palette256[1], "aborted OSC 4 set must not apply");
        assertEquals(0, terminal.x, "after ESC CAN, ESC [ must start a fresh CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void escSubMidOscAbortsAndReturnsToGround() {
        moveCursorToCol4Row4();
        write(OSC4_SET_1_RED + ESC + SUB + CSI + "H"); // ESC SUB aborts, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "ESC SUB must abort OSC to ground");
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void escCanMidDcsAbortsAndReturnsToGround() {
        // CAN/SUB abort is uniform across the string states; pin DCS (only OSC was pinned before).
        moveCursorToCol4Row4();
        write(DCS + "some-dcs-data" + ESC + CAN + CSI + "H"); // ESC CAN aborts DCS, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "ESC CAN must abort DCS to ground");
        assertEquals(0, terminal.x, "after ESC CAN, ESC [ must start a fresh CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void escPNestedDcsStartMidOscAbortsOsc() {
        // ESC P mid-OSC: the OSC aborts and a fresh DCS begins (OC2R abort-and-start-fresh; xterm's
        // BeginString would append to the held payload, which OC2R diverges from for sanity). The
        // OSC payload must NOT apply, and the DCS must then terminate cleanly on ST.
        final int before = terminal.palette256[1];
        write(OSC4_SET_1_RED + ESC + "P" + "q"); // ESC P: abort OSC, start DCS, 'q' = sixel
        assertEquals(Terminal.State.DCS, terminal.state, "ESC P mid-OSC must start a DCS");
        assertEquals(before, terminal.palette256[1], "aborted OSC payload must not apply");
        write(ST); // terminate the DCS
        assertEquals(Terminal.State.NORMAL, terminal.state, "ST must terminate the nested DCS");
    }

    @Test
    void escThenNonBackslashAbortsOscAndRedispatchesByte() {
        // xterm: ESC mid-string -> esc_table; a non-backslash byte drops the held string and begins
        // a fresh escape. ESC [ mid-OSC must abort the OSC and start a CSI (CUP executes), not
        // buffer '[' as payload. Pre-fix, OC2R buffered '[' and stayed in OSC (swallow).
        moveCursorToCol4Row4();
        write(OSC4_SET_1_RED + CSI + "H"); // OSC aborted by ESC [, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state,
            "ESC + non-backslash must abort OSC and return to ground");
        assertEquals(0, terminal.x, "byte after ESC must re-dispatch: ESC [ starts a CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void abortedOscPayloadIsDropped() {
        // xterm drops the accumulated payload on abort (charproc.c:3478 clear) — it is NOT
        // processed. Pre-fix, OC2R kept accumulating and would eventually process a corrupted set.
        final int before = terminal.palette256[1];
        write(OSC4_SET_1_RED + CSI + "H"); // aborted: red set must NOT apply
        assertEquals(before, terminal.palette256[1], "aborted OSC 4 set must not apply");
        assertEquals(Terminal.State.NORMAL, terminal.state);
    }

    @Test
    void dcsEscThenNonBackslashAbortsAndRedispatches() {
        // Pre-fix: DCS swallowed ESC + non-backslash and stayed in DCS until a real ST arrived
        // (stuck — a griefer vector in Minecraft). Now it aborts and re-dispatches the byte.
        moveCursorToCol4Row4();
        write(DCS + "some-dcs-data" + CSI + "H"); // DCS aborted by ESC [, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "ESC + non-backslash must abort DCS");
        assertEquals(0, terminal.x, "byte after ESC must re-dispatch as CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void apcEscThenNonBackslashAbortsAndRedispatches() {
        moveCursorToCol4Row4();
        write(APC + "some-apc-data" + CSI + "H"); // APC aborted by ESC [, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "ESC + non-backslash must abort APC");
        assertEquals(0, terminal.x, "byte after ESC must re-dispatch as CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void canAbortsOscAndReturnsToGround() {
        // xterm CASE_CAN aborts the current sequence; CAN mid-string drops it and returns to
        // ground, so a following ESC [ starts a fresh CSI. Pre-fix, CAN was buffered as OSC payload.
        moveCursorToCol4Row4();
        write(OSC4_SET_1_RED + CAN + CSI + "H"); // CAN aborts, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "CAN must abort OSC and return to ground");
        assertEquals(0, terminal.x, "after CAN, ESC [ must start a fresh CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void subAbortsOscAndReturnsToGround() {
        moveCursorToCol4Row4();
        write(OSC4_SET_1_RED + SUB + CSI + "H"); // SUB aborts, then CUP home
        assertEquals(Terminal.State.NORMAL, terminal.state, "SUB must abort OSC and return to ground");
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void escEscHoldsStringUntilResolvingByte() {
        // xterm: ESC ESC re-enters esc_table with the string held (charproc.c CASE_ESC). The
        // payload is processed only if '\' follows (next test), dropped otherwise. Here ESC ESC [
        // drops the string and starts a CSI.
        moveCursorToCol4Row4();
        write(OSC4_SET_1_RED + ESC + CSI + "H"); // ESC ESC [ H: 2nd ESC re-arms, [ aborts, H = CUP
        assertEquals(Terminal.State.NORMAL, terminal.state);
        assertEquals(0, terminal.x, "ESC ESC [ must drop the string and start a CSI (CUP home)");
        assertEquals(0, terminal.y);
    }

    @Test
    void escEscThenBackslashCompletesSt() {
        // xterm: ESC ESC \ — the first ESC enters esc_table, the second re-enters (string held),
        // '\' completes ST. The accumulated payload is processed (not dropped).
        write(OSC4_SET_1_GREEN + ESC + ST); // ESC ESC \
        assertEquals(Terminal.State.NORMAL, terminal.state);
        assertEquals(0x00FF00, terminal.palette256[1], "ESC ESC + ST must complete ST and process the payload");
    }

    @Test
    void backslashAsContentDoesNotTerminate() {
        // A bare '\' (not preceded by ESC) is content, not ST. It must not terminate the string —
        // only ESC '\' (ST) does. After a content '\', the state stays in the string until a real
        // terminator arrives.
        write(OSC4_SET_1_GREEN + BACKSLASH); // content '\' only, no terminator yet
        assertEquals(Terminal.State.OSC, terminal.state, "a bare backslash must not terminate OSC");
        write(ST); // now a real ST
        assertEquals(Terminal.State.NORMAL, terminal.state, "ST must terminate OSC");
    }

    private void moveCursorToCol4Row4() {
        write(CSI + "5;5H"); // CUP row 5 col 5 -> 0-indexed (x=4, y=4)
        assertEquals(4, terminal.x, "precondition: cursor moved to col 4");
        assertEquals(4, terminal.y, "precondition: cursor moved to row 4");
    }

    private void write(final String text) {
        terminal.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }
}
