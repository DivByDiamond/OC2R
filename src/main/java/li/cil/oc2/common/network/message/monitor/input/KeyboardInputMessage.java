package li.cil.oc2.common.network.message.monitor.input;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import li.cil.oc2.common.network.util.PlayerRateLimits;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KeyboardInputMessage(BlockPos pos, int keycode, boolean isDown)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, KeyboardInputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    KeyboardInputMessage::pos,
                    ByteBufCodecs.INT,
                    KeyboardInputMessage::keycode,
                    ByteBufCodecs.BOOL,
                    KeyboardInputMessage::isDown,
                    KeyboardInputMessage::new);

    public static final CustomPacketPayload.Type<KeyboardInputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "keyboard_input_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public KeyboardInputMessage(
            final KeyboardBlockEntity keyboard, final int keycode, final boolean isDown) {
        this(keyboard.getBlockPos(), keycode, isDown);
    }

    @Override
    public void handleMessage(MessageContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                KeyboardBlockEntity.class,
                (player, keyboard) -> {
                    // Every accepted event is injected into the VM and raises a guest
                    // interrupt; without a cap a spamming client can burn VM CPU time.
                    // Human typing plus key repeat stays well below this limit.
                    if (!PlayerRateLimits.allowEvents(player, 64)) return;
                    keyboard.handleInput(keycode, isDown);
                });
    }
}