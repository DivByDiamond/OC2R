package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.api.prefab.Manual;
import li.cil.oc2.manual.api.render.FontRenderer;
import li.cil.oc2.manual.client.document.DocumentRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class ManualLayoutTest {
    private static final FontRenderer FONT = new FontRenderer() {
        @Override public void draw(final GuiGraphics graphics, final CharSequence text, final int argb) { throw new AssertionError("Layout test unexpectedly rendered text"); }
        @Override public int width(final CharSequence text) { return text.toString().replaceAll("§.", "").length() * 6; }
        @Override public int width(final Component text) { return width(text.getString()); }
        @Override public int lineHeight() { return 9; }
    };
    private static final ManualStyle STYLE = new ManualStyle() {
        @Override public FontRenderer getRegularFont() { return FONT; }
        @Override public FontRenderer getMonospaceFont() { return FONT; }
    };

    @Test
    void wrappingAndHeaderScalingArePreserved() {
        final DocumentRenderer text = renderer("A long paragraph with **bold** and `code`.");
        assertTrue(text.height(60) > text.height(600));
        assertTrue(renderer("# Heading").height(600) > renderer("Heading").height(600));
    }

    @Test
    void fencedCodeCountsBlankLinesAndWrapsOnEveryVersion() {
        final DocumentRenderer fenced = renderer(List.of("```lua", "first", "", "third", "```"));
        assertEquals(3 * renderer("first").height(600), fenced.height(600));
        final DocumentRenderer longCode = renderer(List.of("```", "a_b_c *literal*", "```"));
        assertTrue(longCode.height(24) > longCode.height(600));
    }

    @Test
    void anOversizedGlyphAndIndentedListStillMakeProgress() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertTrue(renderer("W").height(1) > 0);
            assertTrue(renderer("- Wide list").height(1) > 0);
        });
    }

    private static DocumentRenderer renderer(final String line) {
        return renderer(List.of(line));
    }

    private static DocumentRenderer renderer(final List<String> lines) {
        final Manual model = new Manual() {
            @Override public Optional<Document> documentFor(final String path) { return Optional.empty(); }
        };
        final DocumentRenderer renderer = new DocumentRenderer(model, STYLE);
        renderer.parse(new Document(lines));
        return renderer;
    }
}
