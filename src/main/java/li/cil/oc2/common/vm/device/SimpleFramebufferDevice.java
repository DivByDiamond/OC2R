package li.cil.oc2.common.vm.device;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.utils.DirectByteBufferUtils;

public final class SimpleFramebufferDevice implements MemoryMappedDevice {

    private final ReentrantLock lock = new ReentrantLock();

    public static final int STRIDE = 2;

    private final int width;
    private final int height;
    private final ByteBuffer buffer;
    private int length;
    private final BitSet dirtyLines;

    public SimpleFramebufferDevice(final int width, final int height, final ByteBuffer buffer) {
        this.width = width;
        this.height = height;
        final long required = (long) width * height * STRIDE;
        if (required > Integer.MAX_VALUE || required > 32L * 1024 * 1024) {
            throw new IllegalArgumentException("Framebuffer too large: " + width + "x" + height);
        }
        this.length = (int) required;

        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("Buffer too small.");
        }

        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.dirtyLines = new BitSet((height + 1) / 2);
        this.dirtyLines.set(0, (height + 1) / 2);
    }

    public void close() {
        lock.lock();
        try {

            length = 0;
            dirtyLines.clear();
            DirectByteBufferUtils.release(buffer);
        
        } finally {
            lock.unlock();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean copyFrame(final ByteBuffer dst) {
        lock.lock();
        try {

            if (length == 0 || dirtyLines.isEmpty()) {
                return false;
            }

            dst.clear();
            final ByteBuffer src = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            src.position(0);
            src.limit(length);
            dst.put(src);
            dst.flip();
            dirtyLines.clear();

        } finally {
            lock.unlock();
        }

        return true;
    }

    @Override
    public int getLength() {
        lock.lock();
        try {
            return length;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long load(final int offset, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
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
    public void store(final int offset, final long value, final int sizeLog2)
            throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            lock.lock();
            try {
                switch (sizeLog2) {
                    case 0 -> buffer.put(offset, (byte) value);
                    case 1 -> buffer.putShort(offset, (short) value);
                    case 2 -> buffer.putInt(offset, (int) value);
                    case 3 -> buffer.putLong(offset, value);
                    default -> throw new IllegalArgumentException();
                }
                setDirty(offset);
            } finally {
                lock.unlock();
            }
        }
    }

    private void setDirty(final int offset) {
        final int pixelY = offset / (width * STRIDE);
        dirtyLines.set(pixelY / 2);
    }

    public boolean hasChanges() {
        lock.lock();
        try {
            return !dirtyLines.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}