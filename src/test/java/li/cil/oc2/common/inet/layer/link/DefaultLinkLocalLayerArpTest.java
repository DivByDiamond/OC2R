package li.cil.oc2.common.inet.layer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Optional;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The card answers for the gateway only; address conflict detection of the guest must not confuse it. */
class DefaultLinkLocalLayerArpTest {
    private static final int GUEST = 0x0A00020F; // 10.0.2.15
    private static final int GATEWAY = 0x0A000202; // 10.0.2.2
    private static final short MAC_PREFIX = 0x0200;
    private static final int MAC_ADDRESS = 0x0000AA01;

    private NetworkLayer network;
    private DefaultLinkLocalLayer layer;

    @BeforeEach
    void setUp() {
        network = mock(NetworkLayer.class);
        when(network.receivePacket(any())).thenReturn(NetworkLayer.PROTOCOL_NONE);
        final LayerParameters parameters = mock(LayerParameters.class);
        when(parameters.getSavedState()).thenReturn(Optional.empty());
        layer = new DefaultLinkLocalLayer(parameters, network);
    }

    private static ByteBuffer arp(final int senderIp, final int targetIp) {
        final ByteBuffer frame = ByteBuffer.allocate(14 + 28);
        frame.put(new byte[] {-1, -1, -1, -1, -1, -1});
        frame.putShort(MAC_PREFIX).putInt(MAC_ADDRESS);
        frame.putShort((short) 0x0806);
        frame.putShort((short) 1).putShort((short) 0x0800).put((byte) 6).put((byte) 4).putShort((short) 1);
        frame.putShort(MAC_PREFIX).putInt(MAC_ADDRESS).putInt(senderIp);
        frame.putShort((short) 0).putInt(0).putInt(targetIp);
        frame.flip();
        return frame;
    }

    private static ByteBuffer ipv4(final int sourceIp) {
        final ByteBuffer frame = ByteBuffer.allocate(14 + 28);
        frame.put(new byte[] {2, 0, 0, 0, 0, 1});
        frame.putShort(MAC_PREFIX).putInt(MAC_ADDRESS);
        frame.putShort((short) 0x0800);
        frame.put((byte) 0x45).put((byte) 0).putShort((short) 28).putInt(0).put((byte) 64).put((byte) 17).putShort((short) 0);
        frame.putInt(sourceIp).putInt(0x01010101);
        frame.flip();
        return frame;
    }

    private boolean hasReply() {
        return layer.receiveEthernetFrame(ByteBuffer.allocate(1514));
    }

    @Test
    void aRequestForTheGatewayIsAnswered() {
        layer.sendEthernetFrame(arp(GUEST, GATEWAY));
        assertTrue(hasReply());
        assertFalse(hasReply(), "one request, one reply");
    }

    @Test
    void anAddressProbeIsNotAnswered() {
        layer.sendEthernetFrame(arp(0, GUEST));
        assertFalse(hasReply(), "answering tells the guest that someone else owns its address");
    }

    @Test
    void anAnnouncementIsNotAnswered() {
        layer.sendEthernetFrame(arp(GUEST, GUEST));
        assertFalse(hasReply());
    }

    @Test
    void aProbeDoesNotMakeTheCardForgetTheGuestAddress() {
        layer.sendEthernetFrame(arp(GUEST, GATEWAY));
        hasReply();
        layer.sendEthernetFrame(arp(0, GUEST));
        layer.sendEthernetFrame(ipv4(GUEST));
        verify(network, times(1)).sendPacket(anyShort(), any());
    }

    @Test
    void anAnnouncementAloneNamesTheGuestAddress() {
        layer.sendEthernetFrame(arp(GUEST, GUEST));
        layer.sendEthernetFrame(ipv4(GUEST));
        verify(network, times(1)).sendPacket(anyShort(), any());
    }

    @Test
    void aSpoofedSourceIsStillDropped() {
        layer.sendEthernetFrame(arp(GUEST, GATEWAY));
        hasReply();
        layer.sendEthernetFrame(ipv4(0x08080808));
        verify(network, never()).sendPacket(anyShort(), any());
    }

    @Test
    void theGatewayStaysWhateverTheGuestAskedForLast() {
        layer.sendEthernetFrame(arp(GUEST, GATEWAY));
        final ByteBuffer first = ByteBuffer.allocate(1514);
        assertTrue(layer.receiveEthernetFrame(first));
        // The reply names the gateway address as the sender address, at offset 14 + 14.
        assertEquals(GATEWAY, first.getInt(14 + 14));
    }
}
