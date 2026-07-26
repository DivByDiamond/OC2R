package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.TransportMessage;
import java.nio.ByteBuffer;

final class IcmpHandler {
    static final byte ICMP_TYPE_ECHO_REPLY = 0;
    static final byte ICMP_TYPE_ECHO_REQUEST = 8;
    static final byte ICMP_TYPE_ECHO_UNREACHABLE = 3;
    static final byte ICMP_CODE_ECHO_UNREACHABLE_PORT = 3;
    static final byte ICMP_CODE_ECHO_UNREACHABLE_PROHIBITED = 13;
    static final short PORT_ECHO = 7;
    static final int ICMP_HEADER_SIZE = 8;

    ICMPReply icmpReply = null;

    void prepareIcmpHeader(final ByteBuffer buffer, final byte type, final byte code) {
        final int position = buffer.position();
        buffer.put(type);
        buffer.put(code);
        buffer.putShort((short) 0);
        buffer.position(position);
        final short checksum = Rfc1071Checksum.rfc1071Checksum(buffer);
        buffer.putShort(position + 2, checksum);
        buffer.position(position);
    }

    void reject(final ByteBuffer payload, final int srcIpAddress) {
        final byte[] data = InetUtils.quickICMPBody(payload);
        icmpReply = new ICMPReply(
                ICMP_TYPE_ECHO_UNREACHABLE,
                ICMP_CODE_ECHO_UNREACHABLE_PROHIBITED,
                0,
                srcIpAddress,
                data
        );
    }

    boolean consume(final TransportMessage message) {
        if (icmpReply == null) return false;
        message.updateIpv4(icmpReply.srcIpAddress(), icmpReply.dstIpAddress());
        final ByteBuffer data = message.getData();
        final int position = data.position();
        data.putInt(0);
        data.put(icmpReply.payload());
        data.limit(data.position());
        data.position(position);
        prepareIcmpHeader(data, icmpReply.type(), icmpReply.code());
        icmpReply = null;
        return true;
    }
}
