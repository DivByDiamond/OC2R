package li.cil.oc2.common.network.message.computer;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.misc.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import li.cil.oc2.common.network.message.misc.AbstractMessage;

public record FirmwareFlasherMessage(BlockPos pos, ItemStack data) implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, FirmwareFlasherMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    FirmwareFlasherMessage::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    FirmwareFlasherMessage::data,
                    FirmwareFlasherMessage::new);

    public static final CustomPacketPayload.Type<FirmwareFlasherMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "firmware_flasher_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public FirmwareFlasherMessage(final FlashMemoryFlasherBlockEntity diskDrive) {
        this(diskDrive.getBlockPos(), diskDrive.getFloppy());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                FlashMemoryFlasherBlockEntity.class,
                diskDrive -> diskDrive.setFlashMemory(data));
    }
}