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
}
