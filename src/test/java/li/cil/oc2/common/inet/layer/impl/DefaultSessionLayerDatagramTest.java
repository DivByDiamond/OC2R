package li.cil.oc2.common.inet.layer.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionReceiver;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionDiscriminator;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** UDP answers coming back from the real world, read from real loopback sockets. */
class DefaultSessionLayerDatagramTest {
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();
    private static final int FRAME = 1514 - 14 - 20;

    private DefaultSessionLayer layer;
    private DatagramChannel channel;
    private DatagramSocket server;
    private SessionReceiver receiver;

    @BeforeEach
    void setUp() throws Exception {
        final InternetManager manager = mock(InternetManager.class);
        when(manager.runOnInternetThreadTick(any())).thenReturn(mock(InternetManager.Task.class));
        final LayerParameters parameters = mock(LayerParameters.class);
        when(parameters.getInternetManager()).thenReturn(manager);
        layer = new DefaultSessionLayer(parameters);
        channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(LOOPBACK, 0));
        channel.configureBlocking(false);
        server = new DatagramSocket(new InetSocketAddress(LOOPBACK, 0));
        receiver = new SessionReceiver();
        receiver.prepare(ByteBuffer.allocate(FRAME));
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.close();
        server.close();
        layer.onStop();
    }

    private DatagramSessionImpl session(final int destinationPort) {
        final DatagramSessionImpl session = new DatagramSessionImpl(0x7F000001, (short) destinationPort,
                new DatagramSessionDiscriminator(0x0A00020F, (short) 50000, 0x7F000001, (short) destinationPort));
        session.setState(Session.States.ESTABLISHED);
        session.setAttachment(channel);
        return session;
    }

    private void answer(final byte[] datagram) throws Exception {
        server.send(new DatagramPacket(datagram, datagram.length, LOOPBACK,
                ((InetSocketAddress) channel.getLocalAddress()).getPort()));
        Thread.sleep(100); // loopback delivery is immediate; the channel is non-blocking
    }

    private boolean read(final DatagramSessionImpl session) throws Exception {
        final Method read = DefaultSessionLayer.class.getDeclaredMethod("readDatagram", SessionLayer.Receiver.class,
                li.cil.oc2.api.inet.session.DatagramSession.class);
        read.setAccessible(true); // NOPMD - Observe private state without exposing a production-only testing API.
        return (boolean) read.invoke(layer, receiver, session);
    }

    private static byte[] dnsAnswer(final int size) {
        final byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) bytes[i] = (byte) (i * 7 + 1);
        bytes[2] = (byte) 0x81; // response, recursion desired
        bytes[3] = (byte) 0x80;
        return bytes;
    }

    @Test
    void aSmallAnswerArrivesIntact() throws Exception {
        final byte[] sent = dnsAnswer(120);
        answer(sent);
        assertTrue(read(session(5300)));
        final ByteBuffer frame = receiver.getBuffer();
        frame.position(8);
        final byte[] received = new byte[frame.remaining()];
        frame.get(received);
        assertArrayEquals(sent, received);
    }

    @Test
    void anOversizedDnsAnswerIsCutAndFlaggedAsTruncated() throws Exception {
        final byte[] sent = dnsAnswer(3000);
        answer(sent);
        assertTrue(read(session(53)));
        final ByteBuffer frame = receiver.getBuffer();
        frame.position(8);
        final int length = frame.remaining();
        assertEquals(FRAME - 8, length, "it fills the frame");
        assertEquals((byte) 0x83, frame.get(8 + 2), "the TC bit is set on the flags byte");
        assertEquals(sent[5], frame.get(8 + 5));
        assertEquals(sent[length - 1], frame.get(8 + length - 1));
    }

    @Test
    void anOversizedAnswerFromAnythingButDnsIsDropped() throws Exception {
        answer(dnsAnswer(3000));
        final DatagramSessionImpl session = session(5300);
        assertFalse(read(session));
        assertNull(receiver.session, "no message is built from a datagram that was dropped");
    }

    @Test
    void readinessWithoutAnyDataDeliversNothing() throws Exception {
        final DatagramSessionImpl session = session(53);
        assertFalse(read(session));
        assertNull(receiver.session, "an empty read used to hand the guest a datagram of zeros");
    }
}
