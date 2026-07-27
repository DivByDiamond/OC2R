package li.cil.oc2.common.util.nbt;

import java.nio.ByteBuffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class Oc2rStreamCodecs {
    public static final StreamCodec<FriendlyByteBuf, ByteBuffer> BYTE_BUFFER =
            new StreamCodec<FriendlyByteBuf, ByteBuffer>() {
                @Override
                public ByteBuffer decode(FriendlyByteBuf buf) {
                    var limit = buf.readVarInt();
                    var result = ByteBuffer.allocateDirect(limit);
                    buf.readBytes(result);
                    result.flip();
                    return result;
                }

                @Override
                public void encode(FriendlyByteBuf buf, ByteBuffer data) {
                    buf.writeVarInt(data.limit());
                    buf.writeBytes(data);
                    data.position(0);
                }
            };
}