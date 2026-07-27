package li.cil.oc2.common.util;

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.EnergyConsumingBlock;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tags.ItemTags;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class TooltipUtils {
    private static final MutableComponent DEVICE_NEEDS_REBOOT =
            Component.translatable(Constants.TOOLTIP_DEVICE_NEEDS_REBOOT)
                    .withStyle(s -> s.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)));

    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS =
            ThreadLocal.withInitial(ArrayList::new);

    public static void tryAddDescription(final ItemStack stack, final List<Component> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        final String translationKey =
                stack.getDescriptionId() + Constants.TOOLTIP_DESCRIPTION_SUFFIX;
        final Language language = Language.getInstance();
        if (language.has(translationKey)) {
            final MutableComponent description = Component.translatable(translationKey);
            tooltip.add(withFormat(description, ChatFormatting.GRAY));
        }

        if (stack.is(ItemTags.DEVICE_NEEDS_REBOOT)) {
            tooltip.add(DEVICE_NEEDS_REBOOT);
        }

        final int energyConsumption;
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof EnergyConsumingBlock energyConsumingBlock) {
            energyConsumption = energyConsumingBlock.getEnergyConsumption();
        } else {
            final ItemDeviceQuery query = Devices.makeQuery(stack);
            energyConsumption = Devices.getEnergyConsumption(query);
        }

        if (energyConsumption > 0) {
            final MutableComponent energy =
                    withFormat(String.valueOf(energyConsumption), ChatFormatting.GREEN);
            tooltip.add(
                    withFormat(
                            Component.translatable(Constants.TOOLTIP_ENERGY_CONSUMPTION, energy),
                            ChatFormatting.GRAY));
        }

        if (stack.getItem() == Items.INTERNET_GATEWAY.get()) {
            if (Config.gatewayEnergyPerPacket > 0) {
                final MutableComponent energy =
                        withFormat(
                                String.valueOf(Config.gatewayEnergyPerPacket),
                                ChatFormatting.GREEN);
                tooltip.add(
                        withFormat(
                                Component.translatable(
                                        Constants.TOOLTIP_INTERNET_ENERGY_PER_PACKET, energy),
                                ChatFormatting.GRAY));
            }
            if (!Config.internetCardEnabled) {
                tooltip.add(
                        withFormat(
                                Component.translatable(Constants.TOOLTIP_INTERNET_DISABLED),
                                ChatFormatting.RED));
            }
        }
    }

    public static void addInventoryInformation(
            final ItemStack stack, final List<Component> tooltip) {
        var container =
                stack.getOrDefault(
                        li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER,
                        new RestrictedContainer());
        addInventoryInformation(container, tooltip);
    }

    public static void addInventoryInformation(
            final RestrictedContainer container,
            final List<Component> tooltip,
            final String... subInventoryNames) {
        final List<ItemStack> itemStacks = ITEM_STACKS.get();
        itemStacks.clear();

        for (final var typed_stacks : container.items().values()) {
            for (final var x : typed_stacks) {
                if (x.getCount() == 0) continue;

                var item = x.getItem();
                var found = false;
                for (final var y : itemStacks) {
                    if (y.getItem() == item) {
                        y.setCount(y.getCount() + 1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    itemStacks.add(x.copy());
                }
            }
        }

        for (final ItemStack stack : itemStacks) {
            tooltip.add(
                    Component.literal("- ")
                            .append(stack.getDisplayName())
                            .withStyle(
                                    style ->
                                            style.withColor(
                                                    TextColor.fromLegacyFormat(
                                                            ChatFormatting.GRAY)))
                            .append(
                                    Component.literal(" x")
                                            .append(String.valueOf(stack.getCount()))
                                            .withStyle(
                                                    style ->
                                                            style.withColor(
                                                                    TextColor.fromLegacyFormat(
                                                                            ChatFormatting
                                                                                    .DARK_GRAY)))));
        }
    }

    public static void addEntityEnergyInformation(
            final ItemStack stack, final List<Component> tooltip) {
        var energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null) {
            if (energy.getEnergyStored() == 0) {
                return;
            }

            final MutableComponent value =
                    withFormat(
                            energy.getEnergyStored() + "/" + energy.getMaxEnergyStored(),
                            ChatFormatting.GREEN);
            tooltip.add(
                    withFormat(
                            Component.translatable(Constants.TOOLTIP_ENERGY, value),
                            ChatFormatting.GRAY));
        }
    }

    public static void addEnergyConsumption(final double value, final List<Component> tooltip) {
        if (value > 0) {
            tooltip.add(
                    withFormat(
                            Component.translatable(
                                    Constants.TOOLTIP_ENERGY_CONSUMPTION,
                                    withFormat(
                                            new DecimalFormat("#.##").format(value),
                                            ChatFormatting.GREEN)),
                            ChatFormatting.GRAY));
        }
    }
}
