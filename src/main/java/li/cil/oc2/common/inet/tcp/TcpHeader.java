package li.cil.oc2.common.inet.tcp;

import java.nio.ByteBuffer;

/**
 * Parsed or pre-built TCP header, excluding the ports: callers write the port pair into the
 * segment buffer before {@link #write(ByteBuffer)} / after reading a fixed 4 bytes themselves,
 * hence the fixed part measures {@link #MIN_HEADER_SIZE_NO_PORTS} = 16 bytes.
 *
 * <p>The checksum is written as zero by {@link #write(ByteBuffer)}; the actual value is filled in
 * later at offset 16 of the finished segment (see {@code TcpUtils}).
 */
public class TcpHeader {
    public static final int MIN_HEADER_SIZE_NO_PORTS = 16;

    private static final byte OPTION_END = 0;
    private static final byte OPTION_NOOP = 1;
    private static final byte OPTION_MAX_SEGMENT_SIZE = 2;

    public int sequenceNumber;
    public int acknowledgmentNumber;
    public boolean urg;
    public boolean ack;
    public boolean psh;
    public boolean rst;
    public boolean syn;
    public boolean fin; // flags
    public int window;
    public int urgentPointer;

    // Options
    public int maxSegmentSize;

    public boolean read(final ByteBuffer data) {
        if (data.remaining() < MIN_HEADER_SIZE_NO_PORTS) {
            return false;
        }
        final int position = data.position();
        sequenceNumber = data.getInt();
        acknowledgmentNumber = data.getInt();
        final int dataOffset = position + ((data.get() >>> 2) & 0x3C) - 4;
        // The offset must cover at least the fixed header and never exceed the buffer,
        // otherwise parsing below rewinds the position of a live network stream.
        if (dataOffset < position + MIN_HEADER_SIZE_NO_PORTS || dataOffset > data.limit()) {
            return false;
        }
        final int flags = Byte.toUnsignedInt(data.get());
        urg = ((flags >>> 5) & 1) == 1;
        ack = ((flags >>> 4) & 1) == 1;
        psh = ((flags >>> 3) & 1) == 1;
        rst = ((flags >>> 2) & 1) == 1;
        syn = ((flags >>> 1) & 1) == 1;
        fin = (flags & 1) == 1;
        window = Short.toUnsignedInt(data.getShort());
        data.getShort(); // checksum
        urgentPointer = Short.toUnsignedInt(data.getShort());

        int mss = -1;

        while (dataOffset > data.position()) {
            final byte type = data.get();
            switch (type) {
                case OPTION_END:
                    data.position(dataOffset);
                    maxSegmentSize = mss;
                    return true;
                case OPTION_NOOP:
                    continue;
                default:
                    break;
            }
            final int size = Byte.toUnsignedInt(data.get());
            // A length below 2 would rewind the position and loop forever on
            // attacker-controlled data; an option must also not cross dataOffset.
            if (size < 2 || data.position() + size - 2 > dataOffset) {
                data.position(position);
                maxSegmentSize = mss;
                return false;
            }
            if (type == OPTION_MAX_SEGMENT_SIZE) {
                if (size != 4) {
                    data.position(position);
                    maxSegmentSize = mss;
                    return false;
                }
                mss = Short.toUnsignedInt(data.getShort());
            } else {
                // Skip unknown option
                data.position(data.position() + size - 2);
            }
        }
        data.position(dataOffset);
        maxSegmentSize = mss;
        return true;
    }

    private int bool2int(boolean value) {
        return value ? 1 : 0;
    }

    public void write(final ByteBuffer data) {
        data.putInt(sequenceNumber);
        data.putInt(acknowledgmentNumber);
        final int headerLength = 4 + MIN_HEADER_SIZE_NO_PORTS + (maxSegmentSize == -1 ? 0 : 4);
        data.put((byte) (headerLength << 2));
        final int flags =
                (bool2int(urg) << 5)
                        | (bool2int(ack) << 4)
                        | (bool2int(psh) << 3)
                        | (bool2int(rst) << 2)
                        | (bool2int(syn) << 1)
                        | bool2int(fin);
        data.put((byte) flags);
        data.putShort((short) window);
        data.putShort((short) 0); // checksum
        data.putShort((short) urgentPointer);

        // Options
        if (maxSegmentSize != -1) {
            data.put(OPTION_MAX_SEGMENT_SIZE);
            data.put((byte) 4);
            data.putShort((short) maxSegmentSize);
        }
    }

    /** Whether the flags describe a bare SYN (connection initiation) with no other flag set. */
    public boolean isConnectionInitiation() {
        return syn && !urg && !ack && !psh && !rst && !fin;
    }

    /** Rewrites the header in place into a SYN-ACK with the given sequence/window values. */
    public void acceptConnection(final int sequence, final int acknowledgment, final int window) {
        sequenceNumber = sequence;
        acknowledgmentNumber = acknowledgment;
        urg = false;
        ack = true;
        psh = false;
        rst = false;
        syn = true;
        fin = false;
        this.window = window;
        urgentPointer = 0;
        maxSegmentSize = -1;
    }

    /** Whether the flags describe a bare ACK, i.e. the final segment of the 3-way handshake. */
    public boolean isAcceptanceOrRejectionAcknowledged() {
        return !syn && !urg && ack && !psh && !rst && !fin;
    }

    /** Rewrites the header in place into a RST with the given sequence/window values. */
    public void rejectConnection(final int sequence, final int acknowledgment) {
        sequenceNumber = sequence;
        acknowledgmentNumber = acknowledgment;
        urg = false;
        ack = true;
        psh = false;
        rst = true;
        syn = false;
        fin = false;
        window = 0;
        urgentPointer = 0;
        maxSegmentSize = -1;
    }

    @Override
    public String toString() {
        return "TcpHeader{"
                + "sequenceNumber="
                + sequenceNumber
                + ", acknowledgmentNumber="
                + acknowledgmentNumber
                + ", urg="
                + urg
                + ", ack="
                + ack
                + ", psh="
                + psh
                + ", rst="
                + rst
                + ", syn="
                + syn
                + ", fin="
                + fin
                + ", window="
                + window
                + ", urgentPointer="
                + urgentPointer
                + ", maxSegmentSize="
                + maxSegmentSize
                + '}';
    }
}