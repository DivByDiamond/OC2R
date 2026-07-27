package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.TransportMessage;
import li.cil.oc2.api.inet.layer.TransportLayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

final class TcpUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private TcpUtils() {}

    static SessionActions prepareTCPSegment(
            final TransportMessage message, final StreamSessionImpl stream) {
        final ByteBuffer data = message.getData();
        final StreamSessionDiscriminator discriminator = stream.getDiscriminator();
        final int position = data.position();
        final int limit = data.limit();
        data.putShort(discriminator.getDstPort());
        data.putShort(discriminator.getSrcPort());
        final SessionActions recv = stream.receive(data);
        switch (recv) {
            case DROP, IGNORE -> {
                data.position(position);
                data.limit(limit);
                return recv;
            }
            case FORWARD -> {
                data.position(position);
                final short checksum =
                        Rfc1071Checksum.transportRfc1071Checksum(
                                data,
                                discriminator.getDstIpAddress(),
                                discriminator.getSrcIpAddress(),
                                TransportLayer.PROTOCOL_TCP);
                data.putShort(position + 16, checksum);
                data.position(position);
                message.updateIpv4(
                        discriminator.getDstIpAddress(), discriminator.getSrcIpAddress());
                LOGGER.trace("Prepared TCP packet to receive {}", stream.getHeader());
                return SessionActions.FORWARD;
            }
            default -> throw new IllegalStateException();
        }
    }
}
