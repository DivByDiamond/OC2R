package li.cil.oc2.common.inet.tcp.state;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpState;
import li.cil.oc2.common.inet.tcp.TcpStates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Data transfer state, entered after the three-way handshake completed.
 *
 * <ul>
 *   <li>{@code receive}: builds an ACK (or PSH+ACK) segment towards the VM carrying the next
 *       contiguous chunk of {@code receiveBuffer}, bounded by the VM's advertised {@code vmWindow}
 *       minus what is already in flight (sliding window, todo.md §38 П3.1). If there is no new
 *       data and the session is not finishing, a bare ACK is emitted.
 *   <li>{@code send}: validates an inbound segment (sequence number must equal
 *       {@code vmSequence}, payload must fit the advertised window), accepts cumulative ACKs for
 *       any part of the in-flight window, appends PSH payloads to {@code sendBuffer}, advances
 *       sequence numbers, and moves to FINISH on a FIN from the VM.
 * </ul>
 *
 * <p>Sequence bookkeeping: {@code mySequence} is our own sequence number of the first byte still
 * buffered in {@code receiveBuffer}; it advances by the acknowledged amount when the VM sends a
 * cumulative ACK ({@code acknowledgmentNumber - mySequence} must land inside the in-flight
 * window). {@code nextSegmentMark} counts the sent-but-unacknowledged prefix of that buffer;
 * unlike classic stop-and-wait, later segments extend it instead of waiting for a full ACK round.
 * {@code vmSequence} is the next byte number expected from the VM.
 */
public final class EstablishedState extends TcpState {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        final ByteBuffer receiveBuffer = session.receiveBuffer;

        // How much unacknowledged data the VM is still willing to accept, and how much
        // unsent data we have; the segment carries the next contiguous piece starting at
        // sequence number mySequence + nextSegmentMark.
        final int unsentBytes = receiveBuffer.position() - session.nextSegmentMark;
        final int maxPayload = segment.remaining() - TcpHeader.MIN_HEADER_SIZE_NO_PORTS;
        final int windowLeft = session.vmWindow - session.nextSegmentMark;
        final int chunk = Math.min(Math.min(unsentBytes, Math.max(0, windowLeft)), maxPayload);
        if (chunk > 0) {
            LOGGER.trace("Sliding window segment: {} bytes", chunk);
            session.nextSegmentMark += chunk;
        }

        header.urg = false;
        header.syn = false;
        header.rst = false;
        header.ack = true;
        header.sequenceNumber = session.mySequence + session.nextSegmentMark - chunk;
        header.acknowledgmentNumber = session.vmSequence;
        header.maxSegmentSize = -1;
        header.urgentPointer = 0;
        header.psh = chunk > 0;
        header.window = session.computeWindow();
        // No new data: emit a bare ACK (plus FIN when finishing).
        if (header.psh) {
            header.fin = false;
            header.write(segment);

            final int recvPos = receiveBuffer.position();
            final int recvLim = receiveBuffer.limit();
            receiveBuffer.limit(receiveBuffer.position() - unsentBytes + chunk);
            receiveBuffer.position(session.nextSegmentMark - chunk);
            segment.put(receiveBuffer);
            receiveBuffer.limit(recvLim);
            receiveBuffer.position(recvPos);
        } else {
            header.fin = session.state == TcpStates.FINISH;
            header.write(segment);
        }
        segment.flip();
        return SessionActions.FORWARD;
    }

    @Override
    public SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        if (!isValidSegment(session, segment, header)) {
            return SessionActions.IGNORE;
        }
        final int length = segment.remaining();
        session.vmWindow = header.window;
        if (header.psh) {
            handlePush(session, segment, length);
        }
        if (header.fin) {
            ++session.vmSequence;
            session.state = TcpStates.FINISH;
        }
        return SessionActions.FORWARD;
    }

    private boolean isValidSegment(
            final StreamSessionImpl session, final ByteBuffer segment, final TcpHeader header) {
        if (!header.read(segment)) {
            LOGGER.trace("Got invalid TCP header");
            return false;
        }
        if (header.syn) {
            LOGGER.trace("Got syn on established connection");
            return false;
        }
        if (header.sequenceNumber != session.vmSequence) {
            LOGGER.trace(
                    "VM sent invalid sequence number (expected {}, got {})",
                    session.vmSequence,
                    header.sequenceNumber);
            return false;
        }
        final int length = segment.remaining();
        if (header.psh && length > session.computeWindow()) {
            LOGGER.info("Received length > window size");
            return false;
        }
        return !header.ack || handleAcknowledgment(session, header);
    }

    /**
     * Processes a cumulative ACK: any value covering part of (or all of) the in-flight window
     * compacts that much data out of {@code receiveBuffer}. Duplicate ACKs (zero coverage) are
     * valid and change nothing; ACKs beyond the in-flight window are protocol errors.
     */
    private boolean handleAcknowledgment(final StreamSessionImpl session, final TcpHeader header) {
        // Subtraction is wrap-safe: both values live on the same mod-2^32 sequence line.
        final int acknowledgedBytes = header.acknowledgmentNumber - session.mySequence;
        if (acknowledgedBytes < 0 || acknowledgedBytes > session.nextSegmentMark) {
            LOGGER.trace(
                    "VM acked outside the in-flight window (base {}, ack {}, in flight {})",
                    session.mySequence,
                    header.acknowledgmentNumber,
                    session.nextSegmentMark);
            return false;
        }
        if (acknowledgedBytes == 0) {
            return true;
        }
        final ByteBuffer receiveBuffer = session.receiveBuffer;
        final int remainingBytes = receiveBuffer.position() - acknowledgedBytes;
        receiveBuffer.position(acknowledgedBytes);
        receiveBuffer.compact();
        // compact() leaves position at the amount of data moved, which includes stale
        // bytes beyond the valid prefix when the limit is at capacity.
        receiveBuffer.position(remainingBytes);
        session.mySequence += acknowledgedBytes;
        session.nextSegmentMark -= acknowledgedBytes;
        return true;
    }

    private void handlePush(
            final StreamSessionImpl session, final ByteBuffer segment, final int length) {
        session.vmSequence += length;
        final ByteBuffer sendBuffer = session.sendBuffer;
        sendBuffer.compact();
        sendBuffer.put(segment);
        sendBuffer.flip();
        session.needsAcknowledgment = true;
    }

    @Override
    public Session.States toSessionState() {
        return Session.States.ESTABLISHED;
    }
}