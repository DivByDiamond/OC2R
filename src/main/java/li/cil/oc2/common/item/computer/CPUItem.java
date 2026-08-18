package li.cil.oc2.common.item.computer;

import li.cil.oc2.common.item.ModItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CPUItem extends ModItem {

    private final int frequency;

    public CPUItem(int frequency) {
        super();
        this.frequency = frequency;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public Component getName(final ItemStack stack) {
        return Component.literal("")
                .append(super.getName(stack))
                .append(" (")
                .append(String.valueOf(frequency / 1_000_000))
                .append(" MHz")
                                .append(")");
    }
}

