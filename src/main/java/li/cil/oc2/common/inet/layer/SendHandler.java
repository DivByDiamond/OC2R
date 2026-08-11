package li.cil.oc2.common.inet.layer;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.TransportMessage;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.protocol.IcmpHandler;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionBase;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionDiscriminator;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionImpl;
import li.cil.oc2.common.inet.session.echo.EchoSessionDiscriminator;
import li.cil.oc2.common.inet.session.echo.EchoSessionImpl;
import li.cil.oc2.common.inet.session.manager.SessionManager;
import li.cil.oc2.common.inet.session.stream.StreamSessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SendHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int UDP_HEADER_SIZE = 8;
    private static final int MIN_TCP_HEADER_SIZE = 20;

    private final SessionLayer sessionLayer;
    private final SessionManager sessionManager;
    private final IcmpHandler icmpHandler;

    public StreamSessionImpl streamToAck = null;
    public StreamSessionImpl rejectedStream = null;

    public SendHandler(
            final SessionLayer sessionLayer,
            final SessionManager sessionManager,
            final IcmpHandler icmpHandler) {
        this.sessionLayer = sessionLayer;
        this.sessionManager = sessionManager;
        this.icmpHandler = icmpHandler;
    }

    public void sendIcmpMessage(
            final ByteBuffer data,
            final int srcIpAddress,
            final int dstIpAddress,
            final TransportMessage message) {
        if (data.remaining() < IcmpHandler.ICMP_HEADER_SIZE) return;
        final byte type = data.get();
        final byte code = data.get();
        data.getShort();
        if (type != IcmpHandler.ICMP_TYPE_ECHO_REQUEST || code != 0) return;
        final short identity = data.getShort();
        final short sequence = data.getShort();
        final EchoSessionDiscriminator discriminator =
                new EchoSessionDiscriminator(srcIpAddress, dstIpAddress, identity);
        final EchoSessionImpl session =
                sessionManager.getOrCreateSession(
                        discriminator,
                        it -> new EchoSessionImpl(dstIpAddress, IcmpHandler.PORT_ECHO, it));
        if (session == null) {
            icmpHandler.reject(data, srcIpAddress);
        } else {
            session.setSequenceNumber(sequence);
            session.setTtl(message.getTtl());
            sessionLayer.sendSession(session, data);
            sessionSendFinish(session, data, srcIpAddress);
        }
    }

    public void sendUdpMessage(final ByteBuffer data, final int srcIpAddress, final int dstIpAddress) {
        if (data.remaining() < UDP_HEADER_SIZE) return;
        final short srcPort = data.getShort();
        final short dstPort = data.getShort();
        final int datagramLength = Short.toUnsignedInt(data.getShort());
        data.getShort();
        if (data.remaining() + UDP_HEADER_SIZE < datagramLength) return;
        data.limit(data.position() + datagramLength - UDP_HEADER_SIZE);
        final DatagramSessionDiscriminator discriminator =
                new DatagramSessionDiscriminator(srcIpAddress, srcPort, dstIpAddress, dstPort);
        final DatagramSessionImpl session =
                sessionManager.getOrCreateSession(
                        discriminator, it -> new DatagramSessionImpl(dstIpAddress, dstPort, it));
        if (session == null) {
            icmpHandler.reject(data, srcIpAddress);
        } else {
            sessionLayer.sendSession(session, data);
            sessionSendFinish(session, data, srcIpAddress);
        }
    }

    public void sendTcpMessage(final ByteBuffer data, final int srcIpAddress, final int dstIpAddress) {
        if (data.remaining() < MIN_TCP_HEADER_SIZE) return;
        final short srcPort = data.getShort();
        final short dstPort = data.getShort();
        final StreamSessionDiscriminator discriminator =
                new StreamSessionDiscriminator(srcIpAddress, srcPort, dstIpAddress, dstPort);
        final StreamSessionImpl session =
                sessionManager.getOrCreateSession(
                        discriminator, it -> new StreamSessionImpl(dstIpAddress, dstPort, it));
        if (session == null) {
            icmpHandler.reject(data, srcIpAddress);
        } else {
            LOGGER.trace("GOT TCP");
            final SessionActions sendResult = session.send(data);
            if (sendResult == SessionActions.FORWARD) {
                final Session.States state = session.getState();
                if (state == Session.States.NEW || state == Session.States.FINISH) {
                    sessionLayer.sendSession(session, null);
                } else if (state == Session.States.ESTABLISHED) {
                    sessionLayer.sendSession(session, session.getSendBuffer());
                }
                if (state == Session.States.REJECT || state == Session.States.FINISH)
                    rejectedStream = session;
                if (session.isNeedsAcknowledgment()) streamToAck = session;
            } else if (sendResult == SessionActions.DROP) {
                sessionManager.closeSession(session);
            }
        }
    }

    private void sessionSendFinish(
            final DatagramSessionBase session, final ByteBuffer payload, final int srcIpAddress) {
        switch (session.getState()) {
            case NEW -> session.setState(Session.States.ESTABLISHED);
            case REJECT -> icmpHandler.reject(payload, srcIpAddress);
            case FINISH -> sessionManager.closeSession(session);
            case ESTABLISHED -> {}
            default -> throw new IllegalStateException();
        }
    }
}