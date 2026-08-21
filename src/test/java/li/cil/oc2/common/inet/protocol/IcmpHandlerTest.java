package li.cil.oc2.common.inet.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.TransportMessage;
import li.cil.oc2.common.inet.util.checksum.Rfc1071Checksum;
import org.junit.jupiter.api.Test;

final class IcmpHandlerTest {
    private static final int SRC = 0x0A000201;
    private static final int DST = 0x01010101;

    @Test
    void prepareIcmpHeaderWritesTypeCodeAndValidChecksum() {
        final ByteBuffer buffer = ByteBuffer.allocate(IcmpHandler.ICMP_HEADER_SIZE + 4);
        buffer.putInt(0x12345678); // body the checksum must cover
        buffer.position(0);

        new IcmpHandler().prepareIcmpHeader(buffer, IcmpHandler.ICMP_TYPE_ECHO_REQUEST, (byte) 0);

        assertEquals(IcmpHandler.ICMP_TYPE_ECHO_REQUEST, buffer.get(0));
        assertEquals(0, buffer.get(1));
        assertNotZero(buffer.getShort(2));
        final short stored = buffer.getShort(2);
        buffer.putShort(2, (short) 0);
        buffer.position(0);
        assertEquals(stored, Rfc1071Checksum.rfc1071Checksum(buffer));
    }

    private static void assertNotZero(final short value) {
        assertTrue(value != 0, "checksum must be computed");
    }

    @Test
    void rejectThenConsumeWritesReplyIntoMessage() {
        final IcmpHandler handler = new IcmpHandler();
        final TransportMessage message = new TransportMessage();
        final ByteBuffer data = ByteBuffer.allocate(64);
        message.initializeBuffer(data);

        // reject() quotes the original frame via quickICMPBody, which expects an
        // ethernet header followed by an IPv4 header and at least 8 bytes of payload.
        final ByteBuffer frame = ByteBuffer.allocate(14 + 20 + 8);
        frame.position(14);
        frame.put((byte) 0x45);
        frame.put((byte) 0);
        frame.putShort((short) 28);
        frame.putShort((short) 0);
        frame.putShort((short) 0);
        frame.put((byte) 64);
        frame.put((byte) 17);
        frame.putShort((short) 0);
        frame.putInt(SRC);
        frame.putInt(DST);
        frame.put(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        frame.flip();

        handler.reject(frame, DST, SRC);

        assertTrue(handler.consume(message));
        assertFalse(handler.consume(message)); // reply is single-shot

        // The unreachable reply must come from the host that was unreachable,
        // addressed back to the guest (previously the source was 0.0.0.0).
        assertEquals(DST, message.getSrcIpv4Address());
        assertEquals(SRC, message.getDstIpv4Address());

        assertEquals(IcmpHandler.ICMP_HEADER_SIZE - 4 + 4 + 28, data.limit());
        assertEquals(IcmpHandler.ICMP_TYPE_ECHO_UNREACHABLE, data.get(0));
        assertEquals(IcmpHandler.ICMP_CODE_ECHO_UNREACHABLE_PROHIBITED, data.get(1));
        assertNotZero(data.getShort(2)); // checksum over the whole reply
        assertEquals(1500, data.getShort(6) & 0xFFFF); // next-hop MTU written by quickICMPBody
        assertEquals(SRC, data.getInt(8 + 12)); // quoted original IP header source
        final byte[] echoedPayload = new byte[8];
        data.position(8 + 20);
        data.get(echoedPayload);
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, echoedPayload);
    }

    @Test
    void consumeWithoutPendingReplyIsNoOp() {
        final IcmpHandler handler = new IcmpHandler();
        final TransportMessage message = new TransportMessage();
        message.initializeBuffer(ByteBuffer.allocate(16));
        assertFalse(handler.consume(message));
    }

    @Test
    void sendIcmpMessageContractUsesEchoRequestTypeAndPortSeven() {
        assertEquals(7, IcmpHandler.PORT_ECHO);
        assertEquals(8, IcmpHandler.ICMP_TYPE_ECHO_REQUEST);
        assertEquals(0, IcmpHandler.ICMP_TYPE_ECHO_REPLY);
        assertEquals(IcmpHandler.ICMP_HEADER_SIZE, 8);
    }
}
