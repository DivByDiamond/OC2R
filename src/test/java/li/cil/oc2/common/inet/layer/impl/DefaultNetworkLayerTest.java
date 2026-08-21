package li.cil.oc2.common.inet.layer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.TransportMessage;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.api.inet.layer.TransportLayer;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import li.cil.oc2.common.inet.util.checksum.Rfc1071Checksum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class DefaultNetworkLayerTest {
    private static final int SRC = 0x0A000201; // 10.0.2.1
    private static final int DST = 0x01010101; // 1.1.1.1
    private static final int ETHERNET_HEADER_SIZE = 14;
    private static final int IPV4_HEADER_SIZE = 20;
    private static final int ICMP_HEADER_SIZE = 8;

    private LayerParameters layerParameters;
    private InternetManagerImpl internetManager;
    private TransportLayer transportLayer;
    private DefaultNetworkLayer layer;

    @BeforeEach
    void setUp() {
        layerParameters = mock(LayerParameters.class);
        internetManager = mock(InternetManagerImpl.class);
        transportLayer = mock(TransportLayer.class);
        when(layerParameters.getInternetManager()).thenReturn(internetManager);
        when(internetManager.isAllowedToConnect(anyInt())).thenReturn(true);
        when(transportLayer.receiveTransportMessage(any(TransportMessage.class)))
                .thenReturn(TransportLayer.PROTOCOL_NONE);
        layer = new DefaultNetworkLayer(layerParameters, transportLayer);
    }

    private static ByteBuffer ipv4Packet(final int ttl, final int flagsAndFragment, final byte[] payload) {
        return ipv4Packet(0x45, ttl, flagsAndFragment, (byte) 6, SRC, DST, payload);
    }

    private static ByteBuffer ipv4Packet(
            final int versionAndIhl,
            final int ttl,
            final int flagsAndFragment,
            final byte protocol,
            final int src,
            final int dst,
            final byte[] payload) {
        final ByteBuffer packet = ByteBuffer.allocate(IPV4_HEADER_SIZE + payload.length);
        packet.put((byte) versionAndIhl);
        packet.put((byte) 0);
        packet.putShort((short) (IPV4_HEADER_SIZE + payload.length));
        packet.putShort((short) 0x1234); // identification
        packet.putShort((short) flagsAndFragment);
        packet.put((byte) ttl);
        packet.put(protocol);
        packet.putShort((short) 0); // header checksum is not validated by the layer
        packet.putInt(src);
        packet.putInt(dst);
        packet.put(payload);
        packet.flip();
        return packet;
    }

    /** Delivers a queued ICMP error through receivePacket and returns the frame positioned at the IP header. */
    private ByteBuffer receiveQueuedIcmpError() {
        final ByteBuffer frame = ByteBuffer.allocate(ETHERNET_HEADER_SIZE + IPV4_HEADER_SIZE + ICMP_HEADER_SIZE + 28);
        frame.position(ETHERNET_HEADER_SIZE);
        final short result = layer.receivePacket(frame);
        assertEquals(NetworkLayer.PROTOCOL_IPv4, result);
        frame.position(ETHERNET_HEADER_SIZE);
        return frame;
    }

    private static void assertValidChecksum(final ByteBuffer frame, final int absolutePosition, final int size) {
        final short stored = frame.getShort(absolutePosition + 2);
        final ByteBuffer region = ByteBuffer.allocate(size);
        final ByteBuffer duplicate = frame.duplicate();
        duplicate.position(absolutePosition).limit(absolutePosition + size);
        region.put(duplicate);
        region.putShort(2, (short) 0);
        region.flip();
        assertEquals(stored, Rfc1071Checksum.rfc1071Checksum(region));
    }

    @Test
    void sendPacketForwardsValidPacketWithDecrementedTtl() {
        final ByteBuffer packet = ipv4Packet(64, 0, new byte[] {1, 2, 3, 4});
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, packet);
        final ArgumentCaptor<TransportMessage> captor = ArgumentCaptor.forClass(TransportMessage.class);
        verify(transportLayer).sendTransportMessage(eq(TransportLayer.PROTOCOL_TCP), captor.capture());
        assertEquals(63, captor.getValue().getTtl());
    }

    @Test
    void sendPacketIgnoresNonIpv4Protocol() {
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv6, ipv4Packet(64, 0, new byte[4]));
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));
    }

    @Test
    void sendPacketDropsTooSmallPacket() {
        final ByteBuffer packet = ByteBuffer.allocate(10);
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, packet);
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));
    }

    @Test
    void sendPacketDropsWrongProtocolVersion() {
        final ByteBuffer packet = ipv4Packet(0x65, 64, 0, (byte) 6, SRC, DST, new byte[4]);
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, packet);
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));
    }

    @Test
    void fragmentedPacketIsDroppedAndIcmpFragNeededIsQueued() {
        final byte[] payload = {1, 2, 3, 4, 5, 6, 7, 8};
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(64, 0x2000, payload)); // MF set
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));

        final ByteBuffer frame = receiveQueuedIcmpError();
        assertEquals(0x45, frame.get(frame.position())); // version 4, IHL 5
        assertEquals(1, frame.get(frame.position() + 9)); // protocol = ICMP
        assertEquals(DST, frame.getInt(frame.position() + 12)); // source = original destination
        assertEquals(SRC, frame.getInt(frame.position() + 16)); // destination = original source
        final int icmp = frame.position() + IPV4_HEADER_SIZE;
        assertEquals(3, frame.get(icmp)); // Destination Unreachable
        assertEquals(4, frame.get(icmp + 1)); // fragmentation needed
        assertEquals(1500, frame.getShort(icmp + 6) & 0xFFFF); // next-hop MTU
        // Quoted data: original IP header plus first 8 bytes of its payload.
        for (int i = 0; i < IPV4_HEADER_SIZE; i++) {
            assertEquals(ipv4Packet(64, 0x2000, payload).get(i), frame.get(icmp + ICMP_HEADER_SIZE + i));
        }
        for (int i = 0; i < payload.length; i++) {
            assertEquals(payload[i], frame.get(icmp + ICMP_HEADER_SIZE + IPV4_HEADER_SIZE + i));
        }
        assertValidChecksum(frame, icmp, ICMP_HEADER_SIZE + IPV4_HEADER_SIZE + 8);
        assertValidChecksum(frame, frame.position(), IPV4_HEADER_SIZE);
    }

    @Test
    void packetWithNonZeroFragmentOffsetIsDroppedAndIcmpFragNeededIsQueued() {
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(64, 0x0005, new byte[8]));
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));
        receiveQueuedIcmpError();
    }

    @Test
    void expiredTtlIsAnsweredWithIcmpTimeExceeded() {
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(1, 0, new byte[8]));
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));

        final ByteBuffer frame = receiveQueuedIcmpError();
        final int icmp = frame.position() + IPV4_HEADER_SIZE;
        assertEquals(11, frame.get(icmp)); // Time Exceeded
        assertEquals(0, frame.get(icmp + 1)); // TTL count exceeded
        assertEquals(0, frame.getInt(icmp + 4)); // unused field
        assertValidChecksum(frame, icmp, ICMP_HEADER_SIZE + IPV4_HEADER_SIZE + 8);
    }

    @Test
    void deniedDestinationIsDroppedSilently() {
        when(internetManager.isAllowedToConnect(DST)).thenReturn(false);
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(64, 0, new byte[8]));
        verify(transportLayer, never()).sendTransportMessage(anyByte(), any(TransportMessage.class));

        final ByteBuffer frame = ByteBuffer.allocate(128);
        frame.position(ETHERNET_HEADER_SIZE);
        assertEquals(NetworkLayer.PROTOCOL_NONE, layer.receivePacket(frame));
    }

    @Test
    void noIcmpErrorIsDeliveredWhenNothingWasDropped() {
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(64, 0, new byte[8]));
        final ByteBuffer frame = ByteBuffer.allocate(128);
        frame.position(ETHERNET_HEADER_SIZE);
        assertEquals(NetworkLayer.PROTOCOL_NONE, layer.receivePacket(frame));
    }

    @Test
    void icmpErrorIsDeliveredOnlyOnce() {
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(1, 0, new byte[8]));
        receiveQueuedIcmpError();
        final ByteBuffer frame = ByteBuffer.allocate(128);
        frame.position(ETHERNET_HEADER_SIZE);
        assertEquals(NetworkLayer.PROTOCOL_NONE, layer.receivePacket(frame));
    }

    @Test
    void receivePacketReturnsNoneWhenTransportHasNoData() {
        final ByteBuffer frame = ByteBuffer.allocate(128);
        frame.position(ETHERNET_HEADER_SIZE);
        assertEquals(NetworkLayer.PROTOCOL_NONE, layer.receivePacket(frame));
    }

    @Test
    void newestErrorWinsOnlyAfterDelivery() {
        // Second drop while an error is still queued must not overwrite it.
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(1, 0, new byte[8])); // queues Time Exceeded
        layer.sendPacket(NetworkLayer.PROTOCOL_IPv4, ipv4Packet(64, 0x2000, new byte[8])); // ignored

        final ByteBuffer frame = receiveQueuedIcmpError();
        final int icmp = frame.position() + IPV4_HEADER_SIZE;
        assertEquals(11, frame.get(icmp));
        assertTrue(frame.remaining() > 0);
    }
}
