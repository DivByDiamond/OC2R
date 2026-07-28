package li.cil.oc2.common.network.message.monitor;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorStateMessage(BlockPos pos, boolean isMounted, boolean hasEnergy)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, MonitorStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    MonitorStateMessage::pos,
                    ByteBufCodecs.BOOL,
                    MonitorStateMessage::isMounted,
                    ByteBufCodecs.BOOL,
                    MonitorStateMessage::hasEnergy,
                    MonitorStateMessage::new);

    public static final CustomPacketPayload.Type<MonitorStateMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MonitorStateMessage(
            final MonitorBlockEntity monitor, final boolean isMounted, final boolean hasEnergy) {
        this(monitor.getBlockPos(), isMounted, hasEnergy);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                MonitorBlockEntity.class,
                monitor -> monitor.applyMonitorStateClient(isMounted, hasEnergy));
    }
}