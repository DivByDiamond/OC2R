package li.cil.oc2.common.vm.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.jetbrains.annotations.Nullable;

/**
 * Tile-based delta codec for RGB565 frames.
 *
 * <p>The frame is split into {@link #TILE_WIDTH}x{@link #TILE_HEIGHT} tiles. Only
 * tiles that changed since the previously encoded frame are transmitted. Each dirty
 * tile is stored in the cheapest applicable scheme: RLE when the tile contains runs
 * of identical pixels, zlib as a fallback for other compressible data, raw bytes
 * otherwise; a candidate replaces raw only if it is strictly smaller. The first
 * encoded frame and every resolution change produce a keyframe holding a
 * zlib-compressed full framebuffer.
 *
 * <p>Encoding is lossless: decode(encode(frame)) is bit-exact.
 *
 * <p>Wire format (all multi-byte integers are LEB128-style varints):
 * <pre>
 * byte   flags       bit 0 set for keyframes, remaining bits reserved
 * varint width       must match what the decoder passes in
 * varint height
 * </pre>
 * Followed by either:
 * <ul>
 *   <li>keyframe: a single zlib stream inflating to exactly width*height*2 bytes</li>
 *   <li>delta frame:
 *   <pre>
 *   varint dirtyTileCount
 *   dirtyTileCount times:
 *       varint tileIndex      row-major index into the tile grid
 *       byte   mode           one of MODE_RAW / MODE_ZLIB / MODE_RLE
 *       varint payloadLength
 *       byte[] payload        mode-specific compressed tile data
 *   </pre></li>
 * </ul>
 */
public final class DeltaFrameCodec {
    public static final int TILE_WIDTH = 32;
    public static final int TILE_HEIGHT = 16;

    /** Tile stored verbatim as little-endian RGB565; length must match the tile exactly. */
    private static final byte MODE_RAW = 0;
    /** Tile stored as a zlib stream inflating to exactly the tile's RGB565 size. */
    private static final byte MODE_ZLIB = 1;
    /** Tile stored as an RLE stream (see {@link #rleDecode}) expanding to the tile's RGB565 size. */
    private static final byte MODE_RLE = 2;

    private static final int FLAG_KEYFRAME = 1;

    private static final int MAX_TILE_BYTES = TILE_WIDTH * TILE_HEIGHT * 2;

    // Encoder state.
    @Nullable private byte[] previousFrame;
    private int encodedWidth = -1;
    private int encodedHeight = -1;
    private boolean forceKeyframe = true;
    private final Deflater deflater = new Deflater();

    // Decoder state.
    @Nullable private byte[] currentFrame;
    private int decodedWidth = -1;
    private int decodedHeight = -1;
    private final Inflater inflater = new Inflater();

    /**
     * Encodes an RGB565 framebuffer into a self-contained payload; the consumer
     * passes the same width/height alongside the payload when decoding.
     */
    public synchronized byte[] encode(final byte[] rgb565, final int width, final int height) {
        if (rgb565.length != width * height * 2) {
            throw new IllegalArgumentException("frame size mismatch");
        }
        if (forceKeyframe || width != encodedWidth || height != encodedHeight) {
            previousFrame = new byte[rgb565.length];
            encodedWidth = width;
            encodedHeight = height;
            forceKeyframe = true;
        }

        assert previousFrame != null;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        final int tilesX = tileCountX(width);
        final int tilesY = tileCountY(height);

        out.write(forceKeyframe ? FLAG_KEYFRAME : 0);
        writeVarint(out, width);
        writeVarint(out, height);
        if (forceKeyframe) {
            writeZlib(out, rgb565);
            forceKeyframe = false;
        } else {
            int dirtyTiles = 0;
            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    if (isTileDirty(rgb565, width, previousFrame, tx, ty)) {
                        dirtyTiles++;
                    }
                }
            }

            writeVarint(out, dirtyTiles);
            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    if (!isTileDirty(rgb565, width, previousFrame, tx, ty)) {
                        continue;
                    }
                    encodeDirtyTile(out, rgb565, width, height, tilesX, tx, ty);
                }
            }
        }

        System.arraycopy(rgb565, 0, previousFrame, 0, rgb565.length);
        return out.toByteArray();
    }

    /**
     * Decodes a payload produced by {@link #encode} into the full RGB565 framebuffer,
     * or empty if the payload is invalid or a delta arrives without its keyframe.
     */
    public synchronized Optional<byte[]> decode(
            final byte[] data, final int width, final int height) {
        try {
            return decodeInner(data, width, height);
        } catch (final IOException | DataFormatException e) {
            // The decoder state may be corrupted; the next keyframe restores it.
            currentFrame = null;
            return Optional.empty();
        }
    }

    /** Forces the next {@link #encode} call to emit a keyframe. */
    public synchronized void requestKeyframe() {
        forceKeyframe = true;
    }

    private Optional<byte[]> decodeInner(final byte[] data, final int width, final int height)
            throws IOException, DataFormatException {
        final ByteBuffer in = ByteBuffer.wrap(data);
        final boolean keyframe = (in.get() & FLAG_KEYFRAME) != 0;
        // The payload carries its dimensions so a stale/mismatched stream is
        // rejected instead of being misapplied to the wrong grid.
        if (readVarint(in) != width || readVarint(in) != height) {
            return Optional.empty();
        }

        if (width != decodedWidth || height != decodedHeight || currentFrame == null) {
            if (!keyframe) {
                return Optional.empty();
            }
            currentFrame = new byte[width * height * 2];
            decodedWidth = width;
            decodedHeight = height;
        }

        if (keyframe) {
            final byte[] full = inflateExact(in, currentFrame.length);
            if (full == null) {
                currentFrame = null;
                return Optional.empty();
            }
            System.arraycopy(full, 0, currentFrame, 0, full.length);
            return Optional.of(currentFrame.clone());
        }

        final int tilesX = tileCountX(width);
        final int maxTiles = tilesX * tileCountY(height);
        final int dirtyTiles = readVarint(in);
        if (dirtyTiles > maxTiles) {
            return Optional.empty();
        }

        // Apply on a scratch copy so a corrupt delta cannot leave a half-updated
        // frame as the base for the next one; currentFrame stays untouched.
        assert currentFrame != null;
        final byte[] working = currentFrame.clone();
        final byte[] tileBuffer = new byte[MAX_TILE_BYTES];
        for (int i = 0; i < dirtyTiles; i++) {
            final int tileIndex = readVarint(in);
            final int tx = tileIndex % tilesX;
            final int ty = tileIndex / tilesX;
            if (tileIndex >= maxTiles) {
                return Optional.empty();
            }
            final int mode = in.get() & 0xFF;
            final int length = readVarint(in);
            if (length > in.remaining()) {
                return Optional.empty();
            }
            final byte[] chunk = new byte[length];
            in.get(chunk);

            final int expectedSize =
                    Math.min(TILE_WIDTH, width - tx * TILE_WIDTH)
                            * Math.min(TILE_HEIGHT, height - ty * TILE_HEIGHT)
                            * 2;
            switch (mode) {
                case MODE_RAW -> {
                    if (length != expectedSize) {
                        return Optional.empty();
                    }
                    copyTileInto(working, width, chunk, length, tx, ty);
                }
                case MODE_ZLIB -> {
                    final byte[] inflated = inflateExact(ByteBuffer.wrap(chunk), expectedSize);
                    if (inflated == null) {
                        return Optional.empty();
                    }
                    copyTileInto(working, width, inflated, expectedSize, tx, ty);
                }
                case MODE_RLE -> {
                    final int written = rleDecode(chunk, tileBuffer, expectedSize);
                    if (written != expectedSize) {
                        return Optional.empty();
                    }
                    copyTileInto(working, width, tileBuffer, expectedSize, tx, ty);
                }
                default -> {
                    return Optional.empty();
                }
            }
        }

        currentFrame = working;
        return Optional.of(working);
    }

    private static int tileCountX(final int width) {
        return (width + TILE_WIDTH - 1) / TILE_WIDTH;
    }

    private static int tileCountY(final int height) {
        return (height + TILE_HEIGHT - 1) / TILE_HEIGHT;
    }

    private boolean isTileDirty(
            final byte[] frame, final int width, final byte[] reference, final int tx,
            final int ty) {
        final int x0 = tx * TILE_WIDTH;
        final int y0 = ty * TILE_HEIGHT;
        final int tw = Math.min(TILE_WIDTH, width - x0);
        final int th = Math.min(TILE_HEIGHT, reference.length / (width * 2) - y0);
        for (int y = y0; y < y0 + th; y++) {
            final int from = (y * width + x0) * 2;
            for (int b = 0; b < tw * 2; b++) {
                if (frame[from + b] != reference[from + b]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void encodeDirtyTile(
            final ByteArrayOutputStream out,
            final byte[] frame,
            final int width,
            final int height,
            final int tilesX,
            final int tx,
            final int ty) {
        final int tw = Math.min(TILE_WIDTH, width - tx * TILE_WIDTH);
        final int th = Math.min(TILE_HEIGHT, height - ty * TILE_HEIGHT);
        final int tileSize = tw * th * 2;
        final byte[] tile = new byte[tileSize];
        int written = 0;
        for (int y = ty * TILE_HEIGHT; y < ty * TILE_HEIGHT + th; y++) {
            final int from = (y * width + tx * TILE_WIDTH) * 2;
            System.arraycopy(frame, from, tile, written, tw * 2);
            written += tw * 2;
        }

        // zlib is only worth trying when nothing beat raw yet; each candidate must
        // be strictly smaller than the current best to be emitted.
        byte[] best = tile;
        byte bestMode = MODE_RAW;
        if (hasRuns(tile)) {
            final ByteArrayOutputStream rle = rleEncode(tile);
            if (rle.size() < best.length) {
                best = rle.toByteArray();
                bestMode = MODE_RLE;
            }
        }
        if (best.length == tile.length && tileSize > 32) {
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream(tileSize / 2);
            writeZlib(compressed, tile);
            if (compressed.size() < best.length) {
                best = compressed.toByteArray();
                bestMode = MODE_ZLIB;
            }
        }

        writeVarint(out, ty * tilesX + tx);
        out.write(bestMode);
        writeVarint(out, best.length);
        out.write(best, 0, best.length);
    }

    /** Cheap heuristic: two consecutive identical pixels mean RLE can win. */
    private static boolean hasRuns(final byte[] tile) {
        for (int i = 2; i + 1 < tile.length; i += 2) {
            if (tile[i] == tile[i - 2] && tile[i + 1] == tile[i - 1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encodes a tile as run-length sequences of identical RGB565 pixels.
     *
     * <p>RLE stream format: repeated {@code [varint runLength][pixel]} groups, where
     * the pixel is 2 bytes of little-endian RGB565 and {@code runLength} counts
     * pixels, not bytes. Groups cover the whole tile with no header or terminator,
     * so the decoded size always equals the input size.
     */
    private static ByteArrayOutputStream rleEncode(final byte[] tile) {
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
     * Decodes an RLE stream produced by {@link #rleEncode} into {@code out}.
     *
     * @return number of bytes written; callers must treat anything but the expected
     *         tile size as corruption
     * @throws IOException if a run has a non-positive length, would overflow
     *                     {@code out}, or the stream is truncated mid-pixel
     */
    private static int rleDecode(final byte[] data, final byte[] out, final int expectedSize)
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

    private static void copyTileInto(
            final byte[] frame,
            final int width,
            final byte[] tile,
            final int tileSize,
            final int tx,
            final int ty) {
        final int x0 = tx * TILE_WIDTH;
        final int y0 = ty * TILE_HEIGHT;
        final int tw = Math.min(TILE_WIDTH, width - x0);
        int read = 0;
        for (int y = y0; y < y0 + TILE_HEIGHT && read < tileSize; y++) {
            final int to = (y * width + x0) * 2;
            final int rowBytes = Math.min(tw * 2, tileSize - read);
            System.arraycopy(tile, read, frame, to, rowBytes);
            read += rowBytes;
        }
    }

    private void writeZlib(final ByteArrayOutputStream out, final byte[] input) {
        deflater.reset();
        deflater.setInput(input);
        deflater.finish();
        final byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            final int length = deflater.deflate(buffer);
            out.write(buffer, 0, length);
        }
    }

    /** Inflates the rest of {@code input}; result must be exactly {@code size} bytes. */
    @Nullable
    private byte[] inflateExact(final ByteBuffer input, final int size)
            throws DataFormatException {
        inflater.reset();
        inflater.setInput(input.array(), input.position(), input.remaining());
        final byte[] result = new byte[size];
        int written = 0;
        try {
            while (!inflater.finished()) {
                final int length = inflater.inflate(result, written, size - written);
                if (length == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                    continue;
                }
                written += length;
                if (written > size) {
                    return null;
                }
            }
        } catch (final DataFormatException e) {
            throw e;
        }
        if (written != size) {
            return null;
        }
        return result;
    }

    /**
     * Writes a LEB128-style varint: 7 payload bits per byte, little-endian order,
     * high bit set on every byte except the last.
     */
    private static void writeVarint(final ByteArrayOutputStream out, int value) {
        assert value >= 0;
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
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
