package li.cil.oc2.client.renderer;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;

final class DepthOnlyRenderTarget extends TextureTarget {
    public DepthOnlyRenderTarget(final int width, final int height) {
        super(width, height, true, Minecraft.ON_OSX);
    }

    @Override
    public void createBuffers(final int width, final int height, final boolean isOnOSX) {
        super.createBuffers(width, height, isOnOSX);
        if (colorTextureId > -1) {
            if (frameBufferId > -1) {
                glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
                glDrawBuffer(GL_NONE);
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
            }
            TextureUtil.releaseTextureId(this.colorTextureId);
            this.colorTextureId = -1;
        }
    }
}