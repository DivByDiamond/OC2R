package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.disk.DiskDriveBlockEntity;
import li.cil.oc2.common.network.ClientBlockEntityLookup;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DiskDriveFloppyMessage(BlockPos pos, ItemStack data) implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, DiskDriveFloppyMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    DiskDriveFloppyMessage::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    DiskDriveFloppyMessage::data,
                    DiskDriveFloppyMessage::new);

    public static final CustomPacketPayload.Type<DiskDriveFloppyMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "disk_drive_floppy_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public DiskDriveFloppyMessage(final DiskDriveBlockEntity diskDrive) {
        this(diskDrive.getBlockPos(), diskDrive.getFloppy());
    }

    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos, DiskDriveBlockEntity.class, diskDrive -> diskDrive.setFloppyClient(data));
    }
}
