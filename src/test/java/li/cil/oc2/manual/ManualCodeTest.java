//? if <26.1 {
package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.api.prefab.Manual;
import li.cil.oc2.manual.api.render.FontRenderer;
import li.cil.oc2.manual.client.document.DocumentRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class ManualCodeTest {
    @Test
    void codeKeepsItsLiteralCharactersThroughTheWholeParserAndRenderer() {
        final StringBuilder drawn = new StringBuilder();
        final FontRenderer font = new FontRenderer() {
            @Override public void draw(final GuiGraphics graphics, final CharSequence text, final int color) {
                drawn.append(text.toString().replaceAll("§.", ""));
            }
            @Override public int width(final CharSequence text) { return text.length() * 6; }
            @Override public int width(final Component text) { return width(text.getString()); }
            @Override public int lineHeight() { return 9; }
        };
        final ManualStyle style = new ManualStyle() {
            @Override public FontRenderer getRegularFont() { return font; }
            @Override public FontRenderer getMonospaceFont() { return font; }
        };
        final GuiGraphics graphics = mock(GuiGraphics.class);
        when(graphics.pose()).thenReturn(new PoseStack());
        final DocumentRenderer renderer = new DocumentRenderer(new Manual(), style);
        renderer.parse(new Document(List.of("`file_import_export a*bc*d`")));

        renderer.render(graphics, 0, 600, 600, -1, -1);

        assertEquals("file_import_export a*bc*d", drawn.toString());
    }
    @Test
    void fencedCodeKeepsMultilineTextAndWhitespaceLiteral() {
        final Rendered rendered = render(List.of(
                "```lua", "# not a heading", "  a_b_c *literal* [link](missing) `code`  ",
                "", "    nested()", "```", "after"), 600);
        assertEquals(List.of("# not a heading", "  a_b_c *literal* [link](missing) `code`  ",
                "    nested()", "after"), rendered.lines());
        assertEquals(List.of(0, 10, 30, 40), rendered.y());
    }

    @Test
    void longerFencesMayContainTripleBackticksAndUnclosedFencesReachTheEnd() {
        assertEquals(List.of("```", "a_b_c"), render(List.of("````text", "```", "a_b_c", "````"), 600).lines());
        assertEquals(List.of("a_b_c", "*literal*"), render(List.of("```", "a_b_c", "*literal*"), 600).lines());
    }

    @Test
    void codeWhitespaceSurvivesWrappingAndIndentedFences() {
        final Rendered wrapped = render(List.of("```", "abc    de", "```"), 24);
        assertEquals("abc    de", String.join("", wrapped.lines()));
        assertEquals(List.of("  nested()"), render(List.of("  ```lua", "    nested()", "  ```"), 600).lines());
        assertEquals(List.of("    foo bar"), render(List.of("```", "\tfoo\tbar", "```"), 600).lines());
    }

    private static Rendered render(final List<String> source, final int width) {
        final java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        final java.util.ArrayList<Integer> y = new java.util.ArrayList<>();
        final FontRenderer font = new FontRenderer() {
            @Override public void draw(final GuiGraphics graphics, final CharSequence text, final int color) {
                lines.add(text.toString().replaceAll("§.", ""));
                y.add(Math.round(graphics.pose().last().pose().m31()));
            }
            @Override public int width(final CharSequence text) { return text.length() * 6; }
            @Override public int width(final Component text) { return width(text.getString()); }
            @Override public int lineHeight() { return 9; }
        };
        final ManualStyle style = new ManualStyle() {
            @Override public FontRenderer getRegularFont() { return font; }
            @Override public FontRenderer getMonospaceFont() { return font; }
        };
        final Manual model = new Manual() {
            @Override public java.util.Optional<Document> documentFor(final String path) {
                throw new AssertionError("A literal code block must not resolve links: " + path);
            }
        };
        final GuiGraphics graphics = mock(GuiGraphics.class);
        when(graphics.pose()).thenReturn(new PoseStack());
        final DocumentRenderer renderer = new DocumentRenderer(model, style);
        renderer.parse(new Document(source));
        renderer.render(graphics, 0, width, 600, -1, -1);
        return new Rendered(lines, y);
    }

    private record Rendered(List<String> lines, List<Integer> y) {}

}
//?}
