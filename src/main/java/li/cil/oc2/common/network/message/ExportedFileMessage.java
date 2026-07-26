/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.screen.FileChooserScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

public record ExportedFileMessage(String name, byte[] data) implements AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    public static final StreamCodec<ByteBuf, ExportedFileMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ExportedFileMessage::name,
        ByteBufCodecs.BYTE_ARRAY,
        ExportedFileMessage::data,
        ExportedFileMessage::new
    );

    public static final CustomPacketPayload.Type<ExportedFileMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "exported_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public void handleMessage(IPayloadContext context) {
        FileChooserScreen.openFileChooserForSave(name, path -> {
            try {
                Files.write(path, data);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        });
    }
}
