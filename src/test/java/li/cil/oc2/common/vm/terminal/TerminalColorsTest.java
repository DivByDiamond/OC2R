package li.cil.oc2.common.vm.terminal.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TerminalColorsTest {

    @Test
    void xterm256PaletteMatchesCanonicalFormula() {
        // §36 m7: xterm-256 color cube is (16 + 36*r + 6*g + b) with per-channel levels [0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff].
        // The COLORS_256 array had 29 mis-typed entries using 0xdf instead of 0xd7 for the level-4 component.
        // This test checks a few key canonical indices.

        // Level-4 (0xd7) red, level-4 green, level-4 blue (not level-5): index 188 = 16 + 36*4 + 6*4 + 4
        assertEquals(0xd7d7d7, TerminalColors.COLORS_256[188],
                "index 188 (R=4,G=4,B=4) should be 0xd7d7d7 (was mis-typed as 0xdfdfdf)");

        // R=4, G=3, B=0: index 178 = 16 + 36*4 + 6*3 + 0
        assertEquals(0xd7af00, TerminalColors.COLORS_256[178],
                "index 178 (R=4,G=3,B=0) should be 0xd7af00 (was mis-typed as 0xdfaf00)");

        // R=5, G=5, B=4: index 230 = 16 + 36*5 + 6*5 + 4
        assertEquals(0xffffd7, TerminalColors.COLORS_256[230],
                "index 230 (R=5,G=5,B=4) should be 0xffffd7 (was mis-typed as 0xffffdf)");

        // R=5, G=4, B=0: index 220 = 16 + 36*5 + 6*4 + 0
        assertEquals(0xffd700, TerminalColors.COLORS_256[220],
                "index 220 (R=5,G=4,B=0) should be 0xffd700 (was mis-typed as 0xffdf00)");

        // Verify no 0xdf entries exist in the cube region (16-231)
        for (int i = 16; i < 232; i++) {
            String hex = String.format("%06x", TerminalColors.COLORS_256[i]);
            assertFalse(hex.contains("df"),
                    "index " + i + " should not contain 'df' (0xdf is not a valid xterm-256 level component)");
        }
    }
}
