package li.cil.oc2.network.wire;

/**
 * Loader and Minecraft independent shape math for hanging wires, kept here so it can be unit tested.
 * Cable links are validated to at most 16 blocks on both ends, so the curves only ever see that
 * range.
 */
public final class WireGeometry {
    private static final double MIN_HANG = 0.1;
    private static final double SHORT_MAX_HANG = 0.5;
    private static final double SHORT_LENGTH = 8.0;
    private static final double LONG_HANG_PER_BLOCK = 0.05;
    private static final int MIN_SEGMENTS = 8;

    private WireGeometry() {}

    /**
     * How far the middle of a wire of {@code length} blocks hangs below the straight line between its
     * ends. Up to eight blocks this matches the original look (0.1 to 0.5); longer wires keep sagging
     * at a fixed rate (at the maximum link distance of 16 blocks: 0.9).
     */
    public static double hang(final double length) {
        if (length <= SHORT_LENGTH) {
            final double factor = Math.max(0, Math.min(1, length / SHORT_LENGTH));
            return MIN_HANG + (SHORT_MAX_HANG - MIN_HANG) * factor;
        }
        return SHORT_MAX_HANG + (length - SHORT_LENGTH) * LONG_HANG_PER_BLOCK;
    }

    /** Number of segments used to draw a wire, growing with its length so long wires stay smooth. */
    public static int segments(final double length) {
        return Math.max(MIN_SEGMENTS, (int) Math.ceil(length * 1.5));
    }

    /** Half width of the drawn ribbon. */
    public static float thickness() {
        return 0.025f;
    }
}
