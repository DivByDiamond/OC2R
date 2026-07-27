package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.*;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.layer.TransportLayer;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.api.inet.session.StreamSession;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;

public final class DefaultTransportLayer implements TransportLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final SessionLayer sessionLayer;
    private final SessionReceiver receiver = new SessionReceiver();
    private final SessionManager sessionManager;
    private final IcmpHandler icmpHandler = new IcmpHandler();
    private final SendHandler sendHandler;

    public DefaultTransportLayer(final SessionLayer sessionLayer) {
        this.sessionLayer = sessionLayer;
        this.sessionManager = new SessionManager(sessionLayer);
        this.sendHandler = new SendHandler(sessionLayer, sessionManager, icmpHandler);
    }

    @Override
    public byte receiveTransportMessage(final TransportMessage message) {
        sessionManager.processSessionExpirationQueue();
        while (true) {
            if (sendHandler.rejectedStream != null) {
                LOGGER.trace("Rejecting stream {}", sendHandler.rejectedStream.getDiscriminator());
                final SessionActions success =
                        TcpUtils.prepareTCPSegment(message, sendHandler.rejectedStream);
                assert success == SessionActions.FORWARD;
                sessionManager.closeSession(sendHandler.rejectedStream);
                sendHandler.rejectedStream = null;
                return PROTOCOL_TCP;
            }
            if (icmpHandler.consume(message)) return PROTOCOL_ICMP;
            if (sendHandler.streamToAck != null) {
                final StreamSessionImpl stream = sendHandler.streamToAck;
                sendHandler.streamToAck = null;
                sessionManager.updateSession(stream);
                switch (TcpUtils.prepareTCPSegment(message, stream)) {
                    case FORWARD -> {
                        if (stream.isClosed()) sessionManager.closeSession(stream);
                        return PROTOCOL_TCP;
                    }
                    case DROP -> sessionManager.closeSession(stream);
                }
            }
            receiver.prepare(message.getData());
            sessionLayer.receiveSession(receiver);
            final SessionBase session = receiver.session;
            if (session == null) return PROTOCOL_NONE;
            sessionManager.updateSession(session);
            if (session instanceof EchoSession) {
                final byte result = handleEchoSession((EchoSessionImpl) session, message);
                if (result != PROTOCOL_NONE) return result;
            } else if (session instanceof DatagramSession) {
                final byte result = handleDatagramSession((DatagramSessionImpl) session, message);
                if (result != PROTOCOL_NONE) return result;
            } else if (session instanceof StreamSession) {
                final StreamSessionImpl streamSession = (StreamSessionImpl) session;
                switch (TcpUtils.prepareTCPSegment(message, streamSession)) {
                    case FORWARD -> {
                        if (streamSession.isClosed()) sessionManager.closeSession(streamSession);
                        return PROTOCOL_TCP;
                    }
                    case DROP -> sessionManager.closeSession(streamSession);
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private byte handleEchoSession(
            final EchoSessionImpl echoSession, final TransportMessage message) {
        switch (echoSession.getState()) {
            case FINISH:
                sessionManager.closeSession(echoSession);
                return PROTOCOL_NONE;
            case ESTABLISHED:
                final EchoSessionDiscriminator discriminator = echoSession.getDiscriminator();
                final ByteBuffer buffer = receiver.getBuffer();
                final int position = buffer.position();
                buffer.putShort(position + 4, discriminator.getIdentity());
                buffer.putShort(position + 6, (short) echoSession.getSequenceNumber());
                icmpHandler.prepareIcmpHeader(buffer, IcmpHandler.ICMP_TYPE_ECHO_REPLY, (byte) 0);
                message.updateIpv4(
                        discriminator.getDstIpAddress(), discriminator.getSrcIpAddress());
                return PROTOCOL_ICMP;
            default:
                throw new IllegalStateException();
        }
    }

    private byte handleDatagramSession(
            final DatagramSessionImpl datagramSession, final TransportMessage message) {
        switch (datagramSession.getState()) {
            case FINISH:
                sessionManager.closeSession(datagramSession);
                return PROTOCOL_NONE;
            case ESTABLISHED:
                final DatagramSessionDiscriminator discriminator =
                        datagramSession.getDiscriminator();
                final ByteBuffer buffer = receiver.getBuffer();
                final int position = buffer.position();
                buffer.putShort(position, discriminator.getDstPort());
                buffer.putShort(position + 2, discriminator.getSrcPort());
                buffer.putShort(position + 4, (short) buffer.remaining());
                buffer.putShort(position + 6, (short) 0);
                final short checksum =
                        Rfc1071Checksum.transportRfc1071Checksum(
                                buffer,
                                discriminator.getDstIpAddress(),
                                discriminator.getSrcIpAddress(),
                                PROTOCOL_UDP);
                buffer.putShort(position + 6, checksum);
                buffer.position(position);
                message.updateIpv4(
                        discriminator.getDstIpAddress(), discriminator.getSrcIpAddress());
                return PROTOCOL_UDP;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public Optional<Tag> onSave() {
        return sessionLayer
                .onSave()
                .map(
                        sessionLayerState -> {
                            final CompoundTag transportLayerState = new CompoundTag();
                            transportLayerState.put(SessionLayer.LAYER_NAME, sessionLayerState);
                            return transportLayerState;
                        });
    }

    @Override
    public void onStop() {
        for (final SessionBase session : sessionManager.getSessions().values()) {
            session.expire();
            sessionLayer.sendSession(session, null);
            sessionManager.closeSession(session);
        }
        sessionLayer.onStop();
    }

    @Override
    public void sendTransportMessage(final byte protocol, final TransportMessage message) {
        sessionManager.processSessionExpirationQueue();
        final int srcIpAddress = message.getSrcIpv4Address();
        final int dstIpAddress = message.getDstIpv4Address();
        final ByteBuffer data = message.getData();
        switch (protocol) {
            case PROTOCOL_ICMP ->
                    sendHandler.sendIcmpMessage(data, srcIpAddress, dstIpAddress, message);
            case PROTOCOL_UDP -> sendHandler.sendUdpMessage(data, srcIpAddress, dstIpAddress);
            case PROTOCOL_TCP -> sendHandler.sendTcpMessage(data, srcIpAddress, dstIpAddress);
        }
    }
}
