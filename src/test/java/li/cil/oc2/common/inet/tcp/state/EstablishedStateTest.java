package li.cil.oc2.common.inet.tcp.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the internet-to-VM data path of {@link EstablishedState}: sliding window
 * segmentation and cumulative acknowledgment (todo.md §38 П3.1).
 */
final class EstablishedStateTest {
    private static boolean isForward(final SessionActions action) {
        return action == SessionActions.FORWARD;
    }

    private static final int SRC_IP = 0x0A000201;
    private static final int DST_IP = 0x01010101;
    private static final short SRC_PORT = (short) 40000;
    private static final short DST_PORT = 80;

    private StreamSessionImpl session;
    private EstablishedState established;

    /** Segment capacity handed to receive(): header plus this much payload. */
    private static final int SEGMENT_PAYLOAD = 1400;

    @BeforeEach
    void setUp() {
        session =
                new StreamSessionImpl(
                        DST_IP,
                        DST_PORT,
                        new StreamSessionDiscriminator(SRC_IP, SRC_PORT, DST_IP, DST_PORT));
        // Simulate a completed handshake.
        session.state = li.cil.oc2.common.inet.tcp.TcpStates.ESTABLISHED;
        session.vmWindow = 64 * 1024;
        established = new EstablishedState();
    }

    private void fillReceiveBuffer(final int bytes) {
        final ByteBuffer buffer = session.receiveBuffer;
        for (int i = 0; i < bytes; i++) {
            buffer.put((byte) i);
        }
    }

    private ByteBuffer buildSegment() {
        return ByteBuffer.allocate(SEGMENT_PAYLOAD + TcpHeader.MIN_HEADER_SIZE_NO_PORTS);
    }

    /** Parses the payload out of a built segment (header is skipped via its own reader). */
    private byte[] payloadOf(final ByteBuffer segment) {
        // EstablishedState.receive already flips the buffer into read mode.
        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(segment));
        final byte[] payload = new byte[segment.remaining()];
        segment.get(payload);
        return payload;
    }

    @Test
    void firstSegmentCarriesContiguousChunk() {
        fillReceiveBuffer(3000);

        assertTrue(isForward(established.receive(session, buildSegment())));

        assertEquals(session.mySequence, session.header.sequenceNumber);
        assertTrue(session.header.psh);
        assertEquals(Math.min(3000, SEGMENT_PAYLOAD), session.nextSegmentMark);
    }

    @Test
    void subsequentSegmentsExtendTheWindowWithoutAck() {
        fillReceiveBuffer(6000);

        established.receive(session, buildSegment());
        final int firstChunk = session.nextSegmentMark;
        final int firstSeq = session.header.sequenceNumber;

        assertTrue(isForward(established.receive(session, buildSegment())));

        final int secondChunk = session.nextSegmentMark - firstChunk;
        assertTrue(secondChunk > 0, "window must extend without waiting for an ACK");
        assertEquals(firstSeq + firstChunk, session.header.sequenceNumber);
    }

    @Test
    void inFlightDataIsBoundedByVmWindow() {
        session.vmWindow = 2000;
        fillReceiveBuffer(6000);

        established.receive(session, buildSegment());
        SessionActions action = established.receive(session, buildSegment());
        while (isForward(action) && session.header.psh) {
            assertTrue(session.nextSegmentMark <= 2000);
            action = established.receive(session, buildSegment());
        }
        assertEquals(2000, session.nextSegmentMark, "in-flight must cap at the VM window");
    }

    @Test
    void cumulativeAckCompactsBufferAndSlidesSequence() {
        fillReceiveBuffer(5000);
        assertTrue(isForward(established.receive(session, buildSegment()))); // chunk 1
        assertTrue(isForward(established.receive(session, buildSegment()))); // chunk 2
        final int sequenceBefore = session.mySequence;

        final int ackedBytes = 2500;
        final ByteBuffer ack = buildSegment();
        final TcpHeader header = new TcpHeader();
        header.ack = true;
        header.acknowledgmentNumber = sequenceBefore + ackedBytes;
        header.sequenceNumber = session.vmSequence;
        header.psh = false;
        header.write(ack);
        ack.flip();

        assertTrue(isForward(established.send(session, ack)));

        assertEquals(sequenceBefore + ackedBytes, session.mySequence);
        assertEquals(
                5000 - ackedBytes,
                session.receiveBuffer.position(),
                "acked prefix must be compacted out");
        assertTrue(session.nextSegmentMark < 5000 - ackedBytes + 1);
    }

    @Test
    void duplicateAckChangesNothing() {
        fillReceiveBuffer(3000);
        established.receive(session, buildSegment());
        final int markBefore = session.nextSegmentMark;
        final int seqBefore = session.mySequence;

        final ByteBuffer dupAck = buildSegment();
        final TcpHeader header = new TcpHeader();
        header.ack = true;
        header.acknowledgmentNumber = session.mySequence; // zero coverage
        header.sequenceNumber = session.vmSequence;
        header.write(dupAck);
        dupAck.flip();

        assertTrue(isForward(established.send(session, dupAck)));
        assertEquals(markBefore, session.nextSegmentMark);
        assertEquals(seqBefore, session.mySequence);
        assertEquals(3000, session.receiveBuffer.position());
    }

    @Test
    void ackBeyondInFlightWindowIsRejected() {
        fillReceiveBuffer(3000);
        established.receive(session, buildSegment());

        final ByteBuffer badAck = buildSegment();
        final TcpHeader header = new TcpHeader();
        header.ack = true;
        header.acknowledgmentNumber = session.mySequence + session.nextSegmentMark + 1;
        header.sequenceNumber = session.vmSequence;
        header.write(badAck);
        badAck.flip();

        assertFalse(isForward(established.send(session, badAck)));
    }

    @Test
    void sequenceWrapAroundIsHandled() {
        // Put mySequence near the 32-bit wrap point.
        session.mySequence = Integer.MAX_VALUE - 100;
        fillReceiveBuffer(5000);

        established.receive(session, buildSegment());
        final int inFlight = session.nextSegmentMark;

        final ByteBuffer ack = buildSegment();
        final TcpHeader header = new TcpHeader();
        header.ack = true;
        // Acknowledgment number wraps past Integer.MAX_VALUE.
        header.acknowledgmentNumber = session.mySequence + inFlight;
        header.sequenceNumber = session.vmSequence;
        header.write(ack);
        ack.flip();

        assertTrue(isForward(established.send(session, ack)));
        assertEquals(inFlight, session.mySequence - (Integer.MAX_VALUE - 100));
        assertEquals(5000 - inFlight, session.receiveBuffer.position());
    }

    @Test
    void payloadStreamIsContiguousAcrossSegments() {
        final byte[] expected = new byte[4500];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (i * 7);
        }
        session.receiveBuffer.put(expected);

        final var received = new java.io.ByteArrayOutputStream();
        boolean hasPayload;
        do {
            final ByteBuffer segment = buildSegment();
            if (!isForward(established.receive(session, segment))) {
                break;
            }
            // A bare ACK (no PSH) means everything buffered has been handed to the wire.
            hasPayload = session.header.psh;
            if (hasPayload) {
                try {
                    received.write(payloadOf(segment));
                } catch (final java.io.IOException e) {
                    throw new AssertionError(e);
                }
            }
        } while (hasPayload && received.size() < expected.length);

        assertArrayEquals(expected, received.toByteArray());
    }

    @Test
    void finOnlySegmentWhenFinishingWithNoData() {
        session.close(); // ESTABLISHED -> FINISH

        assertTrue(isForward(established.receive(session, buildSegment())));
        assertTrue(session.header.fin);
        assertFalse(session.header.psh);
    }
}
