package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class EstablishedState extends TcpState {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
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
    SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        final boolean correct = header.read(segment);
        if (!correct) {
            LOGGER.trace("Got invalid TCP header");
            return SessionActions.IGNORE;
        }
        if (header.syn) {
            LOGGER.trace("Got syn on established connection");
            return SessionActions.IGNORE;
        }
        if (header.sequenceNumber != session.vmSequence) {
            LOGGER.trace(
                    "VM sent invalid sequence number (expected {}, got {})",
                    session.vmSequence,
                    header.sequenceNumber);
            return SessionActions.IGNORE;
        }
        final int length = segment.remaining();
        if (header.psh && length > session.computeWindow()) {
            LOGGER.info("Received length > window size");
            return SessionActions.IGNORE;
        }
        if (header.ack) {
            if (header.acknowledgmentNumber != (session.mySequence + session.nextSegmentMark)) {
                LOGGER.trace(
                        "VM acked wrong number (expected {}, got {})",
                        session.mySequence,
                        header.acknowledgmentNumber);
                return SessionActions.IGNORE;
            }
            if (header.acknowledgmentNumber == (session.mySequence + session.nextSegmentMark)) {
                final ByteBuffer receiveBuffer = session.receiveBuffer;
                final int newPosition = receiveBuffer.position() - session.nextSegmentMark;
                receiveBuffer.position(session.nextSegmentMark);
                receiveBuffer.compact();
                receiveBuffer.position(newPosition);
                receiveBuffer.limit(receiveBuffer.capacity());
                session.mySequence += session.nextSegmentMark;
                session.nextSegmentMark = 0;
            }
        }
        session.vmWindow = header.window;
        if (header.psh) {
            session.vmSequence += length;
            final ByteBuffer sendBuffer = session.sendBuffer;
            sendBuffer.compact();
            sendBuffer.put(segment);
            sendBuffer.flip();
            session.needsAcknowledgment = true;
        }
        if (header.fin) {
            ++session.vmSequence;
            session.state = TcpStates.FINISH;
        }
        return SessionActions.FORWARD;
    }

    @Override
    Session.States toSessionState() {
        return Session.States.ESTABLISHED;
    }
}