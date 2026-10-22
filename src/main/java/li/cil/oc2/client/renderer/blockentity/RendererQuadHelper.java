package li.cil.oc2.client.renderer.blockentity;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class RendererQuadHelper {
    private static final int TEXT_COLOR = 0xFF000000 | 15610658;
    private static final int FULL_BRIGHT = 15728880;

    public static void drawText(
            final SubmitNodeCollector collector,
            final PoseStack stack,
            final Component text,
            final Font font) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final List<FormattedText> wrappedText =
                font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = font.width(text);
            draw(collector, stack, text.getVisualOrderText(), (maxWidth - textWidth) * 0.5f, 0);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                draw(
                        collector,
                        stack,
                        Component.literal(wrappedText.get(i).getString()).getVisualOrderText(),
                        0,
                        i * font.lineHeight);
            }
        }

        stack.popPose();
    }

    private static void draw(
            final SubmitNodeCollector collector,
            final PoseStack stack,
            final FormattedCharSequence text,
            final float x,
            final float y) {
        collector.submitText(
                stack, x, y, text, false, Font.DisplayMode.NORMAL, FULL_BRIGHT, TEXT_COLOR, 0, 0);
    }
}
*///?} else {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;

public final class RendererQuadHelper {
    public static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        consumer.addVertex(matrix, 0, 0, 0).setUv(0, 0);

        consumer.addVertex(matrix, 0, 16, 0).setUv(0, 1);

        consumer.addVertex(matrix, 16, 16, 0).setUv(1, 1);

        consumer.addVertex(matrix, 16, 0, 0).setUv(1, 0);
    }

    public static void drawText(
            final MultiBufferSource bufferSource,
            final PoseStack stack,
            final Component text,
            final Font font) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final List<FormattedText> wrappedText =
                font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = font.width(text);
            draw(bufferSource, font, stack, text, (maxWidth - textWidth) * 0.5f);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                draw(
                        bufferSource,
                        font,
                        stack,
                        wrappedText.get(i).getString(),
                        i * font.lineHeight);
            }
        }

        stack.popPose();
    }

    private static void draw(
            final MultiBufferSource bufferSource,
            final Font font,
            final PoseStack stack,
            final Component text,
            final float x) {
        font.drawInBatch(
                text,
                x,
                (float) 0,
                15610658,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880);
    }

    private static void draw(
            final MultiBufferSource bufferSource,
            final Font font,
            final PoseStack stack,
            final String text,
            final float y) {
        font.drawInBatch(
                text,
                (float) 0,
                y,
                15610658,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880,
                false);
    }
}
//?}
