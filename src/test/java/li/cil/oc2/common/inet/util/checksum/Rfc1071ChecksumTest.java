package li.cil.oc2.common.inet.util.checksum;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class Rfc1071ChecksumTest {
    @Test
    public void checksumOfZeroData() {
        final ByteBuffer buffer = ByteBuffer.allocate(0);
        final int checksum = Rfc1071Checksum.rfc1071Checksum(buffer, 0, 0);
        assertEquals(0, checksum);
    }

    @Test
    public void checksumOfKnownData() {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(0x45000030);
        buffer.putInt(0x00000000);
        buffer.flip();

        final int checksum = Rfc1071Checksum.rfc1071Checksum(buffer, 0, 8);
        assertNotEquals(0, checksum);
    }

    @Test
    public void checksumOfEmptyRange() {
        final ByteBuffer buffer = ByteBuffer.allocate(10);
        final int checksum = Rfc1071Checksum.rfc1071Checksum(buffer, 5, 0);
        assertEquals(0, checksum);
    }
}
