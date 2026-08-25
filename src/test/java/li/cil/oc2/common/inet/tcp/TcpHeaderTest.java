package li.cil.oc2.common.inet.tcp;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class TcpHeaderTest {
    @Test
    public void readWriteRoundTrip() {
        final TcpHeader header = new TcpHeader();
        header.sequenceNumber = 1000;
        header.acknowledgmentNumber = 500;
        header.syn = true;
        header.window = 65535;

        final ByteBuffer buffer = ByteBuffer.allocate(64);
        header.write(buffer);
        buffer.flip();

        final TcpHeader read = new TcpHeader();
        assertTrue(read.read(buffer));
        assertEquals(1000, read.sequenceNumber);
        assertEquals(500, read.acknowledgmentNumber);
        assertEquals(65535, read.window);
        assertTrue(read.syn);
        assertTrue(read.isConnectionInitiation());
    }

    @Test
    public void connectionInitiation() {
        final TcpHeader header = new TcpHeader();
        header.syn = true;
        assertTrue(header.isConnectionInitiation());
        assertFalse(header.isAcceptanceOrRejectionAcknowledged());

        header.syn = false;
        header.ack = true;
        assertFalse(header.isConnectionInitiation());
        assertTrue(header.isAcceptanceOrRejectionAcknowledged());
    }

    @Test
    public void acceptConnection() {
        final TcpHeader header = new TcpHeader();
        header.syn = true;
        header.ack = true;
        header.sequenceNumber = 100;
        header.acknowledgmentNumber = 200;

        header.acceptConnection(1000, 2000, 65535);
        assertTrue(header.ack);
        assertTrue(header.syn);
        assertEquals(1000, header.sequenceNumber);
        assertEquals(2000, header.acknowledgmentNumber);
        assertEquals(65535, header.window);
    }

    @Test
    public void rejectConnection() {
        final TcpHeader header = new TcpHeader();
        header.syn = true;

        header.rejectConnection(1000, 2000);
        assertTrue(header.rst);
        assertFalse(header.syn);
        assertEquals(1000, header.sequenceNumber);
        assertEquals(2000, header.acknowledgmentNumber);
    }

    @Test
    public void minHeaderSize() {
        assertEquals(16, TcpHeader.MIN_HEADER_SIZE_NO_PORTS);
    }

    @Test
    public void dataOffsetBelowFixedHeaderIsRejected() {
        final ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putInt(0); // sequence
        buffer.putInt(0); // acknowledgment
        // dataOffset nibble = 4 -> header size 16 bytes, below the fixed header
        buffer.put((byte) (4 << 2));
        buffer.put((byte) 0); // flags
        buffer.putShort((short) 0); // window
        buffer.putShort((short) 0); // checksum
        buffer.putShort((short) 0); // urgent pointer
        buffer.putInt(0xDEADBEEF); // payload so remaining() is large enough
        buffer.flip();

        assertFalse(new TcpHeader().read(buffer));
    }

    @Test
    public void unknownOptionWithZeroLengthIsRejected() {
        final ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put((byte) (24 << 2)); // header of 24 bytes: 16 fixed + one option slot
        buffer.put((byte) 0); // flags
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 99); // unknown option type
        buffer.put((byte) 0); // length 0 used to rewind the parser forever
        buffer.putShort((short) 0);
        buffer.flip();

        assertFalse(new TcpHeader().read(buffer));
        assertEquals(0, buffer.position());
    }

    @Test
    public void unknownOptionCrossingDataOffsetIsRejected() {
        final ByteBuffer buffer = ByteBuffer.allocate(28);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put((byte) (24 << 2)); // dataOffset = position + 20 (16 fixed + one option slot)
        buffer.put((byte) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 99); // unknown option at offset 16
        buffer.put((byte) 8); // claims 8 bytes, runs past dataOffset into payload
        buffer.putShort((short) 0);
        buffer.putInt(0xCAFEBABE); // payload
        buffer.flip();

        assertFalse(new TcpHeader().read(buffer));
        assertEquals(0, buffer.position());
    }

    @Test
    public void truncatedUnknownOptionDoesNotThrow() {
        for (int seed = 0; seed < 1000; seed++) {
            final java.util.Random random = new java.util.Random(seed);
            final byte[] garbage = new byte[32];
            random.nextBytes(garbage);
            final ByteBuffer buffer = ByteBuffer.wrap(garbage);
            assertDoesNotThrow(() -> new TcpHeader().read(buffer));
        }
    }
}
