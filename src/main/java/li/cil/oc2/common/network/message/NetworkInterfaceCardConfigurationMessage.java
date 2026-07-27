package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NetworkInterfaceCardConfigurationMessage(
        InteractionHand hand, Direction side, boolean value) implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, NetworkInterfaceCardConfigurationMessage>
            STREAM_CODEC =
                    StreamCodec.composite(
                            NeoForgeStreamCodecs.enumCodec(InteractionHand.class),
                            NetworkInterfaceCardConfigurationMessage::hand,
                            Direction.STREAM_CODEC,
                            NetworkInterfaceCardConfigurationMessage::side,
                            ByteBufCodecs.BOOL,
                            NetworkInterfaceCardConfigurationMessage::value,
                            NetworkInterfaceCardConfigurationMessage::new);

    public static final CustomPacketPayload.Type<NetworkInterfaceCardConfigurationMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "network_interface_card_configuration_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();

        final ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.NETWORK_INTERFACE_CARD.get())) {
            return;
        }

        NetworkInterfaceCardItem.setSideConfiguration(itemStack, side, value);
    }
}