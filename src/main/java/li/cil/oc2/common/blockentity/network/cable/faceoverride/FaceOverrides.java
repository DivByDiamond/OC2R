package li.cil.oc2.common.blockentity.network.cable.faceoverride;

import java.util.Arrays;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * The {@link FaceOverride} of every face of one cable plus its NBT encoding: one byte per face,
 * indexed by {@link Direction#get3DDataValue()}. Kept out of {@code BusCableBlockEntity} so that
 * class only wires things together.
 */
public final class FaceOverrides {
    private final FaceOverride[] overrides = new FaceOverride[Constants.BLOCK_FACE_COUNT];

    public FaceOverrides() {
        Arrays.fill(overrides, FaceOverride.AUTO);
    }

    public FaceOverride get(@Nullable final Direction side) {
        return side == null ? FaceOverride.AUTO : overrides[side.get3DDataValue()];
    }

    /**
     * Sets the override of {@code side}.
     *
     * @return whether the value actually changed.
     */
    public boolean set(final Direction side, final FaceOverride value) {
        final int index = side.get3DDataValue();
        if (overrides[index] == value) {
            return false;
        }
        overrides[index] = value;
        return true;
    }

    public void save(final CompoundTag tag, final String key) {
        final byte[] bytes = new byte[overrides.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = overrides[i].toByte();
        }
        tag.putByteArray(key, bytes);
    }

    public void load(final CompoundTag tag, final String key) {
        final byte[] bytes = tag.getByteArray(key);
        // A missing or truncated array simply falls back to AUTO for the remaining faces.
        for (int i = 0; i < overrides.length; i++) {
            overrides[i] = i < bytes.length ? FaceOverride.fromByte(bytes[i]) : FaceOverride.AUTO;
        }
    }
}
