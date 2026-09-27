package li.cil.oc2.common.blockentity.network.cable.faceoverride;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Per-face connection override stored on every bus cable (docs/CABLE-SYSTEM.md). It is part of the save
 * format from the start so the wrench tool can set it later without a data migration.
 */
public enum FaceOverride {
    /** Follow the face's connection type (default). */
    AUTO((byte) 0),
    /** Reserved for the wrench tool; currently behaves like {@link #AUTO}. */
    FORCED_ON((byte) 1),
    /** The face never connects, neither for bus traversal nor device detection. */
    FORCED_OFF((byte) 2);

    // Built once from a fixed, small enum -- never mutated afterward, so a plain (immutable) Map
    // is the right choice here, not ConcurrentHashMap.
    private static final Map<Byte, FaceOverride> BY_ID =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(value -> value.id, value -> value));

    // Explicit, saved values: independent of declaration order, unlike ordinal(). Never change or
    // reuse an existing constant's id -- that would silently re-interpret it in existing worlds.
    private final byte id;

    FaceOverride(final byte id) {
        this.id = id;
    }

    /** Decodes a saved byte, falling back to {@link #AUTO} for anything unknown. */
    public static FaceOverride fromByte(final byte value) {
        return BY_ID.getOrDefault(value, AUTO);
    }

    /** Encodes this value for saves. */
    public byte toByte() {
        return id;
    }

    public boolean blocksConnection() {
        return this == FORCED_OFF;
    }
}
