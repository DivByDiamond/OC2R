package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.SavedCursorState;
import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * Unified cursor save/restore for the DECSC/DECRC, SCOSC/SCORC, and SAVE_CURSOR/SAVE_CLEAR_AND_SWITCH
 * families — plus the saved-state reset in RIS. All five previously duplicated the same field
 * copies (with subtle drift: RCP restored position-only while DECRC restored the full rendition;
 * CH2/CH3 had their own position-only copies). This single source of truth prevents that drift.
 *
 * <p>The saved state lives on {@link Terminal} as two {@link SavedCursorState} instances (main +
 * alt buffer); this helper owns the read/write of those fields. Per xterm-410 the DECSC, SCORC
 * and DECRC restores all use the same scope (xterm's {@code DECSC_FLAGS} — cursor position, SGR
 * attributes, charsets, autowrap, origin); SCOSC is the same save. So {@link #restore} restores
 * the full saved state, not position-only.
 *
 * <p>{@link #restore} clears {@code autowrapPending} (a cursor move clears the pending wrap,
 * matching xterm's {@code ResetWrap} called from {@code CursorSet} inside {@code CursorRestoreFlags}).
 */
public final class SavedCursor {
    private SavedCursor() {
    }

    /**
     * Resets the saved cursor state to power-on defaults for both buffers. Used by RIS so the
     * saved-state field names are referenced in exactly one place (this class). Every
     * {@link SavedCursorState} field default IS the power-on default, so this is just two fresh
     * instances.
     */
    public static void reset(final Terminal terminal) {
        terminal.savedCursor = new SavedCursorState();
        terminal.altSavedCursor = new SavedCursorState();
    }

    /**
     * Saves the cursor position and rendition to the saved-state fields for the active buffer.
     * Used by DECSC (ESC 7), SCOSC (CSI s), and SAVE_CURSOR (DECSET ?1047/?1049).
     */
    public static void save(final Terminal terminal) {
        final SavedCursorState saved = terminal.currentPrivateModeState.isAltBufferEnabled()
                ? terminal.altSavedCursor
                : terminal.savedCursor;
        saved.x = terminal.x;
        saved.y = terminal.y;
        saved.autowrapPending = terminal.autowrapPending;
        saved.style = terminal.style;
        saved.useG0 = terminal.useG0;
        saved.drawingModeG0 = terminal.drawingModeG0;
        saved.drawingModeG1 = terminal.drawingModeG1;
        saved.foregroundColorMode = terminal.currentForegroundColorMode;
        saved.backgroundColorMode = terminal.currentBackgroundColorMode;
        saved.sixteenColor = terminal.sixteenColor.copy();
        saved.sixteenColorBright = terminal.sixteenColorBright.copy();
        saved.twoFiftySixColor = terminal.twoFiftySixColor.copy();
        saved.foregroundColor = terminal.foregroundColor.copy();
        saved.backgroundColor = terminal.backgroundColor.copy();
    }

    /**
     * Restores the cursor position and rendition from the saved-state fields for the active
     * buffer. Used by DECRC (ESC 8), SCORC (CSI u), and SAVE_CURSOR / SAVE_CLEAR_AND_SWITCH
     * (DECRST ?1047/?1049). Restores the full saved state (matching xterm's DECSC_FLAGS scope),
     * not position-only. The saved {@code autowrapPending} is restored AFTER the cursor move
     * (via {@code setCursorPos}), mirroring xterm's {@code CursorRestoreFlags} which sets
     * {@code do_wrap = sc->wrap_flag} after {@code CursorSet}/ResetWrap — so a cursor that was
     * saved mid-pending-wrap restores still pending. {@code lastPrintedChar} is intentionally
     * not saved (xterm's {@code lastchar} is not part of {@code CursorSave2}); the
     * {@code setCursorPos} clears it, as a restored cursor has no preceding graphic char.
     */
    public static void restore(final Terminal terminal) {
        final SavedCursorState saved = terminal.currentPrivateModeState.isAltBufferEnabled()
                ? terminal.altSavedCursor
                : terminal.savedCursor;
        terminal.setCursorPos(saved.x, saved.y);
        terminal.style = saved.style;
        terminal.useG0 = saved.useG0;
        terminal.drawingModeG0 = saved.drawingModeG0;
        terminal.drawingModeG1 = saved.drawingModeG1;
        terminal.currentForegroundColorMode = saved.foregroundColorMode;
        terminal.currentBackgroundColorMode = saved.backgroundColorMode;
        terminal.sixteenColor = saved.sixteenColor.copy();
        terminal.sixteenColorBright = saved.sixteenColorBright.copy();
        terminal.twoFiftySixColor = saved.twoFiftySixColor.copy();
        terminal.foregroundColor = saved.foregroundColor.copy();
        terminal.backgroundColor = saved.backgroundColor.copy();
        terminal.autowrapPending = saved.autowrapPending;
    }
}
