package li.cil.oc2.common.network.message.monitor.framebuffer;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MonitorPowerMessageForwarded(BlockPos pos, boolean power)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, MonitorPowerMessageForwarded> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    MonitorPowerMessageForwarded::pos,
                    ByteBufCodecs.BOOL,
                    MonitorPowerMessageForwarded::power,
                    MonitorPowerMessageForwarded::new);

    public static final CustomPacketPayload.Type<MonitorPowerMessageForwarded> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "monitor_power_message_forwarded"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MonitorPowerMessageForwarded(final MonitorBlockEntity monitor, final boolean power) {
        this(monitor.getBlockPos(), power);
    }

    public void handleMessage(MessageContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                MonitorBlockEntity.class,
                (monitor) -> {
                    if (power) {
                        monitor.start();
                    } else {
                        monitor.stop();
                    }
                });
    }
}