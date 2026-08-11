package li.cil.oc2.common.item.computer;

import li.cil.oc2.common.item.ModItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class GPUItem extends ModItem {
    private final int width;
    private final int height;
    private final int tier;

    public GPUItem(final int width, final int height, final int tier) {
        this.width = width;
        this.height = height;
        this.tier = tier;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public Component getName(final ItemStack stack) {
        return Component.literal("")
                .append(super.getName(stack))
                .append(" (")
                .append(String.valueOf(width))
                .append("x")
                .append(String.valueOf(height))
                .append(")");
    }
}