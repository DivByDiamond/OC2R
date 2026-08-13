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

        if (terminal.x >= Terminal.WIDTH) {
            if (terminal.currentPrivateModeState.DECAWM) {
                NEL.execute(terminal);
            } else {
                terminal.setCursorPos(Terminal.WIDTH - 1, terminal.y);
            }
        }

        // IRM (Insert Mode): shift characters right before placing new char
        if (terminal.currentModeState.IRM) {
            terminal.bufferManager.insertChars(terminal.y, terminal.x, 1);
        }

        setChar(terminal.x, terminal.y, ch);
        terminal.x++;
    }

    public void setChar(final int x, final int y, final int ch) {
        final boolean altBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        final int index = altBuffer
                ? x + y * Terminal.WIDTH
                : x + (y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT)) * Terminal.WIDTH;

        // Write the character
        if (altBuffer) {
            terminal.altBuffer[index] = ch;
        } else {
            terminal.buffer[index] = ch;
        }

        // Write foreground color
        final TerminalColors.ColorData fgColor;
        switch (terminal.currentForegroundColorMode) {
            case SIXTEEN_COLOR -> fgColor = terminal.sixteenColor.Copy();
            case TWO_FIFTY_SIX_COLOR -> fgColor = terminal.twoFiftySixColor.Copy();
            case TRUE_COLOR -> fgColor = terminal.foregroundColor.Copy();
            case SIXTEEN_COLOR_BRIGHT -> fgColor = terminal.sixteenColorBright.Copy();
            case DEFAULT_FOREGROUND -> fgColor = TerminalColors.DEFAULT_FOREGROUND_COLOR.Copy();
            default -> fgColor = TerminalColors.DEFAULT_FOREGROUND_COLOR.Copy();
        }
        if (altBuffer) {
            terminal.altColors[index] = fgColor;
        } else {
            terminal.colors[index] = fgColor;
        }

        // Write background color
        final TerminalColors.ColorData bgColor;
        switch (terminal.currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> bgColor = terminal.sixteenColor.Copy();
            case TWO_FIFTY_SIX_COLOR -> bgColor = terminal.twoFiftySixColor.Copy();
            case TRUE_COLOR -> bgColor = terminal.backgroundColor.Copy();
            case SIXTEEN_COLOR_BRIGHT -> bgColor = terminal.sixteenColorBright.Copy();
            case DEFAULT_BACKGROUND -> bgColor = TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy();
            default -> bgColor = TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy();
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
                : Terminal.HEIGHT + (terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y) - terminal.lastRowToDisplay);
        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask().accumulateAndGet(1 << dirtyLine, (prev, next) -> prev | next));
    }
}
