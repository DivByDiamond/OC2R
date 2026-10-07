package li.cil.oc2.fabric;

import net.minecraft.nbt.CompoundTag;

/** Implemented by {@code Entity} through {@code EntityMixin}: NBT data that is saved with the entity. */
public interface PersistentDataHolder {
    CompoundTag oc2r$getPersistentData();
}
