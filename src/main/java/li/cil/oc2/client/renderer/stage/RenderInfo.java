package li.cil.oc2.client.renderer.stage;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import li.cil.oc2.common.blockentity.monitor.misc.FrameConsumer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RenderInfo implements FrameConsumer {
    private static final int[] GAMMA_LUT = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            GAMMA_LUT[i] = (int) (Math.pow(i / 255.0, 1.0 / 2.2) * 255 + 0.5);
        }
    }

    // Read on the render thread, replaced from processFrame (main thread); volatile so a
    // frame rendered between recreation steps never observes a closed texture.
    private volatile DynamicTexture texture;
    private volatile int textureWidth;
    private volatile int textureHeight;
    private volatile boolean closed = false;

    public RenderInfo(final int width, final int height) {
        this.texture = createTexture(width, height);
        this.textureWidth = width;
        this.textureHeight = height;
    }

    public DynamicTexture getTexture() {
        return texture;
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        // Schedule texture close on the render thread to avoid closing the
        // NativeImage while a queued upload is still pending in RenderSystem.
        final DynamicTexture current = texture;
        RenderSystem.recordRenderCall(current::close);
    }

    @Override
    public synchronized void processFrame(
            final int width, final int height, final ByteBuffer rgb565) {
        if (closed) return;

        if (width != textureWidth || height != textureHeight) {
            recreateTexture(width, height);
        }

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

    private void recreateTexture(final int width, final int height) {
        // GPU resolution changed: the old texture has the wrong size, swap in a new one
        // and release the old on the render thread (same reasoning as close()).
        final DynamicTexture old = texture;
        if (old != null) {
            RenderSystem.recordRenderCall(old::close);
        }
        texture = createTexture(width, height);
        textureWidth = width;
        textureHeight = height;
    }

    private static DynamicTexture createTexture(final int width, final int height) {
        final DynamicTexture created = new DynamicTexture(width, height, false);
        created.upload();
        return created;
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
