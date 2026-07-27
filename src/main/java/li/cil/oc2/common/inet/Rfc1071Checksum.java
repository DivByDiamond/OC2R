package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;

final class Rfc1071Checksum {
    private static int bufferChecksum(final ByteBuffer buffer, final int size) {
        final int halfSize = size >>> 1;
        int checksum = 0;
        for (int i = 0; i < halfSize; ++i) {
            checksum += Short.toUnsignedInt(buffer.getShort());
        }
        if ((size & 1) != 0) {
            checksum += (buffer.get() << 8) & 0xFFFF;
        }
        return checksum;
    }

    private static short finishChecksum(final int checksum) {
        int result = (checksum >>> 16) + (checksum & 0xFFFF);
        result = (result >>> 16) + (result & 0xFFFF);
        return (short) ~result;
    }

    static short rfc1071Checksum(final ByteBuffer buffer, final int size) {
        final int checksum = bufferChecksum(buffer, size);
        return finishChecksum(checksum);
    }

    static short rfc1071Checksum(final ByteBuffer buffer) {
        return rfc1071Checksum(buffer, buffer.remaining());
    }

    static short transportRfc1071Checksum(
            final ByteBuffer buffer,
            final int srcIpAddress,
            final int dstIpAddress,
            final byte protocol) {
        final int size = buffer.remaining();
        final int checksumPart = bufferChecksum(buffer, size);
        final int checksum =
                checksumPart
                        + Byte.toUnsignedInt(protocol)
                        + size
                        + (srcIpAddress >>> 16)
                        + (srcIpAddress & 0xFFFF)
                        + (dstIpAddress >>> 16)
                        + (dstIpAddress & 0xFFFF);
        return finishChecksum(checksum);
    }
}