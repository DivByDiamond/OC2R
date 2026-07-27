package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientCanceledImportFileMessage(int id) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ClientCanceledImportFileMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ClientCanceledImportFileMessage::id,
                    ClientCanceledImportFileMessage::new);

    public static final CustomPacketPayload.Type<ClientCanceledImportFileMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "client_canceled_import_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();
        FileImportExportCardItemDevice.cancelImport(player, id);
    }
}
