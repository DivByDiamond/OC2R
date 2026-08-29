package li.cil.oc2.common.vm.terminal.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the default xterm-256 palette (via {@link TerminalColors#getDefaultPalette256()})
 * against the canonical layout.
 *
 * <p>Canonical layout: 0-15 are the standard 16 ANSI colors and must match the default ANSI-16
 * tables (0-7 normal, 8-15 bright, read via the per-instance accessors since the arrays are
 * private); 16-231 are the 6x6x6 color cube, {@code index = 16 + 36*r + 6*g + b} with
 * per-channel levels {@code [0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff]}; 232-255 are the grayscale
 * ramp, {@code 8 + 10*i} per channel. The whole 256-entry table is checked, so any single
 * mis-typed channel value fails the test.
 */
public class TerminalColorsTest {

    @Test
    void xterm256PaletteMatchesCanonicalFormula() {
        // Read via the accessors: COLORS/BRIGHT_COLORS/COLORS_256 are private (per-instance
        // restructure for OSC 4), so the cross-check uses the defensive-copy getters.
        final int[] colors = TerminalColors.getDefaultColors16();
        final int[] bright = TerminalColors.getDefaultBrightColors16();
        final int[] palette = TerminalColors.getDefaultPalette256();
        final int[] levels = {0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff};
        for (int i = 0; i < 256; i++) {
            final int expected;
            if (i < 8) {
                expected = colors[i];
            } else if (i < 16) {
                expected = bright[i - 8];
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
            assertEquals(expected, palette[i],
                    "index " + i + " deviates from the canonical xterm-256 palette");
        }
    }

    @Test
    void computeFaintScalesByHalf() {
        // SGR 2 (faint) is a tail modifier scaling the resolved color by 1/2, matching xterm's
        // computeFaint mechanism (util.c) at the out-of-box 1/2 factor. This reproduces the
        // prior fixed DIM_COLORS table for SIXTEEN_COLOR exactly (0xAA0000 -> 0x550000), so
        // existing dim text is unchanged; the gain is dim now composes with bold/256/truecolor.
        // The 1/2 factor (not xterm's 2/3) keeps faint/normal/faintbright/bright as four distinct
        // shades with the VGA/CGA palette (2/3 would collapse faintbright onto normal at 0xAA).
        assertEquals(0x550000, TerminalColors.computeFaint(0xAA0000),
                "dim scales each channel by half (red)");
        assertEquals(0x555555, TerminalColors.computeFaint(0xAAAAAA),
                "dim scales each channel by half (white)");
        assertEquals(0x000000, TerminalColors.computeFaint(0x000000),
                "dim on black stays black");
        // Composes with bright: a bold (bright) red dimmed is a shade in no palette entry.
        assertEquals(0x7f2a2a, TerminalColors.computeFaint(0xFF5555),
                "dim on bright red composes to a synthesized shade");
        // Applies to truecolor: 24-bit resolved color is scaled per-channel.
        assertEquals(0x407f20, TerminalColors.computeFaint(0x80ff40),
                "dim scales truecolor per channel (0x80/0xff/0x40 -> 0x40/0x7f/0x20)");
    }
}
