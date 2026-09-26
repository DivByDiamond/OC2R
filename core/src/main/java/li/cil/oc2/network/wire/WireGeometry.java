package li.cil.oc2.network.wire;

/** Loader and Minecraft independent shape math for hanging wires, kept here so it can be unit tested. */
public final class WireGeometry {
    private static final double MIN_HANG = 0.1;
    private static final double SHORT_MAX_HANG = 0.5;
    private static final double SHORT_LENGTH = 8.0;
    private static final double LONG_HANG_PER_BLOCK = 0.05;
    private static final double MAX_HANG = 3.0;
    private static final int MIN_SEGMENTS = 8;
    private static final int MAX_SEGMENTS = 48;

    private WireGeometry() {}

    /**
     * How far the middle of a wire of {@code length} blocks hangs below the straight line between its
     * ends. Up to eight blocks this matches the original look (0.1 to 0.5); longer wires keep sagging
     * at a fixed rate and are capped so a 64 block optical wire does not drag on the ground.
     */
    public static double hang(final double length) {
        if (length <= SHORT_LENGTH) {
            final double factor = Math.max(0, Math.min(1, length / SHORT_LENGTH));
            return MIN_HANG + (SHORT_MAX_HANG - MIN_HANG) * factor;
        }
        return Math.min(MAX_HANG, SHORT_MAX_HANG + (length - SHORT_LENGTH) * LONG_HANG_PER_BLOCK);
    }

    /** Number of segments used to draw a wire, growing with its length so long wires stay smooth. */
    public static int segments(final double length) {
        final int wanted = (int) Math.ceil(length * 1.5);
        return Math.max(MIN_SEGMENTS, Math.min(MAX_SEGMENTS, wanted));
    }

    /** Half width of the drawn ribbon for a wire tier. */
    public static float thickness(final WireType type) {
        return switch (type) {
            case COPPER -> 0.025f;
            case GOLD -> 0.03f;
            case OPTICAL -> 0.018f;
        };
    }
}
