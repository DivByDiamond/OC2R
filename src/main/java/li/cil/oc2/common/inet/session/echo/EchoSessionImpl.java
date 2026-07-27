package li.cil.oc2.common.inet.session.echo;

import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionBase;

public final class EchoSessionImpl extends DatagramSessionBase implements EchoSession {
    private final EchoSessionDiscriminator discriminator;
    private byte ttl;
    private short sequenceNumber;

    public EchoSessionImpl(
            final int ipAddress, final short port, final EchoSessionDiscriminator discriminator) {
        super(ipAddress, port);
        this.discriminator = discriminator;
    }

    @Override
    public int getTtl() {
        return Byte.toUnsignedInt(ttl);
    }

    public void setTtl(final byte ttl) {
        this.ttl = ttl;
    }

    @Override
    public int getSequenceNumber() {
        return Short.toUnsignedInt(sequenceNumber);
    }

    public void setSequenceNumber(final short sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public EchoSessionDiscriminator getDiscriminator() {
        return discriminator;
    }
}