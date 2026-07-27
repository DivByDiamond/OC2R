package li.cil.oc2.common.vm.terminal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import li.cil.oc2.common.vm.terminal.fonts.FontHandling;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.concurrent.atomic.AtomicInteger;

@OnlyIn(Dist.CLIENT)
public class TerminalRenderer implements RendererModel, RendererView {
    private static final Logger RENDERER_LOGGER = LogManager.getLogger();

    public final Terminal terminal;
    public final VertexBuffer[] lines = new VertexBuffer[Terminal.HEIGHT];
    public final AtomicInteger dirty = new AtomicInteger(-1);

    public TerminalRenderer(final Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void render(
            final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
        if (terminal.currentPrivateModeState.APPLICATION_SYNC) return;
        validateLineCache();
        renderBuffer(stack, projectionMatrix, renderingToBlock);

        boolean steady =
                switch (terminal.cursorMode) {
                    case TerminalColors.CursorMode.STEADY_BLOCK,
                                    TerminalColors.CursorMode.STEADY_UNDERLINE,
                                    TerminalColors.CursorMode.STEADY_BAR_LINE ->
                            true;
                    default -> false;
                };

        if (steady || (System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500) {
            TerminalCursorRenderer.renderCursor(terminal, stack);
        }
    }

    @Override
    public AtomicInteger getDirtyMask() {
        return dirty;
    }

    @Override
    public void close() {
        for (int i = 0; i < lines.length; i++) {
            final VertexBuffer line = lines[i];
            if (line != null) {
                line.close();
                lines[i] = null;
            }
        }
    }

    public int findLineIndex(VertexBuffer[] vba, VertexBuffer vb) {
        int i = 0;
        while (i < vba.length) {
            if (vba[i] == vb) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void renderBuffer(
            final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
        final ShaderInstance shader = GameRenderer.getPositionTexColorShader();
        if (shader == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, FontHandling.getAtlas());

        if (renderingToBlock) {
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().mul(stack.last().pose());
            RenderSystem.applyModelViewMatrix();

            final Matrix4f blockModelViewMatrix = RenderSystem.getModelViewMatrix();
            final Matrix4f blockProjectionMatrix = RenderSystem.getProjectionMatrix();

            for (final VertexBuffer line : lines) {
                if (line != null && !line.isInvalid()) {
                    try {
                        line.bind();
                        line.drawWithShader(blockModelViewMatrix, blockProjectionMatrix, shader);
                        VertexBuffer.unbind();
                    } catch (final Exception e) {
                        RENDERER_LOGGER.error(
                                "Failed to draw terminal line {}", findLineIndex(lines, line), e);
                    }
                }
            }

            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        } else {
            var modelViewMatrix = stack.last().pose();

            for (final VertexBuffer line : lines) {
                if (line != null && !line.isInvalid()) {
                    try {
                        line.bind();
                        line.drawWithShader(modelViewMatrix, projectionMatrix, shader);
                        VertexBuffer.unbind();
                    } catch (final Exception e) {
                        RENDERER_LOGGER.error(
                                "Failed to draw terminal line {}", findLineIndex(lines, line), e);
                    }
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }

    @SuppressWarnings("resource")
    public void validateLineCache() {
        if (dirty.get() == 0) return;

        final int mask = dirty.getAndSet(0);
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1 << row)) == 0) continue;

            BufferBuilder builder =
                    Tesselator.getInstance()
                            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            final Matrix4f matrix = new Matrix4f().translate(0, row * Terminal.CHAR_HEIGHT, 0);

            TerminalBackgroundRenderer.renderBackground(terminal, matrix, builder, row);
            TerminalCharRenderer.renderForeground(terminal, matrix, builder, row);

            MeshData rb = builder.build();

            if (rb != null) {
                if (lines[row] == null) {
                    lines[row] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                } else {
                    lines[row].close();
                    lines[row] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                }

                if (!lines[row].isInvalid()) {
                    lines[row].bind();
                    lines[row].upload(rb);
                    VertexBuffer.unbind();
                }
            } else if (lines[row] != null) {
                lines[row].close();
                lines[row] = null;
            }
        }
    }
}
