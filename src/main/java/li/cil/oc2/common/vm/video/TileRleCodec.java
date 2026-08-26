package li.cil.oc2.common.vm.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Run-length codec for a single RGB565 tile. The stream is a sequence of
 * groups: varint runLength followed by two bytes (little-endian pixel value);
 * {@code runLength} counts pixels, not bytes. Groups cover the whole tile with
 * no header or terminator, so the decoded size always equals the input size.
 */
final class TileRleCodec {
    private TileRleCodec() {}

    /** Encodes {@code tile} into an RLE stream of (varint run, pixel) groups. */
    static ByteArrayOutputStream encode(final byte[] tile) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(tile.length / 4);
        int i = 0;
        while (i < tile.length) {
            final byte b0 = tile[i];
            final byte b1 = i + 1 < tile.length ? tile[i + 1] : (byte) ~b0;
            int run = 1;
            while (i + 2 * run + 1 < tile.length
                    && tile[i + 2 * run] == b0
                    && tile[i + 2 * run + 1] == b1) {
                run++;
            }
            writeVarint(out, run);
            out.write(b0);
            out.write(b1);
            i += 2 * run;
        }
        return out;
    }

    /**
     * Decodes an RLE stream produced by {@link #encode} into {@code out}.
     *
     * @return number of bytes written; callers must treat anything but the expected
     *         tile size as corruption
     * @throws IOException if a run has a non-positive length, would overflow
     *                     {@code out}, or the stream is truncated mid-pixel
     */
    static int decode(final byte[] data, final byte[] out, final int expectedSize)
            throws IOException {
        final ByteBuffer in = ByteBuffer.wrap(data);
        int written = 0;
        while (in.hasRemaining()) {
            final int run = readVarint(in);
            if (run <= 0 || written + run * 2 > expectedSize || in.remaining() < 2) {
                throw new IOException("corrupt RLE stream");
            }
            final byte b0 = in.get();
            final byte b1 = in.get();
            for (int i = 0; i < run; i++) {
                out[written++] = b0;
                out[written++] = b1;
            }
        }
        return written;
    }

    /**
     * Writes a LEB128-style varint: 7 payload bits per byte, little-endian order,
     * high bit set on every byte except the last.
     */
    private static void writeVarint(final ByteArrayOutputStream out, final int value) {
        assert value >= 0;
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    /** Inverse of {@link #writeVarint}; rejects truncated or overlong (5+ byte) values. */
    private static int readVarint(final ByteBuffer in) throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            if (!in.hasRemaining() || shift >= 32) {
                throw new IOException("corrupt varint");
            }
            final int b = in.get() & 0xFF;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
    }
}
