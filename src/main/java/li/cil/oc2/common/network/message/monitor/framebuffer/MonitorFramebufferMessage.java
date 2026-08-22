package li.cil.oc2.common.network.message.monitor.framebuffer;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.common.util.nbt.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorFramebufferMessage(
        BlockPos pos,
        int codec,
        int width,
        int height,
        int frameSize,
        int chunkIndex,
        int chunkCount,
        byte[] data) implements AbstractMessage {

    public static final StreamCodec<FriendlyByteBuf, MonitorFramebufferMessage> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public MonitorFramebufferMessage decode(final FriendlyByteBuf buf) {
                    return new MonitorFramebufferMessage(
                            BlockPos.STREAM_CODEC.decode(buf),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            Oc2rStreamCodecs.BYTE_ARRAY.decode(buf));
                }

                @Override
                public void encode(
                        final FriendlyByteBuf buf, final MonitorFramebufferMessage message) {
                    BlockPos.STREAM_CODEC.encode(buf, message.pos());
                    buf.writeVarInt(message.codec());
                    buf.writeVarInt(message.width());
                    buf.writeVarInt(message.height());
                    buf.writeVarInt(message.frameSize());
                    buf.writeVarInt(message.chunkIndex());
                    buf.writeVarInt(message.chunkCount());
                    Oc2rStreamCodecs.BYTE_ARRAY.encode(buf, message.data());
                }
            };

    public static final CustomPacketPayload.Type<MonitorFramebufferMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "monitor_framebuffer_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos, MonitorBlockEntity.class, monitor -> monitor.video.applyChunk(
                        codec, width, height, frameSize, chunkIndex, chunkCount, data));
    }
}
