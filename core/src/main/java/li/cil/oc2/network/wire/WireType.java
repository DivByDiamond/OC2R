package li.cil.oc2.network.wire;

import java.util.Optional;

/**
 * Wire tiers for network connector links. A link uses the shorter range of its wire; throughput scales
 * with the wire's bandwidth factor. Only the type id is ever stored; every derived value is looked up here.
 */
public enum WireType {
    COPPER("copper", 16, 1),
    GOLD("gold", 32, 2),
    OPTICAL("optical", 64, 4);

    private static final WireType[] VALUES = values();

    private final String id;
    private final int maxRange;
    private final int bandwidthFactor;

    WireType(final String id, final int maxRange, final int bandwidthFactor) {
        this.id = id;
        this.maxRange = maxRange;
        this.bandwidthFactor = bandwidthFactor;
    }

    /** Stable id used in saved data and item names. */
    public String id() {
        return id;
    }

    /** Longest link, in blocks, this wire supports. */
    public int maxRange() {
        return maxRange;
    }

    /** Multiplier applied to the base per-tick byte budget. */
    public int bandwidthFactor() {
        return bandwidthFactor;
    }

    public static Optional<WireType> fromId(final String id) {
        for (final WireType type : VALUES) {
            if (type.id.equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Saved data without a wire type predates wire tiers and is copper. */
    public static WireType fromIdOrDefault(final String id) {
        return fromId(id).orElse(COPPER);
    }

    /** The limiting range for a link between two wires (currently a link has one wire; kept for clarity). */
    public static int linkRange(final WireType type, final int configuredMaxRange) {
        return Math.min(type.maxRange, Math.max(1, configuredMaxRange));
    }
}
