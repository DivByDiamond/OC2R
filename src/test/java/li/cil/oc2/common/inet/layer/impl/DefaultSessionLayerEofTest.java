package li.cil.oc2.common.inet.layer.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Real-world EOF must follow all buffered response bytes towards the guest. */
class DefaultSessionLayerEofTest {
    private DefaultSessionLayer layer;
    private StreamSessionImpl session;
    private SocketChannel channel;

    @BeforeEach
    void setUp() {
        final InternetManager manager = mock(InternetManager.class);
        when(manager.runOnInternetThreadTick(any())).thenReturn(mock(InternetManager.Task.class));
        final LayerParameters parameters = mock(LayerParameters.class);
        when(parameters.getInternetManager()).thenReturn(manager);
        layer = new DefaultSessionLayer(parameters);
        session = new StreamSessionImpl(0x01010101, (short) 80,
                new StreamSessionDiscriminator(0x0A00020F, (short) 40000, 0x01010101, (short) 80));
        session.state = TcpStates.ESTABLISHED;
        session.vmWindow = 65535;
        channel = mock(SocketChannel.class);
        session.setAttachment(channel);
    }

    @AfterEach
    void tearDown() {
        layer.onStop();
    }

    @Test
    void responseAndImmediateEofKeepEveryHttpByteUntilAcknowledgedThenSendFin() throws Exception {
        final byte[] response = "HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nPASS".getBytes(StandardCharsets.US_ASCII);
        when(channel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            invocation.<ByteBuffer>getArgument(0).put(response);
            return response.length;
        }).thenReturn(-1);
        readSocket();
        assertFalse(session.isClosed(), "socket EOF must not discard the queued HTTP response");
        final ByteBuffer wire = segment(response.length);
        assertEquals(SessionActions.FORWARD, session.receive(wire));
        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(wire));
        assertFalse(header.fin, "FIN must not precede response data");
        final byte[] received = new byte[wire.remaining()];
        wire.get(received);
        assertArrayEquals(response, received);
        acknowledge(response.length);
        assertFin();
    }

    @Test
    void emptyPeerCloseStillProducesFinInsteadOfSilentlyDroppingTheConnection() throws Exception {
        when(channel.read(any(ByteBuffer.class))).thenReturn(-1);
        readSocket();
        assertFin();
    }

    @Test
    void fullReceiveBufferAndLaterEofPreserveUnsentAndInFlightBytes() throws Exception {
        final byte[] response = new byte[session.receiveBuffer.capacity()];
        Arrays.fill(response, (byte) 73);
        when(channel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            final ByteBuffer target = invocation.getArgument(0);
            target.put(response);
            return response.length;
        }).thenReturn(0, -1);
        readSocket();
        final int first = response.length / 2;
        assertEquals(SessionActions.FORWARD, session.receive(segment(first)));
        acknowledge(first);
        readSocket();
        final ByteBuffer rest = segment(response.length);
        assertEquals(SessionActions.FORWARD, session.receive(rest));
        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(rest));
        assertFalse(header.fin);
        assertEquals(response.length - first, rest.remaining());
        while (rest.hasRemaining()) assertEquals(73, rest.get());
        acknowledge(response.length - first);
        assertFin();
    }

    private void readSocket() throws Exception {
        final Method read = DefaultSessionLayer.class.getDeclaredMethod("readStream", SessionLayer.Receiver.class, StreamSession.class);
        read.setAccessible(true); // NOPMD - Observe private state without exposing a production-only testing API.
        final SessionLayer.Receiver receiver = ignored -> session.getReceiveBuffer();
        assertEquals(true, read.invoke(layer, receiver, session));
    }

    private static ByteBuffer segment(final int payload) {
        return ByteBuffer.allocate(TcpHeader.MIN_HEADER_SIZE_NO_PORTS + payload);
    }

    private void acknowledge(final int count) {
        final TcpHeader header = new TcpHeader();
        header.ack = true;
        header.maxSegmentSize = -1;
        header.sequenceNumber = session.vmSequence;
        header.acknowledgmentNumber = session.mySequence + count;
        header.window = 65535;
        final ByteBuffer ack = segment(0);
        header.write(ack);
        ack.flip();
        assertEquals(SessionActions.FORWARD, session.send(ack));
    }

    private void assertFin() {
        final ByteBuffer wire = segment(0);
        assertEquals(SessionActions.FORWARD, session.receive(wire), "EOF must be communicated as a TCP FIN");
        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(wire));
        assertTrue(header.fin);
        assertFalse(header.rst);
        assertFalse(wire.hasRemaining());
    }
}
