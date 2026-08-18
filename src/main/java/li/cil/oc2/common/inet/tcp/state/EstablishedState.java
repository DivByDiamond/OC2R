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

public final class EstablishedState extends TcpState {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        final ByteBuffer receiveBuffer = session.receiveBuffer;
        if (session.nextSegmentMark == 0) {
            session.nextSegmentMark =
                    Math.min(
                            Math.min(session.vmWindow, receiveBuffer.position()),
                            segment.remaining() - TcpHeader.MIN_HEADER_SIZE_NO_PORTS);
            LOGGER.trace("Next segment mark: {}", session.nextSegmentMark);
        }
        header.urg = false;
        header.syn = false;
        header.rst = false;
        header.ack = true;
        header.sequenceNumber = session.mySequence;
        header.acknowledgmentNumber = session.vmSequence;
        header.maxSegmentSize = -1;
        header.urgentPointer = 0;
        header.psh = session.nextSegmentMark != 0;
        header.window = session.computeWindow();
        if (!header.ack && !header.psh && session.state != TcpStates.FINISH) {
            LOGGER.trace("Established session nothing to send");
            return SessionActions.IGNORE;
        }
        if (header.psh) {
            header.fin = false;
            header.write(segment);

            final int recvPos = receiveBuffer.position();
            final int recvLim = receiveBuffer.limit();
            receiveBuffer.limit(session.nextSegmentMark);
            receiveBuffer.position(0);
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

    private boolean handleAcknowledgment(final StreamSessionImpl session, final TcpHeader header) {
        if (header.acknowledgmentNumber != (session.mySequence + session.nextSegmentMark)) {
            LOGGER.trace(
                    "VM acked wrong number (expected {}, got {})",
                    session.mySequence,
                    header.acknowledgmentNumber);
            return false;
        }
        final ByteBuffer receiveBuffer = session.receiveBuffer;
        final int newPosition = receiveBuffer.position() - session.nextSegmentMark;
        receiveBuffer.position(session.nextSegmentMark);
        receiveBuffer.compact();
        receiveBuffer.position(newPosition);
        receiveBuffer.limit(receiveBuffer.capacity());
        session.mySequence += session.nextSegmentMark;
        session.nextSegmentMark = 0;
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