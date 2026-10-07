package li.cil.oc2.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Loader-independent stream codecs. The wire format of {@link #enumCodec} matches the one of
 * NeoForge's {@code NeoForgeStreamCodecs.enumCodec}, a var-int ordinal.
 */
public final class StreamCodecs {
    public static <B extends FriendlyByteBuf, V extends Enum<V>> StreamCodec<B, V> enumCodec(final Class<V> enumClass) {
        return new StreamCodec<>() {
            @Override
            public V decode(final B buf) {
                return buf.readEnum(enumClass);
            }

            @Override
            public void encode(final B buf, final V value) {
                buf.writeEnum(value);
            }
        };
    }

    private StreamCodecs() {
    }
}
