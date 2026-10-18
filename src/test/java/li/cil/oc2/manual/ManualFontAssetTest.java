package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.*;

import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ManualFontAssetTest {
    @Test
    void everyPrintableAsciiGlyphHasPixelsInTheBundledAtlas() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/oc2r/textures/font/monospace.png")) {
            assertNotNull(stream, "manual bitmap font must be packaged");
            final var atlas = ImageIO.read(stream);
            assertEquals(96, atlas.getWidth());
            assertEquals(96, atlas.getHeight());
            for (int codepoint = 33; codepoint < 127; codepoint++) {
                final int index = codepoint - 32;
                boolean visible = false;
                for (int y = 0; y < 9; y++) {
                    for (int x = 0; x < 6; x++) {
                        visible |= (atlas.getRGB(index % 16 * 6 + x, index / 16 * 9 + y) >>> 24) != 0;
                    }
                }
                assertTrue(visible, "empty glyph: " + (char) codepoint);
            }
        }
    }
}
