package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.buffer.TerminalBufferWriter;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH10 extends CSISequenceHandler { // Combined Handler 10 (DCH and XTPUSHCOLORS)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH10(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return state.hash ? new int[0] : new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.hash) { // XTPUSHCOLORS
            LOGGER.warn("XTPUSHCOLORS not implemented");
        } else { // DCH
            int chars = Math.min(args[0], Terminal.WIDTH - terminal.x);
            int startIndex =
                    (terminal.currentPrivateModeState.isAltBufferEnabled()
                                    ? terminal.y * Terminal.WIDTH
                                    : (terminal.y
                                                    + terminal.lastRowToDisplayMax
                                                    - Terminal.HEIGHT)
                                            * Terminal.WIDTH)
                            + terminal.x;
            int count = Terminal.WIDTH - terminal.x - chars;
            int endIndex = startIndex + count;
            TerminalColors.ColorData c;
            switch (terminal.currentBackgroundColorMode) {
                case SIXTEEN_COLOR -> c = terminal.sixteenColor;
                case TWO_FIFTY_SIX_COLOR -> c = terminal.twoFiftySixColor;
                case TRUE_COLOR -> c = terminal.backgroundColor;
                case SIXTEEN_COLOR_BRIGHT -> c = terminal.sixteenColorBright;
                default -> c = TerminalColors.DEFAULT_BACKGROUND_COLOR;
            }
            if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                System.arraycopy(
                        terminal.altBuffer,
                        startIndex + chars,
                        terminal.altBuffer,
                        startIndex,
                        count);
                System.arraycopy(
                        terminal.altColors,
                        startIndex + chars,
                        terminal.altColors,
                        startIndex,
                        count);
                System.arraycopy(
                        terminal.altColorsBackground,
                        startIndex + chars,
                        terminal.altColorsBackground,
                        startIndex,
                        count);
                System.arraycopy(
                        terminal.altStyles,
                        startIndex + chars,
                        terminal.altStyles,
                        startIndex,
                        count);
                Arrays.fill(terminal.altBuffer, endIndex, endIndex + chars, ' ');
                Arrays.fill(
                        terminal.altColors,
                        endIndex,
                        endIndex + chars,
                        TerminalColors.DEFAULT_COLORS.copy());
                Arrays.fill(terminal.altColorsBackground, endIndex, endIndex + chars, c.copy());
                Arrays.fill(
                        terminal.altStyles,
                        endIndex,
                        endIndex + chars,
                        TerminalColors.DEFAULT_STYLE);
            } else {
                System.arraycopy(
                        terminal.buffer, startIndex + chars, terminal.buffer, startIndex, count);
                System.arraycopy(
                        terminal.colors, startIndex + chars, terminal.colors, startIndex, count);
                System.arraycopy(
                        terminal.colorsBackground,
                        startIndex + chars,
                        terminal.colorsBackground,
                        startIndex,
                        count);
                System.arraycopy(
                        terminal.styles, startIndex + chars, terminal.styles, startIndex, count);
                Arrays.fill(terminal.buffer, endIndex, endIndex + chars, ' ');
                Arrays.fill(
                        terminal.colors,
                        endIndex,
                        endIndex + chars,
                        TerminalColors.DEFAULT_COLORS.copy());
                Arrays.fill(terminal.colorsBackground, endIndex, endIndex + chars, c.copy());
                Arrays.fill(
                        terminal.styles,
                        endIndex,
                        endIndex + chars,
                        TerminalColors.DEFAULT_STYLE);
            }

            terminal.markDirty(1 << TerminalBufferWriter.getDirtyRow(terminal, terminal.y));
        }
    }
}