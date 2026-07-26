
package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public record ServerCanceledImportFileMessage(int id) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ServerCanceledImportFileMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        ServerCanceledImportFileMessage::id,
        ServerCanceledImportFileMessage::new
    );

    public static final CustomPacketPayload.Type<ServerCanceledImportFileMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "server_canceled_import_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();
        FileImportExportCardItemDevice.cancelImport(player, id);
    }
}
