/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.device;

import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.RgbToYuv420j;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.utils.DirectByteBufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public final class SimpleFramebufferDevice implements MemoryMappedDevice {
    public static final int STRIDE = 2;

    ///////////////////////////////////////////////////////////////

    private final int width, height;
    private final ByteBuffer buffer;
    private int length;
    private final BitSet dirtyLines;

    ///////////////////////////////////////////////////////////////

    public SimpleFramebufferDevice(final int width, final int height, final ByteBuffer buffer) {
        this.width = width;
        this.height = height;
        this.length = width * height * STRIDE;

        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("Buffer too small.");
        }

        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.dirtyLines = new BitSet(height / 2);
        this.dirtyLines.set(0, height / 2);
    }

    ///////////////////////////////////////////////////////////////

    public void close() {
        synchronized (buffer) {
            length = 0;
            dirtyLines.clear();
            DirectByteBufferUtils.release(buffer);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasChanges() {
        return !dirtyLines.isEmpty();
    }

    public boolean applyChanges(final Picture picture) {
        if (dirtyLines.isEmpty()) {
            return false;
        }

        synchronized (buffer) {
            convertR5G6B5ToYUV420J(buffer, width, height, picture);
        }

        return true;
    }

    public static void convertR5G6B5ToYUV420J(ByteBuffer rgbBuffer, int width, int height, Picture yuvPicture) {

        // Retrieve the YUV planes from the Picture object
        byte[][] yuvData = yuvPicture.getData();
        byte[] yPlane = yuvData[0];
        byte[] uPlane = yuvData[1];
        byte[] vPlane = yuvData[2];

        int uvWidth = width / 2;

        // Iterate through each pixel
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int index = j * width + i;

                // Extract pixel data from ByteBuffer
                short pixel = rgbBuffer.getShort(index * 2);

                final int r5 = (pixel >>> 11) & 0b11111;
                final int g6 = (pixel >>> 5) & 0b111111;
                final int b5 = pixel & 0b11111;
                final byte r = (byte) ((r5 * 255 / 0b11111) - 128);
                final byte g = (byte) ((g6 * 255 / 0b111111) - 128);
                final byte b = (byte) ((b5 * 255 / 0b11111) - 128);

                int[] yuv = new int[3];

                RgbToYuv420j.rgb2yuv(r, g, b, yuv);

                // Set Y plane
                yPlane[index] = (byte) yuv[0];

                // Set U and V planes (subsampled)
                if (j % 2 == 0 && i % 2 == 0) {
                    int uvIndex = (j / 2) * uvWidth + (i / 2);
                    uPlane[uvIndex] = (byte) yuv[1];
                    vPlane[uvIndex] = (byte) yuv[2];
                }
            }
        }
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public long load(final int offset, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            return switch (sizeLog2) {
                case 0 -> buffer.get(offset);
                case 1 -> buffer.getShort(offset);
                case 2 -> buffer.getInt(offset);
                case 3 -> buffer.getLong(offset);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return 0;
        }
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            switch (sizeLog2) {
                case 0 -> buffer.put(offset, (byte) value);
                case 1 -> buffer.putShort(offset, (short) value);
                case 2 -> buffer.putInt(offset, (int) value);
                case 3 -> buffer.putLong(offset, value);
                default -> throw new IllegalArgumentException();
            }
            setDirty(offset);
        }
    }

    ///////////////////////////////////////////////////////////////

    private void setDirty(final int offset) {
        final int pixelY = offset / (width * STRIDE);
        dirtyLines.set(pixelY / 2);
    }
}
