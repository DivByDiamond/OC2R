package li.cil.oc2.common.blockentity.network.cable;

/**
 * Per-face connection override stored on every bus cable (docs/CABLE-SYSTEM.md). It is part of the save
 * format from the start so the wrench tool can set it later without a data migration.
 */
public enum FaceOverride {
    /** Follow the face's connection type (default). */
    AUTO,
    /** Reserved for the wrench tool; currently behaves like {@link #AUTO}. */
    FORCED_ON,
    /** The face never connects, neither for bus traversal nor device detection. */
    FORCED_OFF;

    private static final FaceOverride[] VALUES = values();

    /** Decodes a saved byte, falling back to {@link #AUTO} for anything unknown. */
    public static FaceOverride fromByte(final byte value) {
        return value >= 0 && value < VALUES.length ? VALUES[value] : AUTO;
    }

    public byte toByte() {
        return (byte) ordinal();
    }

    public boolean blocksConnection() {
        return this == FORCED_OFF;
    }
}
