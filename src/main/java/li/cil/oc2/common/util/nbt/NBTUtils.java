package li.cil.oc2.common.util.nbt;

import java.util.Optional;
import javax.annotation.Nullable;
import li.cil.oc2.platform.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class NBTUtils {
    public static <T extends Enum<T>> void putEnum(
            final CompoundTag compound, final String key, @Nullable final Enum<T> value) {
        if (value != null) {
            compound.putString(key, value.name());
        }
    }

    @Nullable
    public static <T extends Enum<T>> T getEnum(
            final CompoundTag compound, final String key, final Class<T> enumType) {
        //? if >=26.1 {
        /*if (compound.contains(key)) {
            final String name = compound.getStringOr(key, "");
        *///?} else {
        if (compound.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
            final String name = compound.getString(key);
        //?}
            try {
                return Enum.valueOf(enumType, name);
            } catch (final IllegalArgumentException ignored) {
                // fall back to legacy int ordinal
            }
        }

        //? if >=26.1 {
        /*if (compound.contains(key)) {
            final int ordinal = compound.getIntOr(key, 0);
        *///?} else {
        if (compound.contains(key, NBTTagIds.TAG_INT)) {
            final int ordinal = compound.getInt(key);
        //?}
            final T[] constants = enumType.getEnumConstants();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
        }

        return null;
    }

    public static CompoundTag getChildTag(@Nullable final ItemStack stack, final String... path) {
        if (stack == null || !stack.has(DataComponents.CUSTOM_DATA)) {
            return new CompoundTag();
        }

        return getChildTag(stack.get(DataComponents.CUSTOM_DATA), path);
    }

    public static CompoundTag getChildTag(@Nullable final CustomData nbt, final String... path) {
        if (nbt == null) {
            return new CompoundTag();
        }

        return getChildTag(nbt.copyTag(), path);
    }

    public static CompoundTag getChildTag(@Nullable final CompoundTag tag, final String... path) {
        if (tag == null) {
            return new CompoundTag();
        }

        CompoundTag childTag = tag;
        for (final String tagName : path) {
            //? if >=26.1 {
            /*if (!childTag.contains(tagName)) {
            *///?} else {
            if (!childTag.contains(tagName, NBTTagIds.TAG_COMPOUND)) {
            //?}
                return new CompoundTag();
            }
            //? if >=26.1 {
            /*childTag = childTag.getCompoundOrEmpty(tagName);
            *///?} else {
            childTag = childTag.getCompound(tagName);
            //?}
        }

        return childTag;
    }

    public static CompoundTag getOrCreateChildTag(final CompoundTag tag, final String... path) {
        CompoundTag childTag = tag;
        for (final String tagName : path) {
            //? if >=26.1 {
            /*if (!childTag.contains(tagName)) {
            *///?} else {
            if (!childTag.contains(tagName, NBTTagIds.TAG_COMPOUND)) {
            //?}
                // NOPMD: each child tag is a distinct node stored in the tree
                childTag.put(tagName, new CompoundTag()); // NOPMD allocation depends on loop iteration / per-item state
            }
            //? if >=26.1 {
            /*childTag = childTag.getCompoundOrEmpty(tagName);
            *///?} else {
            childTag = childTag.getCompound(tagName);
            //?}
        }
        return childTag;
    }

    public static CompoundTag makeInventoryTag(
            HolderLookup.Provider provider, final ItemStack... items) {
        return new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, items)).serializeNBT(provider);
    }

    /// Tries to read an older format read/writeBlockPos used to use
    public static Optional<BlockPos> readBlockPosLegacy(CompoundTag tag) {
        //? if >=26.1 {
        /*if (!tag.contains("X") || !tag.contains("Y") || !tag.contains("Z")) {
        *///?} else {
        if (!tag.contains("X", 99) || !tag.contains("Y", 99) || !tag.contains("Z", 99)) {
        //?}
            return Optional.empty();
        }
        //? if >=26.1 {
        /*return Optional.of(new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0)));
        *///?} else {
        return Optional.of(new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")));
        //?}
    }
}