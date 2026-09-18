package li.cil.oc2.common.vm.terminal;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;

/**
 * Saved cursor position + rendition for one buffer (main or alt), as written by DECSC/SCOSC and
 * read back by DECRC/SCORC (see {@link li.cil.oc2.common.vm.terminal.escapes.SavedCursor}).
 * {@link Terminal} holds one instance per buffer ({@code savedCursor}/{@code altSavedCursor}).
 * Extracted from {@link Terminal} (А1); every field default here is the power-on default, so
 * RIS's saved-state reset is just {@code new SavedCursorState()} for both buffers.
 */
@Serialized
public class SavedCursorState {
    public int x;
    public int y;
    /**
     * Saved autowrap-pending flag (xterm's {@code sc->wrap_flag}), restored AFTER the cursor
     * move in {@link li.cil.oc2.common.vm.terminal.escapes.SavedCursor#restore}, mirroring
     * xterm's "after CursorSet/ResetWrap" ordering.
     */
    public boolean autowrapPending;
    public byte style = TerminalColors.DEFAULT_STYLE;
    public boolean useG0 = true;
    public int drawingModeG0;
    public int drawingModeG1;
    public ColorMode foregroundColorMode = ColorMode.DEFAULT_FOREGROUND;
    public ColorMode backgroundColorMode = ColorMode.DEFAULT_BACKGROUND;
    public ColorData sixteenColor = TerminalColors.DEFAULT_COLORS.copy();
    public ColorData sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
    public ColorData twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
    public ColorData foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
    public ColorData backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
}
