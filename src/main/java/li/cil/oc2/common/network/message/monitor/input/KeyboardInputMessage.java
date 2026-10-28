package li.cil.oc2.common.network.message.monitor.input;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import li.cil.oc2.common.network.util.PlayerRateLimits;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Identity-checked capture input. Codes -1/-2 release/heartbeat; 1024+axis is relative motion. */
public record KeyboardInputMessage(BlockPos pos, UUID keyboardId, BlockPos focus, int keycode, int value)
        implements AbstractMessage {
    public static final int RELEASE = -1;
    public static final int HEARTBEAT = -2;
    public static final int RELATIVE = 1024;
    public static final StreamCodec<ByteBuf, KeyboardInputMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public KeyboardInputMessage decode(final ByteBuf buffer) {
            return new KeyboardInputMessage(BlockPos.STREAM_CODEC.decode(buffer),
                    new UUID(buffer.readLong(), buffer.readLong()), BlockPos.STREAM_CODEC.decode(buffer),
                    buffer.readInt(), buffer.readInt());
        }

        @Override
        public void encode(final ByteBuf buffer, final KeyboardInputMessage message) {
            BlockPos.STREAM_CODEC.encode(buffer, message.pos);
            buffer.writeLong(message.keyboardId.getMostSignificantBits());
            buffer.writeLong(message.keyboardId.getLeastSignificantBits());
            BlockPos.STREAM_CODEC.encode(buffer, message.focus);
            buffer.writeInt(message.keycode);
            buffer.writeInt(message.value);
        }
    };

    public static final CustomPacketPayload.Type<KeyboardInputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "keyboard_input_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public KeyboardInputMessage(final KeyboardBlockEntity keyboard, final int keycode, final boolean down) {
        this(keyboard, keyboard.getTargetPos(), keycode, down ? 1 : 0);
    }

    public KeyboardInputMessage(final KeyboardBlockEntity keyboard, final BlockPos focus,
            final int keycode, final int value) {
        this(keyboard.getBlockPos(), keyboard.getInputId(), focus, keycode, value);
    }

    @Override
    public void handleMessage(final MessageContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, KeyboardBlockEntity.class,
                (player, keyboard) -> {
                    if (!keyboard.getInputId().equals(keyboardId) || !keyboard.capture.owns(player)) return;
                    if (keycode == RELEASE) {
                        keyboard.capture.release();
                        return;
                    }
                    // Releasing already-held state is cheap and must survive an exhausted rate budget.
                    // It targets the accepted receiver, never a new client-supplied focus.
                    if (keycode >= 1 && keycode <= 0x2ff && value == 0) {
                        keyboard.capture.key(keycode, false);
                        return;
                    }
                    if (!PlayerRateLimits.allowEvents(player, 256)) return;
                    final KeyboardDevice<?> target = keyboard.inputTarget(player, focus);
                    if (target == null) {
                        keyboard.capture.release();
                        return;
                    }
                    keyboard.capture.focus(target);
                    if (keycode >= RELATIVE) keyboard.capture.relative(keycode - RELATIVE, value);
                    else if (keycode >= 0 && (value == 0 || value == 1)) keyboard.capture.key(keycode, value != 0);
                });
    }
}
