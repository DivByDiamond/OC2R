package li.cil.oc2.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/**
 * Loader-independent counterpart of NeoForge's {@code INBTSerializable}: an object that saves to
 * and loads from an NBT tag. The signatures are identical so implementations written against the
 * NeoForge interface keep working unchanged.
 *
 * @param <T> the tag type
 */
public interface NbtSerializable<T extends Tag> {
    T serializeNBT(HolderLookup.Provider provider);

    void deserializeNBT(HolderLookup.Provider provider, T nbt);
}
