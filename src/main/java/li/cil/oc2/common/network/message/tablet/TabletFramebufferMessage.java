package li.cil.oc2.common.network.message.tablet;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.tablet.TabletClientView;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A bounded lossless framebuffer chunk, sent only to authenticated open tablet menus. */
public record TabletFramebufferMessage(UUID tabletId, int frameSize, int index, int count, byte[] data)
        implements AbstractMessage {
    public static final int MAX_FRAME_SIZE = 128 * 1024;
    public static final int MAX_CHUNK_SIZE = 24 * 1024;

    public TabletFramebufferMessage { data = data.clone(); }

    @Override public byte[] data() { return data.clone(); }

    public static final StreamCodec<ByteBuf, TabletFramebufferMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TabletFramebufferMessage decode(final ByteBuf buffer) {
            return new TabletFramebufferMessage(UUIDUtil.STREAM_CODEC.decode(buffer),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    ByteBufCodecs.byteArray(MAX_CHUNK_SIZE).decode(buffer));
        }

        @Override
        public void encode(final ByteBuf buffer, final TabletFramebufferMessage message) {
            UUIDUtil.STREAM_CODEC.encode(buffer, message.tabletId());
            buffer.writeInt(message.frameSize()).writeInt(message.index()).writeInt(message.count());
            ByteBufCodecs.byteArray(MAX_CHUNK_SIZE).encode(buffer, message.data);
        }
    };
    public static final CustomPacketPayload.Type<TabletFramebufferMessage> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "tablet_framebuffer"));

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    @Override public void handleMessage(final MessageContext context) {
        TabletClientView.find(tabletId).ifPresent(view -> view.acceptFrame(frameSize, index, count, data));
    }
}
