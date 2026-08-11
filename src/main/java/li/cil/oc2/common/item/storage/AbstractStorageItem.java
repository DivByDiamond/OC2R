package li.cil.oc2.common.item.storage;

import li.cil.oc2.common.item.ModItem;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import li.cil.oc2.common.util.text.TextFormatUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public abstract class AbstractStorageItem extends ModItem {
    private static final String CAPACITY_TAG_NAME = "capacity";

    private final int defaultCapacity;

    protected AbstractStorageItem(final Properties properties, final int defaultCapacity) {
        super(properties);
        this.defaultCapacity = defaultCapacity;
    }

    protected AbstractStorageItem(final int capacity) {
        this(createProperties(), capacity);
    }

    public int getCapacity(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (!tag.contains(CAPACITY_TAG_NAME, NBTTagIds.TAG_INT)) {
            return defaultCapacity;
        }

        return tag.getInt(CAPACITY_TAG_NAME);
    }

    public ItemStack withCapacity(final ItemStack stack, final int capacity) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                (nbt) -> {
                    ItemStackUtils.getOrCreateModDataTag(nbt).putInt(CAPACITY_TAG_NAME, capacity);
                });
        return stack;
    }

    public ItemStack withCapacity(final int capacity) {
        return withCapacity(new ItemStack(this), capacity);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final int capacity = getCapacity(stack);
        return Component.literal("")
                .append(super.getName(stack))
                .append(" (")
                .append(TextFormatUtils.formatSize(capacity))
                                .append(")");
    }
}

