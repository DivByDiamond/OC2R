package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.config.Config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Random;

public class StreamSessionImpl extends SessionBase implements StreamSession {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Random random = new Random();

    private final StreamSessionDiscriminator discriminator;

    final ByteBuffer receiveBuffer = ByteBuffer.allocate(Config.streamBufferSize);
    int vmWindow = 0;
    int nextSegmentMark = 0;

    final ByteBuffer sendBuffer = ByteBuffer.allocate(Config.streamBufferSize);

    int mySequence = random.nextInt();
    int vmSequence;

    final TcpHeader header = new TcpHeader();

    TcpState state = TcpStates.CONNECT;

    boolean needsAcknowledgment = false;

    public StreamSessionImpl(
            final int ipAddress, final short port, final StreamSessionDiscriminator discriminator) {
        super(ipAddress, port);
        this.discriminator = discriminator;
        sendBuffer.limit(0);
    }

    public SessionActions receive(final ByteBuffer segment) {
        return state.receive(this, segment);
    }

    public SessionActions send(final ByteBuffer segment) {
        return state.send(this, segment);
    }

    boolean isNeedsAcknowledgment() {
        return needsAcknowledgment;
    }

    @Override
    public ByteBuffer getReceiveBuffer() {
        if (state == TcpStates.EXPIRED || state == TcpStates.FINISH || state == TcpStates.REJECT) {
            throw new IllegalStateException();
        }
        return receiveBuffer;
    }

    @Override
    public ByteBuffer getSendBuffer() {
        if (state == TcpStates.EXPIRED || state == TcpStates.REJECT) {
            throw new IllegalStateException();
        }
        return sendBuffer;
    }

    @Override
    public StreamSessionDiscriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public void expire() {
        state = TcpStates.EXPIRED;
    }

    @Override
    public void connect() {
        if (state != TcpStates.CONNECT) {
            throw new IllegalStateException();
        }
        state = TcpStates.ACCEPT;
    }

    @Override
    public States getState() {
        return state.toSessionState();
    }

    @Override
    public void close() {
        if (state == TcpStates.ESTABLISHED) {
            state = TcpStates.FINISH;
        } else if (state == TcpStates.CONNECT) {
            state = TcpStates.REJECT;
        } else if (state == TcpStates.ACCEPT) {
            LOGGER.warn("Closing session in ACCEPT state, forcing to REJECT");
            state = TcpStates.REJECT;
        }
    }

    public TcpHeader getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "StreamSession(" + discriminator + ")";
    }

    int computeWindow() {
        return sendBuffer.capacity() - sendBuffer.limit();
    }
}
