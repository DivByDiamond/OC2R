package li.cil.oc2.common.vm.terminal.fonts;

import li.cil.oc2.common.Main;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontHandling {
    public static final FontAtlas FontAtlas = new FontAtlas(1024, 1024, "font_atlas");
    // Regular
    public static final Font RegularFont = loadFont("/assets/oc2r/fonts/monocraft-r.ttf", 32f);
    public static final UnicodeFontRenderer regularFontRenderer = new UnicodeFontRenderer(RegularFont, false);
    // Bold
    public static final Font BoldFont = loadFont("/assets/oc2r/fonts/monocraft-b.ttf", 32f);
    public static final UnicodeFontRenderer boldFontRenderer = new UnicodeFontRenderer(BoldFont, false);
    // Italic
    public static final Font ItalicFont = loadFont("/assets/oc2r/fonts/monocraft-i.ttf", 32f);
    public static final UnicodeFontRenderer italicFontRenderer = new UnicodeFontRenderer(ItalicFont, true);
    // Bold
    public static final Font BoldItalicFont = loadFont("/assets/oc2r/fonts/monocraft-bi.ttf", 32f);
    public static final UnicodeFontRenderer boldItalicFontRenderer = new UnicodeFontRenderer(BoldItalicFont, true);

    public static Glyph getGlyph(int character, FontStyle style) {
        return switch (style) {
            case REGULAR -> regularFontRenderer.getGlyph(character);
            case ITALIC -> italicFontRenderer.getGlyph(character);
            case BOLD -> boldFontRenderer.getGlyph(character);
            case BOLD_ITALIC -> boldItalicFontRenderer.getGlyph(character);
        };
    }

    public static ResourceLocation getAtlas() {
        return FontAtlas.getTextureId();
    }

    public static Font loadFont(String path, float size) {
        try (InputStream is = Main.class.getResourceAsStream(path)) {
            if (is == null) {
                return new Font("Arial", Font.PLAIN, (int) size);
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (FontFormatException | IOException e) {
            return new Font("Arial", Font.PLAIN, (int) size); // fallback
        }
    }

    public enum FontStyle {
        REGULAR,
        ITALIC,
        BOLD,
        BOLD_ITALIC
    }
}
