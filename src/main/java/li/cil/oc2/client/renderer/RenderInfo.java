package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import li.cil.oc2.common.blockentity.monitor.FrameConsumer;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
record RenderInfo(DynamicTexture texture) implements FrameConsumer {
    private static final ThreadLocal<byte[]> RGB = ThreadLocal.withInitial(() -> new byte[3]);

    public synchronized void close() {
        texture.close();
    }

    @Override
    public synchronized void processFrame(final Picture picture) {
        final NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }

        final byte[] y = picture.getPlaneData(0);
        final byte[] u = picture.getPlaneData(1);
        final byte[] v = picture.getPlaneData(2);

        int lumaIndex = 0, chromaIndex = 0;
        for (int halfRow = 0;
                halfRow < MonitorDevice.HEIGHT / 2;
                halfRow++, lumaIndex += MonitorDevice.WIDTH * 2) {
            final int row = halfRow * 2;
            for (int halfCol = 0; halfCol < MonitorDevice.WIDTH / 2; halfCol++, chromaIndex++) {
                final int col = halfCol * 2;
                final int yIndex = lumaIndex + col;
                final byte cb = u[chromaIndex];
                final byte cr = v[chromaIndex];
                setFromYUV420(image, col, row, y[yIndex], cb, cr);
                setFromYUV420(image, col + 1, row, y[yIndex + 1], cb, cr);
                setFromYUV420(image, col, row + 1, y[yIndex + MonitorDevice.WIDTH], cb, cr);
                setFromYUV420(image, col + 1, row + 1, y[yIndex + MonitorDevice.WIDTH + 1], cb, cr);
            }
        }

        texture.upload();
    }

    private static void setFromYUV420(
            final NativeImage image,
            final int col,
            final int row,
            final byte y,
            final byte cb,
            final byte cr) {
        final byte[] bytes = RGB.get();
        Yuv420jToRgb.YUVJtoRGB(y, cb, cr, bytes, 0);
        final int r = bytes[0] + 128;
        final int g = bytes[1] + 128;
        final int b = bytes[2] + 128;
        image.setPixelRGBA(col, row, r | (g << 8) | (b << 16) | (0xFF << 24));
    }
}