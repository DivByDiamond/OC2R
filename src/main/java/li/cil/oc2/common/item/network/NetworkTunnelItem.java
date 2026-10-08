package li.cil.oc2.common.item.network;

import static li.cil.oc2.common.util.text.TranslationUtils.key;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import li.cil.oc2.common.container.network.NetworkTunnelContainer;
import li.cil.oc2.common.item.ModItem;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.text.TextFormatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
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

public final class NetworkTunnelItem extends ModItem {
    private static final String TUNNEL_ID_TAG_NAME = "tunnel";
    private static final String TUNNEL_ID_TEXT = key("tooltip.{mod}.network_tunnel_id");

    public NetworkTunnelItem() {
        super(createProperties().stacksTo(1));
    }

    public static Optional<UUID> getTunnelId(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        //? if >=26.1 {
        /*if (tag.read(TUNNEL_ID_TAG_NAME, net.minecraft.core.UUIDUtil.CODEC).isPresent()) {
            return Optional.of(tag.read(TUNNEL_ID_TAG_NAME, net.minecraft.core.UUIDUtil.CODEC).orElseThrow());
        *///?} else {
        if (tag.hasUUID(TUNNEL_ID_TAG_NAME)) {
            return Optional.of(tag.getUUID(TUNNEL_ID_TAG_NAME));
        //?}
        } else {
            return Optional.empty();
        }
    }

    public static void setTunnelId(final ItemStack stack, final UUID value) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                (nbt) -> {
                    //? if >=26.1 {
                    /*ItemStackUtils.getOrCreateModDataTag(nbt).store(TUNNEL_ID_TAG_NAME, net.minecraft.core.UUIDUtil.CODEC, value);
                    *///?} else {
                    ItemStackUtils.getOrCreateModDataTag(nbt).putUUID(TUNNEL_ID_TAG_NAME, value);
                    //?}
                });
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> components,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        getTunnelId(stack)
                .ifPresent(
                        id -> {
                            final String idString =
                                    StringUtil.truncateStringIfNecessary(
                                            id.toString(), 8 + 3, true);
                            final MutableComponent idComponent =
                                    TextFormatUtils.withFormat(idString, ChatFormatting.GREEN);
                            components.add(
                                    Component.translatable(TUNNEL_ID_TEXT, idComponent)
                                            .withStyle(ChatFormatting.GRAY));
                        });
    }

    @Override
    //? if >=26.1 {
    /*public InteractionResult use(
    *///?} else {
    public InteractionResultHolder<ItemStack> use(
    //?}
            final Level level, final Player player, final InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            openContainerScreen(serverPlayer, hand);
        }

        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResultHolder.sidedSuccess(
                player.getItemInHand(hand), level.isClientSide());
        //?}
    }

    private void openContainerScreen(final ServerPlayer player, final InteractionHand hand) {
        NetworkTunnelContainer.createServer(player, hand);
    }
}