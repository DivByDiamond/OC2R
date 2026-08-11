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

        setChar(terminal.x, terminal.y, ch);
        terminal.x++;
    }

    public void setChar(final int x, final int y, final int ch) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            final int index = x + y * Terminal.WIDTH;

            terminal.altBuffer[index] = ch;

            switch (terminal.currentForegroundColorMode) {
                case SIXTEEN_COLOR -> terminal.altColors[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.altColors[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR -> terminal.altColors[index] = terminal.foregroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.altColors[index] = terminal.sixteenColorBright.Copy();
                default -> {}
            }

            switch (terminal.currentBackgroundColorMode) {
                case SIXTEEN_COLOR ->
                        terminal.altColorsBackground[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.altColorsBackground[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR ->
                        terminal.altColorsBackground[index] = terminal.backgroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.altColorsBackground[index] = terminal.sixteenColorBright.Copy();
                case DEFAULT_BACKGROUND ->
                        terminal.altColorsBackground[index] = TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy();
                default -> {}
            }

            terminal.altStyles[index] = terminal.style;
            terminal.renderers.forEach(
                    model ->
                            model.getDirtyMask()
                                    .accumulateAndGet(1 << (y), (prev, next) -> prev | next));
        } else {
            int correctedY = (y + (terminal.lastRowToDisplayMax - Terminal.HEIGHT));
            final int index = x + correctedY * Terminal.WIDTH;

            terminal.buffer[index] = ch;

            switch (terminal.currentForegroundColorMode) {
                case SIXTEEN_COLOR -> terminal.colors[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.colors[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR -> terminal.colors[index] = terminal.foregroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.colors[index] = terminal.sixteenColorBright.Copy();
                default -> {}
            }

            switch (terminal.currentBackgroundColorMode) {
                case SIXTEEN_COLOR ->
                        terminal.colorsBackground[index] = terminal.sixteenColor.Copy();
                case TWO_FIFTY_SIX_COLOR ->
                        terminal.colorsBackground[index] = terminal.twoFiftySixColor.Copy();
                case TRUE_COLOR ->
                        terminal.colorsBackground[index] = terminal.backgroundColor.Copy();
                case SIXTEEN_COLOR_BRIGHT ->
                        terminal.colorsBackground[index] = terminal.sixteenColorBright.Copy();
                case DEFAULT_BACKGROUND ->
                        terminal.colorsBackground[index] = TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy();
                default -> {}
            }

            terminal.styles[index] = terminal.style;
            int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - y);
            int localY = Terminal.HEIGHT + (globalY - terminal.lastRowToDisplay);
            terminal.renderers.forEach(
                    model ->
                            model.getDirtyMask()
                                    .accumulateAndGet(1 << (localY), (prev, next) -> prev | next));
        }
    }
}
