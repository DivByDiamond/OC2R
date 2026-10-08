package li.cil.oc2.common.item;

import java.util.List;
import li.cil.oc2.common.util.text.TooltipUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ModItem extends Item {
    public ModItem(final Properties properties) {
        super(properties);
    }

    public ModItem() {
        this(createProperties());
    }

    //? if >=26.1 {
    /*// Minecraft 26.x feeds tooltip lines to a consumer; the 1.21 list based hook below is kept for subclasses.
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> components,
            final TooltipFlag flag) {
        TooltipUtils.tryAddDescription(stack, components);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public final void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final net.minecraft.world.item.component.TooltipDisplay display,
            final java.util.function.Consumer<Component> adder,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        final List<Component> components = new java.util.ArrayList<>();
        appendHoverText(stack, context, components, flag);
        components.forEach(adder);
    }
    *///?} else {
    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> components,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        TooltipUtils.tryAddDescription(stack, components);
    }
    //?}

    protected static Properties createProperties() {
        return new Properties();
    }
}