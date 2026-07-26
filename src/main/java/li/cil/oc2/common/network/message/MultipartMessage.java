
package li.cil.oc2.common.network.message;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Utility wrapper message for client to server messages exceeding the regular custom payload size.
 */
public record MultipartMessage(int messageId, int multipartMessageId, byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, MultipartMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        MultipartMessage::messageId,
        ByteBufCodecs.INT,
        MultipartMessage::multipartMessageId,
        ByteBufCodecs.BYTE_ARRAY,
        MultipartMessage::data,
        MultipartMessage::new
    );

    public static final CustomPacketPayload.Type<MultipartMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "multipart_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MULTIPART_MESSAGE_SIZE = 1024 * Constants.KILOBYTE;
    private static final int MAX_PAYLOAD_SIZE = 8 * Constants.KILOBYTE;
    private static final int HEADER_SIZE =
        1 /* forge message index */ +
            4 /* message id */ +
            4 /* multipart message id */ +
            2 /* length */;


    /**
     * Cache for collecting multipart messages on the server into one big buffer again. Discard them after some
     * time to avoid malicious clients being able to grow the memory used by this cache to grow infinitely.
     */
    private static final Cache<Integer, ByteBuf> MULTIPART_MESSAGE_BUFFER_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .build();
    private static int lastAssignedMultipartMessageId;


    private static final Map<Class<? extends AbstractMessage>, Entry> ENTRY_BY_TYPE = new HashMap<>();
    private static final Int2ObjectMap<Entry> ENTRY_BY_ID = new Int2ObjectArrayMap<>();
    private static int lastAssignedId;

    public static <T extends AbstractMessage> void registerMessage(final Class<T> type, StreamCodec<? super FriendlyByteBuf, T> streamCodec) {
        if (ENTRY_BY_TYPE.containsKey(type)) {
            throw new IllegalArgumentException("Message of this type has already been registered.");
        }
        final int id = ++lastAssignedId;
        //noinspection unchecked
        final Entry entry = new Entry(id, (StreamCodec<? super FriendlyByteBuf, AbstractMessage>) streamCodec);
        ENTRY_BY_TYPE.put(type, entry);
        ENTRY_BY_ID.put(id, entry);
    }


    public static void sendToServer(final AbstractMessage message) {
        final Entry entry = ENTRY_BY_TYPE.get(message.getClass());
        if (entry == null) {
            throw new IllegalArgumentException("Trying to send multipart message of unregistered message (" + message.getClass().getName() + ").");
        }

        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        entry.streamCodec.encode(buffer, message);
        if (buffer.readableBytes() <= MAX_PAYLOAD_SIZE) {
            // Message fits into one custom payload packet, send it as is.
            Network.sendToServer(message);
            return;
        }
        if (buffer.readableBytes() > MAX_MULTIPART_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message too large.");
        }

        final int messageId = entry.id();
        final int multipartMessageId = ++lastAssignedMultipartMessageId;

        boolean lastPacketFullLength = true;

        while (buffer.readableBytes() > 0 || lastPacketFullLength) {
            int dataLength = Math.min(buffer.readableBytes(), MAX_PAYLOAD_SIZE - HEADER_SIZE);
            lastPacketFullLength = dataLength == MAX_PAYLOAD_SIZE - HEADER_SIZE;
            final byte[] data = new byte[dataLength];
            buffer.readBytes(data);
            Network.sendToServer(new MultipartMessage(messageId, multipartMessageId, data));
        }
    }


    public MultipartMessage(final int messageId, final int multipartMessageId, final byte[] data) {
        this.messageId = messageId;
        this.multipartMessageId = multipartMessageId;
        this.data = data;
    }


    public void handleMessage(IPayloadContext context) {
        try {
            final boolean isFinalPart = data.length < MAX_PAYLOAD_SIZE - HEADER_SIZE;

            final ByteBuf buffer = MULTIPART_MESSAGE_BUFFER_CACHE.get(lastAssignedMultipartMessageId, Unpooled::buffer);
            if (buffer.capacity() == 0) {
                return; // Invalidated entry due to being over-sized.
            }

            buffer.writeBytes(data);
            if (buffer.readableBytes() > MAX_MULTIPART_MESSAGE_SIZE) {
                LOGGER.error("Received over-sized multipart message from client [{}], ignoring.", context.player());
                MULTIPART_MESSAGE_BUFFER_CACHE.put(lastAssignedMultipartMessageId, Unpooled.buffer(0));
                return;
            }

            if (isFinalPart) {
                MULTIPART_MESSAGE_BUFFER_CACHE.invalidate(lastAssignedMultipartMessageId);

                final Entry entry = ENTRY_BY_ID.get(messageId);
                if (entry == null) {
                    LOGGER.error("Received multipart message for unregistered message from client [{}]. Are the mod version on the server and client the same?", context.player());
                    return;
                }

                entry.streamCodec.decode(new FriendlyByteBuf(buffer)).handleMessage(context);
            }
        } catch (final ExecutionException e) {
            LOGGER.error("Error when handling multipart message received from client [{}]: {}", context.player(), e);
        }
    }


    private record Entry(int id, StreamCodec<? super FriendlyByteBuf, AbstractMessage> streamCodec) { }
}
