//? if <26.1 {
package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import li.cil.oc2.manual.api.prefab.renderer.BitmapFontRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ManualBitmapFontTest {
    @Test
    void atlasPaddingDoesNotChangeTheRenderedCharacterAdvance() {
        final ResourceLocation texture = ResourceLocation.parse("oc2r:test_font");
        final BitmapFontRenderer font = new BitmapFontRenderer() {
            @Override protected CharSequence getCharacters() { return "?AB"; }
            @Override protected ResourceLocation getTextureLocation() { return texture; }
            @Override protected int getResolution() { return 96; }
            @Override protected int getGapU() { return 1; }
            @Override protected int getGapV() { return 1; }
            @Override protected int charWidth() { return 6; }
            @Override public int lineHeight() { return 9; }
        };
        final GuiGraphics graphics = mock(GuiGraphics.class);

        font.draw(graphics, "AB", -1);

        assertEquals(12, font.width("AB"));
        verify(graphics).blit(texture, 6, 0, 14f, 0f, 6, 9, 96, 96);
    }
}
//?}
