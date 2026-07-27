package li.cil.oc2.common.item;

import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public abstract class AbstractBlockDeviceItem extends ModItem {
    public static final String DATA_TAG_NAME = "data";

    private final ResourceLocation defaultData;

    protected AbstractBlockDeviceItem(
            final Properties properties, final ResourceLocation defaultData) {
        super(properties.stacksTo(1));
        this.defaultData = defaultData;
    }

    protected AbstractBlockDeviceItem(final ResourceLocation defaultData) {
        this(createProperties(), defaultData);
    }

    @Nullable
    public BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(DATA_TAG_NAME);

        ResourceLocation location = defaultData;
        if (!StringUtil.isNullOrEmpty(registryName)) {
            try {
                location = ResourceLocation.parse(registryName);
            } catch (final ResourceLocationException ignored) {
            }
        }

        return BlockDeviceDataRegistry.getValue(location);
    }

    public ItemStack withData(final ItemStack stack, final BlockDeviceData data) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return ItemStack.EMPTY;
        }

        final ResourceLocation key = BlockDeviceDataRegistry.getKey(data);
        if (key == null) {
            return ItemStack.EMPTY;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                (nbt) -> {
                    ItemStackUtils.getOrCreateModDataTag(nbt)
                            .putString(DATA_TAG_NAME, key.toString());
                });

        return stack;
    }

    @SuppressWarnings("unused")
    public ItemStack withData(final BlockDeviceData data) {
        return withData(new ItemStack(this), data);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final BlockDeviceData data = getData(stack);
        if (data != null) {
            return Component.literal("")
                    .append(super.getName(stack))
                    .append(" (")
                    .append(data.getDisplayName())
                    .append(")");
        } else {
            return super.getName(stack);
        }
    }

    @SuppressWarnings("unused")
    protected ResourceLocation getDefaultData() {
        return defaultData;
    }
}