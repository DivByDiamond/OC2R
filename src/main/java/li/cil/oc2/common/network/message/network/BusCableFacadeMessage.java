package li.cil.oc2.common.network.message.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BusCableFacadeMessage(BlockPos pos, ItemStack stack) implements AbstractMessage {

    public static final StreamCodec<RegistryFriendlyByteBuf, BusCableFacadeMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    BusCableFacadeMessage::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    BusCableFacadeMessage::stack,
                    BusCableFacadeMessage::new);

    public static final CustomPacketPayload.Type<BusCableFacadeMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "bus_cable_facade_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos, BusCableBlockEntity.class, busCable -> busCable.setFacade(stack));
    }
}