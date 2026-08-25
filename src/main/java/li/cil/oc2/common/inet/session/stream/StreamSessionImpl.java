package li.cil.oc2.common.inet.session.stream;

import java.nio.ByteBuffer;
import java.util.Random;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.SessionBase;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpState;
import li.cil.oc2.common.inet.tcp.TcpStates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A TCP stream session on the VM side of the stack. Data travels through two buffers: bytes
 * accepted from the VM are staged in {@link #sendBuffer} until written to the real socket, and
 * bytes read from the real socket are staged in {@link #receiveBuffer} until the VM acknowledges
 * them.
 *
 * <p>All behaviour is delegated to the current {@link TcpState} node in {@link #state}; there
 * {@code receive()} builds a segment to be handed <em>to</em> the VM, while {@code send()}
 * validates and consumes a segment coming <em>from</em> the VM.
 */
public class StreamSessionImpl extends SessionBase implements StreamSession {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Random random = new Random();

    private final StreamSessionDiscriminator discriminator;

    /** Payload read from the real socket, waiting to be delivered to (and ACKed by) the VM. */
    public final ByteBuffer receiveBuffer = ByteBuffer.allocate(Config.streamBufferSize);

    /**
     * Last receive window advertised by the VM: an upper bound on how many buffered bytes may be
     * pushed towards it in a single segment ({@code EstablishedState}).
     */
    public int vmWindow = 0;

    /**
     * Number of leading {@link #receiveBuffer} bytes already sent to the VM but not yet
     * cumulatively acknowledged — the in-flight window. Unlike stop-and-wait, later segments
     * extend this range instead of waiting for a full ACK round ({@code EstablishedState});
     * a cumulative ACK compacts the acknowledged prefix out of the buffer.
     */
    public int nextSegmentMark = 0;

    /** Payload accepted from the VM, queued for writing to the real socket. */
    public final ByteBuffer sendBuffer = ByteBuffer.allocate(Config.streamBufferSize);

    /**
     * Our TCP sequence number for segments sent towards the VM. Starts at a random ISN; the SYN
     * consumed by the handshake and every byte acknowledged by the VM advance it.
     */
    public int mySequence = random.nextInt();

    /**
     * The sequence number we expect from the VM, i.e. the next byte number it is allowed to send.
     * Every inbound segment must carry exactly this value; payload length and FIN increment it.
     */
    public int vmSequence;

    /** Reusable header template filled in when building the next outbound segment. */
    public final TcpHeader header = new TcpHeader();

    /** Current node of the TCP state machine ({@link TcpStates}). */
    public TcpState state = TcpStates.CONNECT;

    /**
     * Set when unacknowledged data from the VM is sitting in {@link #sendBuffer}; used by the
     * session manager's retransmission scan to pick this stream for a renewed ACK segment.
     */
    public boolean needsAcknowledgment = false;

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

    public boolean isNeedsAcknowledgment() {
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

    /**
     * Receive window we advertise to the VM: the free space left in {@link #sendBuffer}, since
     * everything the VM sends is staged there until written to the real socket.
     */
    public int computeWindow() {
        return sendBuffer.capacity() - sendBuffer.limit();
    }
}