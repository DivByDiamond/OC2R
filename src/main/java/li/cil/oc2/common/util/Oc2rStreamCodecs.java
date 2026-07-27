package li.cil.oc2.common.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.nio.ByteBuffer;

public class Oc2rStreamCodecs {
    public static final StreamCodec<FriendlyByteBuf, ByteBuffer> BYTE_BUFFER = new StreamCodec<FriendlyByteBuf, ByteBuffer>() {
        public ByteBuffer decode(FriendlyByteBuf buf) {
            var limit = buf.readVarInt();
            var result = ByteBuffer.allocateDirect(limit);
            buf.readBytes(result);
            result.flip();
            return result;
        }

        public void encode(FriendlyByteBuf buf, ByteBuffer data) {
            buf.writeVarInt(data.limit());
            buf.writeBytes(data);
            data.position(0);
        }
    };
}
