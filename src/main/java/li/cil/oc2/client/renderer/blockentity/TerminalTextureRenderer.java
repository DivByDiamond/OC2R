package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.common.vm.terminal.RendererModel;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix4f;

/**
 * Renders terminal content into a DynamicTexture once per change,
 * then displays that texture as a quad on the block face.
 * Uses NEAREST filtering for crisp text at any distance (LOD support).
 */
public class TerminalTextureRenderer implements RendererModel {
    private static final int TEX_W = TerminalTextureBuilder.TEX_W;
    private static final int TEX_H = TerminalTextureBuilder.TEX_H;

    private final DynamicTexture texture;
    private final AtomicInteger dirtyMask = new AtomicInteger(-1);
    private Terminal currentTerminal;

    public TerminalTextureRenderer() {
        texture = new DynamicTexture(TEX_W, TEX_H, true);
        texture.setFilter(false, false); // NEAREST filtering, no mipmap – crisp text
        texture.upload();
    }

    @Override
    public AtomicInteger getDirtyMask() {
        return dirtyMask;
    }

    @Override
    public void close() {
        if (currentTerminal != null) {
            currentTerminal.renderers.remove(this);
        }
        texture.close();
    }

    public void render(
            final PoseStack stack,
            final Matrix4f projection,
            final Terminal terminal,
            final boolean renderingToBlock) {
        // Track which Terminal we're rendering; re-register as RendererModel on change
        if (terminal != currentTerminal) {
            if (currentTerminal != null) currentTerminal.renderers.remove(this);
            currentTerminal = terminal;
            if (terminal != null) terminal.renderers.add(this);
            dirtyMask.set(-1); // force full rebuild
        }
        if (terminal == null) return;

        if (dirtyMask.get() != 0) {
            final NativeImage img = texture.getPixels();
            if (img != null) {
                TerminalTextureBuilder.updateTexture(img, terminal);
                texture.upload();
                dirtyMask.set(0);
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, texture.getId());

        if (GameRenderer.getPositionTexShader() == null) {
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            return;
        }

        if (renderingToBlock) {
            // Sodium/Sable compatibility: push pose onto RenderSystem model-view stack
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().mul(stack.last().pose());
            RenderSystem.applyModelViewMatrix();
        }

        final Matrix4f mv = renderingToBlock ? RenderSystem.getModelViewMatrix() : stack.last().pose();

        final BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(mv, 0, 0, 0).setUv(0, 0);
        builder.addVertex(mv, 0, TEX_H, 0).setUv(0, 1);
        builder.addVertex(mv, TEX_W, TEX_H, 0).setUv(1, 1);
        builder.addVertex(mv, TEX_W, 0, 0).setUv(1, 0);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        if (renderingToBlock) {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }
}
