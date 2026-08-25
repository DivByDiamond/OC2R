package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;

/**
 * Unified cursor save/restore for the DECSC/DECRC, SCOSC/SCORC, and SAVE_CURSOR/SAVE_CLEAR_AND_SWITCH
 * families — plus the saved-state reset in RIS. All five previously duplicated the same field
 * copies (with subtle drift: RCP restored position-only while DECRC restored the full rendition;
 * CH2/CH3 had their own position-only copies). This single source of truth prevents that drift.
 *
 * <p>The saved state lives on {@link Terminal} as 13 field pairs (main + alt buffer); this helper
 * owns the read/write of those fields so the storage layout is not re-architected, only the logic
 * is unified. Per xterm-410 the DECSC, SCORC and DECRC restores all use the same scope (xterm's
 * {@code DECSC_FLAGS} — cursor position, SGR attributes, charsets, autowrap, origin); SCOSC is the
 * same save. So {@link #restore} restores the full saved state, not position-only.
 *
 * <p>{@link #restore} clears {@code autowrapPending} (a cursor move clears the pending wrap,
 * matching xterm's {@code ResetWrap} called from {@code CursorSet} inside {@code CursorRestoreFlags}).
 */
public final class SavedCursor {
    private SavedCursor() {
    }

    /**
     * Resets the saved cursor state to power-on defaults for both buffers. Used by RIS so the
     * saved-state field names are referenced in exactly one place (this class).
     */
    public static void reset(final Terminal terminal) {
        terminal.savedX = 0;
        terminal.savedY = 0;
        terminal.altSavedX = 0;
        terminal.altSavedY = 0;
        terminal.savedStyle = TerminalColors.DEFAULT_STYLE;
        terminal.savedUseG0 = true;
        terminal.savedDrawingModeG0 = TerminalColors.DrawingMode.ASCII;
        terminal.savedDrawingModeG1 = TerminalColors.DrawingMode.ASCII;
        terminal.savedForegroundColorMode = TerminalColors.ColorMode.DEFAULT_FOREGROUND;
        terminal.savedBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
        terminal.savedSixteenColor = TerminalColors.DEFAULT_COLORS.copy();
        terminal.savedSixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
        terminal.savedTwoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
        terminal.savedForegroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
        terminal.savedBackgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
        terminal.altSavedStyle = TerminalColors.DEFAULT_STYLE;
        terminal.altSavedUseG0 = true;
        terminal.altSavedDrawingModeG0 = TerminalColors.DrawingMode.ASCII;
        terminal.altSavedDrawingModeG1 = TerminalColors.DrawingMode.ASCII;
        terminal.altSavedForegroundColorMode = TerminalColors.ColorMode.DEFAULT_FOREGROUND;
        terminal.altSavedBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
        terminal.altSavedSixteenColor = TerminalColors.DEFAULT_COLORS.copy();
        terminal.altSavedSixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
        terminal.altSavedTwoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
        terminal.altSavedForegroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
        terminal.altSavedBackgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
    }

    /**
     * Saves the cursor position and rendition to the saved-state fields for the active buffer.
     * Used by DECSC (ESC 7), SCOSC (CSI s), and SAVE_CURSOR (DECSET ?1047/?1049).
     */
    public static void save(final Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.altSavedX = terminal.x;
            terminal.altSavedY = terminal.y;
            terminal.altSavedStyle = terminal.style;
            terminal.altSavedUseG0 = terminal.useG0;
            terminal.altSavedDrawingModeG0 = terminal.drawingModeG0;
            terminal.altSavedDrawingModeG1 = terminal.drawingModeG1;
            terminal.altSavedForegroundColorMode = terminal.currentForegroundColorMode;
            terminal.altSavedBackgroundColorMode = terminal.currentBackgroundColorMode;
            terminal.altSavedSixteenColor = terminal.sixteenColor.copy();
            terminal.altSavedSixteenColorBright = terminal.sixteenColorBright.copy();
            terminal.altSavedTwoFiftySixColor = terminal.twoFiftySixColor.copy();
            terminal.altSavedForegroundColor = terminal.foregroundColor.copy();
            terminal.altSavedBackgroundColor = terminal.backgroundColor.copy();
        } else {
            terminal.savedX = terminal.x;
            terminal.savedY = terminal.y;
            terminal.savedStyle = terminal.style;
            terminal.savedUseG0 = terminal.useG0;
            terminal.savedDrawingModeG0 = terminal.drawingModeG0;
            terminal.savedDrawingModeG1 = terminal.drawingModeG1;
            terminal.savedForegroundColorMode = terminal.currentForegroundColorMode;
            terminal.savedBackgroundColorMode = terminal.currentBackgroundColorMode;
            terminal.savedSixteenColor = terminal.sixteenColor.copy();
            terminal.savedSixteenColorBright = terminal.sixteenColorBright.copy();
            terminal.savedTwoFiftySixColor = terminal.twoFiftySixColor.copy();
            terminal.savedForegroundColor = terminal.foregroundColor.copy();
            terminal.savedBackgroundColor = terminal.backgroundColor.copy();
        }
    }

    /**
     * Restores the cursor position and rendition from the saved-state fields for the active
     * buffer, clearing {@code autowrapPending}. Used by DECRC (ESC 8), SCORC (CSI u), and
     * SAVE_CURSOR / SAVE_CLEAR_AND_SWITCH (DECRST ?1047/?1049). Restores the full saved state
     * (matching xterm's DECSC_FLAGS scope), not position-only.
     */
    public static void restore(final Terminal terminal) {
        terminal.autowrapPending = false; // a cursor move clears the pending wrap (xterm ResetWrap)
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.setCursorPos(terminal.altSavedX, terminal.altSavedY);
            terminal.style = terminal.altSavedStyle;
            terminal.useG0 = terminal.altSavedUseG0;
            terminal.drawingModeG0 = terminal.altSavedDrawingModeG0;
            terminal.drawingModeG1 = terminal.altSavedDrawingModeG1;
            terminal.currentForegroundColorMode = terminal.altSavedForegroundColorMode;
            terminal.currentBackgroundColorMode = terminal.altSavedBackgroundColorMode;
            terminal.sixteenColor = terminal.altSavedSixteenColor.copy();
            terminal.sixteenColorBright = terminal.altSavedSixteenColorBright.copy();
            terminal.twoFiftySixColor = terminal.altSavedTwoFiftySixColor.copy();
            terminal.foregroundColor = terminal.altSavedForegroundColor.copy();
            terminal.backgroundColor = terminal.altSavedBackgroundColor.copy();
        } else {
            terminal.setCursorPos(terminal.savedX, terminal.savedY);
            terminal.style = terminal.savedStyle;
            terminal.useG0 = terminal.savedUseG0;
            terminal.drawingModeG0 = terminal.savedDrawingModeG0;
            terminal.drawingModeG1 = terminal.savedDrawingModeG1;
            terminal.currentForegroundColorMode = terminal.savedForegroundColorMode;
            terminal.currentBackgroundColorMode = terminal.savedBackgroundColorMode;
            terminal.sixteenColor = terminal.savedSixteenColor.copy();
            terminal.sixteenColorBright = terminal.savedSixteenColorBright.copy();
            terminal.twoFiftySixColor = terminal.savedTwoFiftySixColor.copy();
            terminal.foregroundColor = terminal.savedForegroundColor.copy();
            terminal.backgroundColor = terminal.savedBackgroundColor.copy();
        }
    }
}
