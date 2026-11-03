package li.cil.oc2.common.radio.service;

import li.cil.oc2.common.radio.RadioAddress;
import java.io.*;
import java.util.UUID;

/** Versioned, length bounded binary frame for reliable application streams. */
public final class RadioMessageCodec {
    public static final int MAX_FRAME = RadioMessage.MAX_PAYLOAD + 1024;
    private RadioMessageCodec() {}
    public static byte[] encode(RadioMessage message) {
        try {
            var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes);
            out.writeInt(message.version()); uuid(out, message.source()); out.writeUTF(message.sourceAddress().toString());
            uuid(out, message.destination()); out.writeUTF(message.destinationAddress().toString());
            out.writeUTF(message.service()); out.writeUTF(message.channel()); uuid(out, message.requestId());
            out.writeByte(message.type().ordinal()); var payload = message.payload(); out.writeInt(payload.length); out.write(payload);
            return bytes.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException(exception); }
    }
    public static RadioMessage decode(byte[] frame) {
        if (frame == null || frame.length > MAX_FRAME) throw new IllegalArgumentException("Frame limit");
        try {
            var input = new DataInputStream(new ByteArrayInputStream(frame));
            int version = input.readInt(); var source = uuid(input); var sourceAddress = RadioAddress.parse(input.readUTF());
            var destination = uuid(input); var destinationAddress = RadioAddress.parse(input.readUTF());
            String service = input.readUTF(), channel = input.readUTF(); var request = uuid(input);
            int type = input.readUnsignedByte(), length = input.readInt();
            if (type >= RadioMessage.Type.values().length || length < 0 || length > RadioMessage.MAX_PAYLOAD || length != input.available()) throw new IllegalArgumentException("Malformed frame");
            return new RadioMessage(version, source, sourceAddress, destination, destinationAddress, service, channel, request, RadioMessage.Type.values()[type], input.readNBytes(length));
        } catch (IOException exception) { throw new IllegalArgumentException("Malformed frame", exception); }
    }
    private static void uuid(DataOutputStream out, UUID value) throws IOException { out.writeLong(value.getMostSignificantBits()); out.writeLong(value.getLeastSignificantBits()); }
    private static UUID uuid(DataInputStream input) throws IOException { return new UUID(input.readLong(), input.readLong()); }
}
