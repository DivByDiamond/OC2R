package li.cil.oc2.common.vm.terminal.buffer;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.index.NEL;

public class TerminalBufferWriter {
    private final Terminal terminal;

    public TerminalBufferWriter(final Terminal terminal) {
        this.terminal = terminal;
    }

    public void putChar(final int ch) {
        if (Character.isISOControl(ch)) return;

        // Deferred autowrap: if the previous printable filled the last column it left the
        // cursor at width-1 with autowrapPending set. Fire the wrap now, on THIS printable,
        // not the one that filled the margin — so the margin char survives and any control
        // char (BS/CR/Tab) between the two prints can clear the pending wrap instead.
        if (terminal.autowrapPending) {
            if (terminal.currentPrivateModeState.DECAWM) {
                NEL.execute(terminal); // moves to (0, y+1) and clears pending via setCursorPos
            } else {
                terminal.autowrapPending = false; // DECAWM off: overwrite the last column
            }
        }

        // IRM (Insert Mode): shift characters right before placing new char
        if (terminal.currentModeState.IRM) {
            terminal.bufferManager.insertChars(terminal.y, terminal.x, 1);
        }

        setChar(terminal.x, terminal.y, ch);
        // Fill the last column: arm the pending wrap and hold the cursor at width-1
        // (never advance to a phantom width). Otherwise advance normally.
        if (terminal.x == terminal.width - 1) {
            if (terminal.currentPrivateModeState.DECAWM) {
                terminal.autowrapPending = true;
            }
        } else {
            terminal.x++;
        }
    }

    public void setChar(final int x, final int y, final int ch) { // NOPMD: data-driven foreground/background color-mode switches
        final boolean altBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        final int index = altBuffer
                ? x + y * terminal.width
                : x + (y + terminal.lastRowToDisplayMax - Terminal.HEIGHT) * terminal.width;

        // Write the character
        if (altBuffer) {
            terminal.altBuffer[index] = ch;
        } else {
            terminal.buffer[index] = ch;
        }

        // Write foreground color
        final TerminalColors.ColorData fgColor;
        switch (terminal.currentForegroundColorMode) {
            case SIXTEEN_COLOR -> fgColor = terminal.sixteenColor.copy();
            case TWO_FIFTY_SIX_COLOR -> fgColor = terminal.twoFiftySixColor.copy();
            case TRUE_COLOR -> fgColor = terminal.foregroundColor.copy();
            case SIXTEEN_COLOR_BRIGHT -> fgColor = terminal.sixteenColorBright.copy();
            case DEFAULT_FOREGROUND -> fgColor = TerminalColors.DEFAULT_FOREGROUND_COLOR.copy();
            default -> fgColor = TerminalColors.DEFAULT_FOREGROUND_COLOR.copy();
        }
        if (altBuffer) {
            terminal.altColors[index] = fgColor;
        } else {
            terminal.colors[index] = fgColor;
        }

        // Write background color
        final TerminalColors.ColorData bgColor;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> bgColor = terminal.sixteenColor.copy();
            case TWO_FIFTY_SIX_COLOR -> bgColor = terminal.twoFiftySixColor.copy();
            case TRUE_COLOR -> bgColor = terminal.backgroundColor.copy();
            case SIXTEEN_COLOR_BRIGHT -> bgColor = terminal.sixteenColorBright.copy();
            case DEFAULT_BACKGROUND -> bgColor = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();
            default -> bgColor = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();
        }
        if (altBuffer) {
            terminal.altColorsBackground[index] = bgColor;
            terminal.altStyles[index] = terminal.style;
        } else {
            terminal.colorsBackground[index] = bgColor;
            terminal.styles[index] = terminal.style;
        }

        // Mark dirty — alt buffer uses y directly, main buffer needs scrollback offset
        final int dirtyLine = altBuffer
                ? y
                : Terminal.HEIGHT + terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y) - terminal.lastRowToDisplay;
        terminal.markDirty(1 << dirtyLine);
    }

    public static int getDirtyRow(final Terminal terminal, final int y) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            return y;
        }
        int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y);
        return Terminal.HEIGHT + globalY - terminal.lastRowToDisplay;
    }
}
