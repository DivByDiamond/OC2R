package li.cil.oc2.common.network.message.monitor.framebuffer;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.common.network.util.frame.FrameChunker;
import li.cil.oc2.common.util.nbt.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorFramebufferMessage(
        BlockPos pos,
        int width,
        int height,
        int chunkIndex,
        int chunkCount,
        byte[] data) implements AbstractMessage {

    public static final StreamCodec<FriendlyByteBuf, MonitorFramebufferMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    MonitorFramebufferMessage::pos,
                    ByteBufCodecs.VAR_INT,
                    MonitorFramebufferMessage::width,
                    ByteBufCodecs.VAR_INT,
                    MonitorFramebufferMessage::height,
                    ByteBufCodecs.VAR_INT,
                    MonitorFramebufferMessage::chunkIndex,
                    ByteBufCodecs.VAR_INT,
                    MonitorFramebufferMessage::chunkCount,
                    Oc2rStreamCodecs.BYTE_ARRAY,
                    MonitorFramebufferMessage::data,
                    MonitorFramebufferMessage::new);

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
                        width, height, chunkIndex, chunkCount, data));
    }
}
