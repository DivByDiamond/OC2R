package li.cil.oc2.common.vm.terminal.fonts;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT) // §36 M5: rasterizeGlyph registers into the client-only FontAtlas texture
public class UnicodeFontRenderer {
    private static final float CAP_GLYPH_VERTICAL_SCALE = 0.82f;

    public final Font font;
    private final Map<Integer, Glyph> glyphCache = new ConcurrentHashMap<>();
    private final FontRenderContext frc = new FontRenderContext(null, true, false);
    private final boolean isItalic;

    public UnicodeFontRenderer(Font font, boolean isItalic) {
        this.font = font;
        this.isItalic = isItalic;

        String initialSet =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+-=_.,:;<>?;':\"\\|`~[]{}1234567890△▽"
                    + " ";
        int[] characters = initialSet.codePoints().toArray();
        for (final int character : characters) {
            getGlyph(character);
        }
    }

    // Sanitizes any invalid Unicode codepoints to the Unicode replacement character.
    private static int sanitizeCharacter(int character) {
        return Character.isValidCodePoint(character) ? character : 0xFFFD;
    }

    public final Glyph getGlyph(int character) {
        return glyphCache.computeIfAbsent(sanitizeCharacter(character), this::rasterizeGlyph);
    }

    static float verticalScaleFor(final int character) {
        return Character.isUpperCase(character) || Character.isDigit(character)
                ? CAP_GLYPH_VERTICAL_SCALE
                : 1f;
    }

    private Glyph rasterizeGlyph(int character) {
        GlyphVector gv = font.createGlyphVector(frc, Character.toChars(character));
        BufferedImage img =
                new BufferedImage(
                        isItalic ? 44 : 20,
                        32,
                        BufferedImage.TYPE_INT_ARGB); // size can be dynamic
        Graphics2D g = img.createGraphics();

        g.setFont(font);
        g.setColor(Color.WHITE);

        FontMetrics metrics = g.getFontMetrics();
        final float verticalScale = verticalScaleFor(character);
        final int baseline = metrics.getAscent() - 1;

        // Monocraft's cap-height is much taller than its x-height. Compress capitals and digits
        // around the shared baseline so they fit the 16px terminal row without changing their
        // monospaced horizontal advance or shifting the following glyphs.
        if (verticalScale != 1f) {
            g.scale(1, verticalScale);
        }
        g.drawGlyphVector(gv, 0, baseline / verticalScale);
        g.dispose();

        Glyph glyph = new Glyph(img);

        FontHandling.FontAtlas.addGlyph(glyph);
        return glyph;
    }
}
