package li.cil.oc2.client.renderer.projector;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import li.cil.oc2.common.blockentity.projector.misc.FrameConsumer;
import net.minecraft.client.renderer.texture.DynamicTexture;

final class ProjectorDepthRenderInfo implements FrameConsumer {
    private static final int[] GAMMA_LUT = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            GAMMA_LUT[i] = (int) (Math.pow(i / 255.0, 1.0 / 2.2) * 255 + 0.5);
        }
    }

    private final DynamicTexture texture;
    private volatile boolean closed = false;

    ProjectorDepthRenderInfo(final DynamicTexture texture) {
        this.texture = texture;
    }

    DynamicTexture getTexture() {
        return texture;
    }

    public void close() {
        closed = true;
        // Schedule texture close on the render thread to avoid closing the
        // NativeImage while a queued upload is still pending in RenderSystem.
        RenderSystem.recordRenderCall(texture::close);
    }

    @Override
    public synchronized void processFrame(
            final int width, final int height, final ByteBuffer rgb565) {
        if (closed) return;

        final NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }

        rgb565.order(ByteOrder.LITTLE_ENDIAN);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                final int pixel = rgb565.getShort() & 0xFFFF;
                image.setPixelRGBA(col, row, toAbgr(pixel));
            }
        }

        texture.upload();
    }

    private static int toAbgr(final int pixel) {
        final int r5 = (pixel >>> 11) & 0x1F;
        final int g6 = (pixel >>> 5) & 0x3F;
        final int b5 = pixel & 0x1F;
        final int r = GAMMA_LUT[(r5 << 3) | (r5 >> 2)];
        final int g = GAMMA_LUT[(g6 << 2) | (g6 >> 4)];
        final int b = GAMMA_LUT[(b5 << 3) | (b5 >> 2)];
        return r | (g << 8) | (b << 16) | (0xFF << 24);
    }
}
