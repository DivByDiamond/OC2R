package li.cil.oc2.common.network.message.file.cancel;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.screen.file.FileChooserScreen;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerCanceledImportFileMessage(int id) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ServerCanceledImportFileMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerCanceledImportFileMessage::id,
                    ServerCanceledImportFileMessage::new);

    public static final CustomPacketPayload.Type<ServerCanceledImportFileMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "server_canceled_import_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        // The server notifies the client that an import request was fulfilled (or aborted on the
        // server side), so the client should close an open file chooser for that import without
        // sending a cancellation back to the server.
        Minecraft.getInstance().tell(() -> {
            if (Minecraft.getInstance().screen instanceof FileChooserScreen screen) {
                screen.onClose();
            }
        });
    }
}