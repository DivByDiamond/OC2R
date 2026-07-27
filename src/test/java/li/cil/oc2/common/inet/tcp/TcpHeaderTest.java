package li.cil.oc2.common.inet.tcp;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class TcpHeaderTest {
    @Test
    public void readWriteRoundTrip() {
        final TcpHeader header = new TcpHeader();
        header.srcPort = 12345;
        header.dstPort = 80;
        header.seqNumber = 1000;
        header.ackNumber = 500;
        header.dataOffset = 5;
        header.flags = TcpHeader.FLAG_SYN | TcpHeader.FLAG_ACK;
        header.windowSize = 65535;

        final ByteBuffer buffer = ByteBuffer.allocate(64);
        header.write(buffer);
        buffer.flip();

        final TcpHeader read = TcpHeader.read(buffer, header.srcPort, header.dstPort);
        assertEquals(12345, read.srcPort);
        assertEquals(80, read.dstPort);
        assertEquals(1000, read.seqNumber);
        assertEquals(500, read.ackNumber);
        assertEquals(5, read.dataOffset);
        assertTrue(read.isConnectionInitiation());
        assertTrue(read.isAcceptanceOrRejectionAcknowledged());
        assertEquals(65535, read.windowSize);
    }

    @Test
    public void connectionInitiation() {
        final TcpHeader header = new TcpHeader();
        header.flags = TcpHeader.FLAG_SYN;
        assertTrue(header.isConnectionInitiation());
        assertFalse(header.isAcceptanceOrRejectionAcknowledged());

        header.flags = TcpHeader.FLAG_ACK;
        assertFalse(header.isConnectionInitiation());
    }

    @Test
    public void acceptConnection() {
        final TcpHeader header = new TcpHeader();
        header.flags = TcpHeader.FLAG_SYN | TcpHeader.FLAG_ACK;
        header.seqNumber = 100;
        header.ackNumber = 200;
        assertTrue(header.isAcceptanceOrRejectionAcknowledged());

        final TcpHeader accepted = header.acceptConnection();
        assertTrue((accepted.flags & TcpHeader.FLAG_ACK) != 0);
    }

    @Test
    public void rejectConnection() {
        final TcpHeader header = new TcpHeader();
        header.flags = TcpHeader.FLAG_SYN;
        final TcpHeader rejected = header.rejectConnection();
        assertTrue((rejected.flags & TcpHeader.FLAG_RST) != 0);
    }

    @Test
    public void minHeaderSize() {
        assertEquals(20, TcpHeader.MIN_HEADER_SIZE_NO_PORTS);
    }
}
