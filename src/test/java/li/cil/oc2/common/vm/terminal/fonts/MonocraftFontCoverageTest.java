package li.cil.oc2.common.vm.terminal.fonts;

import java.awt.Font;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled Monocraft builds must carry the glyphs the terminal's charset mapping can
 * produce but that have no procedural fallback — today that is the DEC Special Graphics
 * medium shade (U+2592), which lives in the font via the patched-Monocraft build (upstream
 * HEAD 4.2 + the shade matrix; see todo.md §44.5). A font swap that silently drops it
 * regresses the render to .notdef tofu while every buffer-level test stays green (they
 * assert codepoints, not ink) — this test is the guard for exactly that blind spot.
 *
 * <p>Pure AWT against the packaged TTFs; no Minecraft classes, headless-safe.
 */
public class MonocraftFontCoverageTest {
    private static final String[] FONT_FILES = {
            "/assets/oc2r/fonts/monocraft-r.ttf",
            "/assets/oc2r/fonts/monocraft-b.ttf",
            "/assets/oc2r/fonts/monocraft-i.ttf",
            "/assets/oc2r/fonts/monocraft-bi.ttf",
    };

    @Test
    void bundledFontsCarryDecSpecialGraphicsShade() throws Exception {
        for (final String path : FONT_FILES) {
            try (InputStream is = MonocraftFontCoverageTest.class.getResourceAsStream(path)) {
                assertTrue(is != null, "font resource missing: " + path);
                final Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                assertTrue(font.canDisplay(0x2592),
                        path + " (" + font.getFontName() + ") must map U+2592 (DEC medium shade)");
            }
        }
    }
}
