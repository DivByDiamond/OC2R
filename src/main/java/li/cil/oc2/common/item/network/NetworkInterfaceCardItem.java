package li.cil.oc2.common.item.network;

import static li.cil.oc2.common.util.text.TextFormatUtils.withFormat;
import static li.cil.oc2.common.util.text.TranslationUtils.text;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.ModItem;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
//? if >=26.1 {
/*import net.minecraft.world.InteractionResult;
*///?} else {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class NetworkInterfaceCardItem extends ModItem {
    private static final String SIDE_CONFIGURATION_TAG_NAME = "sides";
    private static final Component IS_CONFIGURED_TEXT =
            withFormat(
                    text("item.{mod}.network_interface_card.is_configured"), ChatFormatting.GREEN);

    public static void setSideConfiguration(
            final ItemStack stack, final Direction side, final boolean enabled) {
        final int index = side.get3DDataValue();

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                (nbt) -> {
                    final CompoundTag tag = ItemStackUtils.getOrCreateModDataTag(nbt);
                    final byte[] values;
                    //? if >=26.1 {
                    /*if (tag.contains(SIDE_CONFIGURATION_TAG_NAME)
                            && tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME).orElse(new byte[0]).length
                    *///?} else {
                    if (tag.contains(SIDE_CONFIGURATION_TAG_NAME, NBTTagIds.TAG_BYTE_ARRAY)
                            && tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME).length
                    //?}
                                    == Constants.BLOCK_FACE_COUNT) {
                        //? if >=26.1 {
                        /*values = tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME).orElse(new byte[0]);
                        *///?} else {
                        values = tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME);
                        //?}
                    } else {
                        values = new byte[Constants.BLOCK_FACE_COUNT];
                        Arrays.fill(values, (byte) 1);
                    }

                    values[index] = (byte) (enabled ? 1 : 0);

                    tag.putByteArray(SIDE_CONFIGURATION_TAG_NAME, values);
                });
    }

    public static boolean getSideConfiguration(
            final ItemStack stack, @Nullable final Direction side) {
        if (side == null) {
            return false;
        }

        final int index = side.get3DDataValue();

        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        //? if >=26.1 {
        /*if (tag.contains(SIDE_CONFIGURATION_TAG_NAME)) {
            final byte[] values = tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME).orElse(new byte[0]);
        *///?} else {
        if (tag.contains(SIDE_CONFIGURATION_TAG_NAME, NBTTagIds.TAG_BYTE_ARRAY)) {
            final byte[] values = tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME);
        //?}
            if (index < values.length) {
                return values[index] != 0;
            }
        }

        return true;
    }

    public static boolean hasConfiguration(final ItemStack stack) {
        final byte[] values =
                //? if >=26.1 {
                /*ItemStackUtils.getModDataTag(stack).getByteArray(SIDE_CONFIGURATION_TAG_NAME).orElse(new byte[0]);
                *///?} else {
                ItemStackUtils.getModDataTag(stack).getByteArray(SIDE_CONFIGURATION_TAG_NAME);
                //?}
        for (final byte value : values) {
            if (value == 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> components,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        if (NetworkInterfaceCardItem.hasConfiguration(stack)) {
            components.add(IS_CONFIGURED_TEXT);
        }
    }

    @Override
    //? if >=26.1 {
    /*public InteractionResult use(
    *///?} else {
    public InteractionResultHolder<ItemStack> use(
    //?}
            final Level level, final Player player, final InteractionHand hand) {
        final ItemStack itemStack = player.getItemInHand(hand);

        if (player.level().isClientSide()
                && itemStack.is(Items.NETWORK_INTERFACE_CARD.get())) {
            openConfigurationScreen(player, hand);
        }

        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResultHolder.sidedSuccess(itemStack, player.level().isClientSide());
        //?}
    }

    private void openConfigurationScreen(final Player player, final InteractionHand hand) {
        ClientProxy.get().openNetworkInterfaceCardScreen(player, hand);
    }
}