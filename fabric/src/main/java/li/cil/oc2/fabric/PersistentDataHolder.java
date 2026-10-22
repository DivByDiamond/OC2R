package li.cil.oc2.fabric;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;

/** Implemented by {@code Entity} through {@code EntityMixin}: NBT data that is saved with the entity. */
public interface PersistentDataHolder {
    /** Never null: the implementation creates the tag on first access, like NeoForge does. */
    @Nonnull
    CompoundTag oc2r$getPersistentData();
}
