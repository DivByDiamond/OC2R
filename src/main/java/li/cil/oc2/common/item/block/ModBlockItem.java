package li.cil.oc2.common.item.block;

import java.util.List;
import li.cil.oc2.common.block.BlockTooltipProvider;
import li.cil.oc2.common.util.text.TooltipUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ModBlockItem extends BlockItem {
    public ModBlockItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    public ModBlockItem(final Block block) {
        this(block, createProperties());
    }

    //? if >=26.1 {
    /*// Minecraft 26.x feeds tooltip lines to a consumer; the 1.21 list based hook below is kept for subclasses.
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag flag) {
        TooltipUtils.tryAddDescription(stack, tooltip);
        if (getBlock() instanceof final BlockTooltipProvider provider) {
            provider.appendHoverText(stack, context, tooltip, flag);
        }
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
        final List<Component> tooltip = new java.util.ArrayList<>();
        appendHoverText(stack, context, tooltip, flag);
        tooltip.forEach(adder);
    }
    *///?} else {
    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag flag) {
        TooltipUtils.tryAddDescription(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
    //?}

    //? if >=26.1 {
    /*protected static Properties createProperties() {
        return new Properties().useBlockDescriptionPrefix();
    }
    *///?} else {
    protected static Properties createProperties() {
        return new Properties();
    }
    //?}
}