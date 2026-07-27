package li.cil.oc2.common.inet.util.checksum;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class Rfc1071ChecksumTest {
    @Test
    public void checksumOfZeroData() {
        final ByteBuffer buffer = ByteBuffer.allocate(0);
        final short checksum = Rfc1071Checksum.rfc1071Checksum(buffer);
        // RFC 1071 checksum of empty data is ~0 = 0xFFFF (signed short -1).
        assertEquals((short) -1, checksum);
    }

    @Test
    public void checksumOfKnownData() {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(0x45000030);
        buffer.putInt(0x00000000);
        buffer.flip();

        final short checksum = Rfc1071Checksum.rfc1071Checksum(buffer);
        assertNotEquals(0, checksum);
    }

    @Test
    public void checksumOfEmptyRange() {
        final ByteBuffer buffer = ByteBuffer.allocate(10);
        final short checksum = Rfc1071Checksum.rfc1071Checksum(buffer, 0);
        // RFC 1071 checksum of empty range is ~0 = 0xFFFF (signed short -1).
        assertEquals((short) -1, checksum);
    }
}
