package li.cil.oc2.client.renderer.blockentity.projector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

final class ProjectorLightRenderer {
    private static final int LIGHT_COLOR_NEAR = 0x22FFFFFF;
    private static final int LIGHT_COLOR_FAR = 0x00FFFFFF;
    private static final int LENS_COLOR = 0xDDFFFFFF;
    private static final int MISSING_ENERGY_COLOR = 0xEEFF6666;
    private static final int LED_COLOR = 0xCC6688DD;

    private static final float LENS_RIGHT = 0 + 4 / 16f;
    private static final float LENS_LEFT = 1 - 4 / 16f;
    private static final float LENS_BOTTOM = 0 + 4 / 16f;
    private static final float LENS_TOP = 1 - 4 / 16f;

    static void renderProjectorLight(final PoseStack stack, final MultiBufferSource bufferSource) {
        stack.translate(-0.5, 0, 0.5);
        final VertexConsumer consumer = bufferSource.getBuffer(ModRenderType.getProjectorLight());
        final Matrix4f matrix = stack.last().pose();

        final float leftFar = 1.25f;
        final float rightFar = -0.25f;
        final float topFar = 1.5f;
        final float bottomFar = 0 + 1 / 16f;

        consumer.addVertex(matrix, leftFar, topFar, 1).setColor(LIGHT_COLOR_FAR);
        consumer.addVertex(matrix, LENS_LEFT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, LENS_RIGHT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, rightFar, topFar, 1).setColor(LIGHT_COLOR_FAR);

        consumer.addVertex(matrix, leftFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR);
        consumer.addVertex(matrix, LENS_LEFT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, LENS_RIGHT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, rightFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR);

        consumer.addVertex(matrix, leftFar, topFar, 1).setColor(LIGHT_COLOR_FAR);
        consumer.addVertex(matrix, leftFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR);
        consumer.addVertex(matrix, LENS_LEFT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, LENS_LEFT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR);

        consumer.addVertex(matrix, rightFar, topFar, 1).setColor(LIGHT_COLOR_FAR);
        consumer.addVertex(matrix, LENS_RIGHT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, LENS_RIGHT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR);
        consumer.addVertex(matrix, rightFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR);

        renderLens(matrix, consumer, LENS_COLOR);
        renderLed(matrix, consumer);
    }

    static void renderMissingEnergyIndicator(
            final PoseStack stack, final MultiBufferSource bufferSource) {
        stack.translate(-0.5, 0, 0.5);
        final VertexConsumer consumer = bufferSource.getBuffer(ModRenderType.getProjectorLight());
        final Matrix4f matrix = stack.last().pose();

        renderLens(matrix, consumer, MISSING_ENERGY_COLOR);
        renderLed(matrix, consumer);
    }

    private static void renderLens(
            final Matrix4f matrix, final VertexConsumer consumer, final int color) {
        final float lensDepth = -1 / 16f;
        consumer.addVertex(matrix, LENS_RIGHT, LENS_BOTTOM, lensDepth).setColor(color);
        consumer.addVertex(matrix, LENS_LEFT, LENS_BOTTOM, lensDepth).setColor(color);
        consumer.addVertex(matrix, LENS_LEFT, LENS_TOP, lensDepth).setColor(color);
        consumer.addVertex(matrix, LENS_RIGHT, LENS_TOP, lensDepth).setColor(color);
    }

    private static void renderLed(final Matrix4f matrix, final VertexConsumer consumer) {
        final float ledRight = 0 + 7 / 16f;
        final float ledLeft = 0 + 9 / 16f;
        final float ledBottom = 0 + 3 / 16f;
        final float ledTop = 0 + 4 / 16f;
        final float ledDepth = -0.75f / 16f;

        consumer.addVertex(matrix, ledRight, ledBottom, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(matrix, ledLeft, ledBottom, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(matrix, ledLeft, ledTop, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(matrix, ledRight, ledTop, ledDepth).setColor(LED_COLOR);
    }
}