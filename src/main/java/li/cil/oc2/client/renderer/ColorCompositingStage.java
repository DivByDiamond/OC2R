package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

final class ColorCompositingStage {
    static void renderProjectorColors(
            final Minecraft minecraft,
            final Matrix4f pose,
            final Matrix4f projectionMatrix,
            final int projectorCount) {
        var modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
        modelViewMatrix.mul(pose);

        prepareColorBufferRendering();
        try {
            prepareOrthographicRendering(minecraft);

            RenderSystem.setShader(ModShaders::getProjectorsShader);
            ModShaders.configureProjectorsShader(
                    ProjectorDepthRenderer.MAIN_CAMERA_DEPTH,
                    constructInverseMainCameraMatrix(modelViewMatrix, projectionMatrix),
                    ProjectorDepthRenderer.PROJECTOR_COLOR_TARGETS,
                    ProjectorDepthRenderer.PROJECTOR_DEPTH_TARGETS,
                    ProjectorDepthRenderer.PROJECTOR_CAMERA_MATRICES,
                    projectorCount);

            renderIntoScreenRect();
        } finally {
            finishColorBufferRendering();
        }
    }

    private static void prepareColorBufferRendering() {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.getModelViewStack().pushMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void finishColorBufferRendering() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableBlend();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void prepareOrthographicRendering(final Minecraft minecraft) {
        final Matrix4f screenProjectionMatrix =
                new Matrix4f()
                        .setOrtho(
                                0f,
                                minecraft.getWindow().getWidth(),
                                minecraft.getWindow().getHeight(),
                                0,
                                1000,
                                3000);

        RenderSystem.setProjectionMatrix(screenProjectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

        final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.identity();
        modelViewStack.translate(0, 0, -2000);
        RenderSystem.applyModelViewMatrix();
    }

    private static Matrix4f constructInverseMainCameraMatrix(
            final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix) {
        final Matrix4f inverseModelViewMatrix = new Matrix4f(projectionMatrix);
        inverseModelViewMatrix.mul(modelViewMatrix);
        inverseModelViewMatrix.invert();
        return inverseModelViewMatrix;
    }

    private static void renderIntoScreenRect() {
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder builder =
                tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.addVertex(0, 0, 0).setUv(0, 1);
        builder.addVertex(0, ProjectorDepthRenderer.MAIN_CAMERA_DEPTH.height, 0).setUv(0, 0);
        builder.addVertex(
                        ProjectorDepthRenderer.MAIN_CAMERA_DEPTH.width,
                        ProjectorDepthRenderer.MAIN_CAMERA_DEPTH.height,
                        0)
                .setUv(1, 0);
        builder.addVertex(ProjectorDepthRenderer.MAIN_CAMERA_DEPTH.width, 0, 0).setUv(1, 1);

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
}