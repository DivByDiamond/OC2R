package li.cil.oc2.common.inet.session.echo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.SessionReceiver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A ping out of the game: the answer has to carry the data of the request, and a burst must not lose replies. */
@SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "The tests set the process-wide configuration and put it back afterwards.")
final class EchoHandlerTest {
    private static final int LOOPBACK = 0x7F000001;
    private static final int TEST_NET = 0xC0000201; // 192.0.2.1, documentation range, nobody answers

    private Queue<EchoResponse> responses;
    private int savedTimeout;

    @BeforeEach
    void setUp() {
        responses = EchoHandler.newResponseQueue();
        savedTimeout = Config.defaultEchoRequestTimeoutMs;
        Config.defaultEchoRequestTimeoutMs = 400;
    }

    @AfterEach
    void tearDown() {
        Config.defaultEchoRequestTimeoutMs = savedTimeout;
    }

    private static EchoSessionImpl session(final int address, final int identity) {
        final EchoSessionImpl session = new EchoSessionImpl(address, (short) 0,
                new EchoSessionDiscriminator(0x0A000201, address, (short) identity));
        session.setSequenceNumber((short) 1);
        return session;
    }

    private static byte[] payload(final int seed) {
        final byte[] bytes = new byte[56];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i * 3 + seed);
        return bytes;
    }

    private EchoResponse awaitReply() throws InterruptedException {
        for (int i = 0; i < 100 && responses.isEmpty(); i++) TimeUnit.MILLISECONDS.sleep(50);
        return responses.poll();
    }

    @Test
    void theReplyCarriesTheDataOfTheRequest() throws Exception {
        final byte[] sent = payload(1);
        final EchoSessionImpl session = session(LOOPBACK, 7);
        assertTrue(EchoHandler.handleEchoSession(session, ByteBuffer.wrap(sent.clone()), responses));
        final EchoResponse reply = awaitReply();
        assertNotNull(reply, "loopback answers a ping");
        assertArrayEquals(sent, reply.payload, "the fallback used to answer with an empty payload");
    }

    @Test
    void aReplyIsDeliveredIntoTheFrameOfTheSession() throws Exception {
        final byte[] sent = payload(5);
        final EchoSessionImpl session = session(LOOPBACK, 8);
        session.setState(Session.States.ESTABLISHED);
        EchoHandler.handleEchoSession(session, ByteBuffer.wrap(sent.clone()), responses);
        for (int i = 0; i < 100 && responses.isEmpty(); i++) TimeUnit.MILLISECONDS.sleep(50);
        final SessionReceiver receiver = new SessionReceiver();
        receiver.prepare(ByteBuffer.allocate(1514).position(34).slice());
        assertTrue(EchoHandler.deliverPendingResponse(responses, receiver));
        final ByteBuffer frame = receiver.getBuffer();
        frame.position(0);
        frame.limit(8 + sent.length);
        frame.position(8);
        final byte[] received = new byte[sent.length];
        frame.get(received);
        assertArrayEquals(sent, received);
    }

    @Test
    void aBurstOfPingsKeepsEveryReply() throws Exception {
        final List<byte[]> sent = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sent.add(payload(i));
            EchoHandler.handleEchoSession(session(LOOPBACK, 100 + i), ByteBuffer.wrap(sent.get(i).clone()), responses);
        }
        for (int i = 0; i < 100 && responses.size() < 5; i++) TimeUnit.MILLISECONDS.sleep(50);
        assertEquals(5, responses.size(), "replies overwrote each other in the single-slot holder");
    }

    @Test
    void aHostThatDoesNotAnswerGetsNoReply() throws Exception {
        EchoHandler.handleEchoSession(session(TEST_NET, 9), ByteBuffer.wrap(payload(2)), responses);
        TimeUnit.MILLISECONDS.sleep(2500);
        assertNull(responses.poll());
    }

    @Test
    void anEmptyQueueDeliversNothing() {
        final SessionLayer.Receiver receiver = mock(SessionLayer.Receiver.class);
        when(receiver.receive(org.mockito.ArgumentMatchers.any())).thenReturn(ByteBuffer.allocate(16));
        assertFalse(EchoHandler.deliverPendingResponse(responses, receiver));
    }
}
