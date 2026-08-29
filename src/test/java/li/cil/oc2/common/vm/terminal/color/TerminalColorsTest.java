package li.cil.oc2.common.vm.terminal.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link TerminalColors#COLORS_256} against the canonical xterm-256 palette.
 *
 * <p>Canonical layout: 0-15 are the standard 16 ANSI colors and must match
 * {@link TerminalColors#COLORS} (0-7) and {@link TerminalColors#BRIGHT_COLORS} (8-15);
 * 16-231 are the 6x6x6 color cube, {@code index = 16 + 36*r + 6*g + b} with per-channel
 * levels {@code [0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff]}; 232-255 are the grayscale ramp,
 * {@code 8 + 10*i} per channel. The whole 256-entry table is checked, so any single
 * mis-typed channel value fails the test.
 */
public class TerminalColorsTest {

    @Test
    void xterm256PaletteMatchesCanonicalFormula() {
        final int[] levels = {0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff};
        for (int i = 0; i < 256; i++) {
            final int expected;
            if (i < 8) {
                expected = TerminalColors.COLORS[i];
            } else if (i < 16) {
                expected = TerminalColors.BRIGHT_COLORS[i - 8];
            } else if (i < 232) {
                final int cube = i - 16;
                final int r = cube / 36;
                final int g = cube / 6 % 6;
                final int b = cube % 6;
                expected = levels[r] << 16 | levels[g] << 8 | levels[b];
            } else {
                final int gray = 8 + 10 * (i - 232);
                expected = gray << 16 | gray << 8 | gray;
            }
            assertEquals(expected, TerminalColors.COLORS_256[i],
                    "index " + i + " deviates from the canonical xterm-256 palette");
        }
    }
}
