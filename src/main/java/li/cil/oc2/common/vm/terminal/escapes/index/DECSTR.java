package li.cil.oc2.common.vm.terminal.escapes.index;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.SavedCursor;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;

/**
 * Soft Terminal Reset (DECSTR, {@code CSI ! p}) — the gentler counterpart to {@link RIS}. Resets
 * rendition, mode tables, parser state, charsets, scroll margins, and the saved cursor to power-on
 * defaults WITHOUT the destructive geometry reset RIS performs.
 *
 * <p>Per DEC VT510-RM Table 5-9 and xterm-410 {@code VTReset(full=false)} (charproc.c,
 * {@code CASE_DECSTR}): DECSTR resets DECSTBM (margins to full page), DECOM (to absolute), DECAWM
 * (to default), IRM (to replace), DECTCEM (to visible via fresh {@code PrivateModeState}),
 * charsets (to ASCII), SGR rendition (colors+style, not the OSC 4 palette), DECSCA, and the
 * saved-cursor position (to home). It does not clear the screen, reset tab stops, move the active
 * cursor, change column width, drop the input queue, or reset the OSC 4 palette — all of which RIS
 * does.
 *
 * <p>Mirrors {@link RIS}: a static reset command in {@code escapes.index} rather than a
 * {@link Terminal} method, so it writes Terminal's public fields from outside (as RIS does). Writing
 * them from inside Terminal would trip SpotBugs {@code PA_PUBLIC_PRIMITIVE_ATTRIBUTE} ("public field
 * set from inside the class"), which RIS avoids precisely by living here.
 *
 * <p>DECAWM: VT510-RM Table 5-9 lists DECAWM off, but xterm restores it to the resource default
 * (on); OC2R's {@link PrivateModeState} default is DECAWM=true (matching xterm), so a fresh
 * {@code PrivateModeState} matches xterm. (OC2R todo.md's claim that DECSTR preserves scroll
 * margins is incorrect — DEC and xterm both reset DECSTBM to full.)
 *
 * <p>Column-width invariant: DECSTR preserves width, so the fresh {@code PrivateModeState} (which
 * clears DECCOLM) re-applies {@code DECCOLM = (width == 132)} to keep the flag and the allocated
 * width in agreement — the same invariant RIS documents.
 */
public class DECSTR {
    public static void execute(final Terminal terminal) {
        terminal.resetRendition();
        final int preservedWidth = terminal.width;
        terminal.currentModeState = new ModeState();
        terminal.currentPrivateModeState = new PrivateModeState();
        // DECSTR preserves column width (unlike RIS), so re-apply DECCOLM to keep the flag and
        // the allocated width in agreement — the same invariant RIS documents.
        terminal.currentPrivateModeState.DECCOLM = (preservedWidth == 132);
        // VT510-RM doesn't specify saved-state handling; mirror RIS.
        terminal.savePrivateModeState = new PrivateModeState();
        terminal.state = Terminal.State.NORMAL;
        // DECSTBM: scroll margins to full page. DEC VT510-RM Table 5-9 and xterm both reset this
        terminal.scrollFirst = 0;
        terminal.scrollLast = Terminal.HEIGHT - 1;
        terminal.drawingModeG0 = TerminalColors.DrawingMode.ASCII;
        terminal.drawingModeG1 = TerminalColors.DrawingMode.ASCII;
        terminal.useG0 = true;
        SavedCursor.reset(terminal); // DECSC saved cursor to home + default rendition
    }
}
