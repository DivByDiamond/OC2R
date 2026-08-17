package li.cil.oc2.common.network.message.computer;

import li.cil.oc2.api.API;
import li.cil.oc2.client.audio.SoundClientManager;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SoundCardBeepMessage(BlockPos pos, float frequency, int durationMs)
        implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, SoundCardBeepMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SoundCardBeepMessage::pos,
                    ByteBufCodecs.FLOAT,
                    SoundCardBeepMessage::frequency,
                    ByteBufCodecs.INT,
                    SoundCardBeepMessage::durationMs,
                    SoundCardBeepMessage::new);

    public static final CustomPacketPayload.Type<SoundCardBeepMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "sound_card_beep_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final IPayloadContext context) {
        context.enqueueWork(() -> SoundClientManager.playTone(pos, frequency, durationMs));
    }
}