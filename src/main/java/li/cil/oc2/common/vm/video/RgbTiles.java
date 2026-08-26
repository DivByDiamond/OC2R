package li.cil.oc2.common.vm.video;

/**
 * Pure helpers for working with RGB565 framebuffer grids split into fixed-size
 * tiles. Shared by {@link DeltaFrameCodec}'s encoder and decoder halves.
 */
final class RgbTiles {
    private RgbTiles() {}

    /** Number of complete or partial tiles spanning the given pixel width. */
    static int tileCountX(final int width) {
        return (width + DeltaFrameCodec.TILE_WIDTH - 1) / DeltaFrameCodec.TILE_WIDTH;
    }

    /** Number of complete or partial tiles spanning the given pixel height. */
    static int tileCountY(final int height) {
        return (height + DeltaFrameCodec.TILE_HEIGHT - 1) / DeltaFrameCodec.TILE_HEIGHT;
    }

    /**
     * Compares one tile of {@code frame} against {@code reference}.
     * Edge tiles are clipped to their actual extent.
     */
    static boolean isTileDirty(
            final byte[] frame,
            final int width,
            final byte[] reference,
            final int tx,
            final int ty,
            final int referenceRows) {
        final int x0 = tx * DeltaFrameCodec.TILE_WIDTH;
        final int y0 = ty * DeltaFrameCodec.TILE_HEIGHT;
        final int tw = Math.min(DeltaFrameCodec.TILE_WIDTH, width - x0);
        final int th =
                Math.min(DeltaFrameCodec.TILE_HEIGHT, referenceRows - y0);
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

    /**
     * Copies a decoded tile ({@code tileSize} bytes) back into a full framebuffer
     * row by row; edge tiles only overwrite their clipped extent.
     */
    static void copyTileInto(
            final byte[] frame,
            final int width,
            final byte[] tile,
            final int tileSize,
            final int tx,
            final int ty) {
        final int x0 = tx * DeltaFrameCodec.TILE_WIDTH;
        final int y0 = ty * DeltaFrameCodec.TILE_HEIGHT;
        final int tw = Math.min(DeltaFrameCodec.TILE_WIDTH, width - x0);
        int read = 0;
        for (
                int y = y0;
                y < y0 + DeltaFrameCodec.TILE_HEIGHT && read < tileSize;
                y++) {
            final int to = (y * width + x0) * 2;
            final int rowBytes = Math.min(tw * 2, tileSize - read);
            System.arraycopy(tile, read, frame, to, rowBytes);
            read += rowBytes;
        }
    }
}
