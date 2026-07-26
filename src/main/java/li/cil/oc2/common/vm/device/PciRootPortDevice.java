/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.device;

import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.utils.DirectByteBufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public final class PciRootPortDevice implements MemoryMappedDevice {


    ///////////////////////////////////////////////////////////////

    private final ByteBuffer buffer;
    private int length;

    ///////////////////////////////////////////////////////////////

    public PciRootPortDevice(final int bus_count, final int window_size, final ByteBuffer buffer) {

        length = window_size * 2;
        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("Buffer too small.");
        }

        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);

        this.buffer.putInt(0, 0x12345678);
        this.buffer.putInt(4, 2);
        this.buffer.putInt(8, 0xFF000000);
        this.buffer.putInt(12, 0x00000101);
        this.buffer.putInt(0x10, 0x00000000);
        this.buffer.putInt(0x2C, 0x12345678);


    }

    ///////////////////////////////////////////////////////////////

    public void close() {
        synchronized (buffer) {
            length = 0;
            DirectByteBufferUtils.release(buffer);
        }
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public long load(final int offset, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            System.out.printf("PCI config read: %x %x%n", offset, sizeLog2);
            if (offset == 0x10) {
                long res = buffer.getInt(offset);
                System.out.printf("        00:00.0 BAR0 read    %x%n", res);
                res = res & 0xFFFFF000L;
                System.out.printf("Clipped 00:00.0 BAR0 read to %x%n", res);
                return res;
            }
            return switch (sizeLog2) {
                case 0 -> buffer.get(offset);
                case 1 -> buffer.getShort(offset);
                case 2 -> buffer.getInt(offset);
                case 3 -> buffer.getLong(offset);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return 0;
        }
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            System.out.printf("PCI config write: %x %x %x%n", offset, value, sizeLog2);
            switch (sizeLog2) {
                case 0 -> buffer.put(offset, (byte) value);
                case 1 -> buffer.putShort(offset, (short) value);
                case 2 -> buffer.putInt(offset, (int) value);
                case 3 -> buffer.putLong(offset, value);
                default -> throw new IllegalArgumentException();
            }
        }
    }

    ///////////////////////////////////////////////////////////////

}
