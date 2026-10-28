package li.cil.oc2.common.vm.device;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import li.cil.oc2.common.vm.device.frame.FramePublication;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.utils.DirectByteBufferUtils;

public final class SimpleFramebufferDevice implements MemoryMappedDevice {

    private final FramePublication publication = new FramePublication();

    public static final int STRIDE = 2;

    // VRAM budget: caps the shared-memory framebuffer region reserved for a single device.
    private static final long MAX_FRAMEBUFFER_BYTES = 32L * 1024 * 1024;

    private final int width;
    private final int height;
    private final ByteBuffer buffer;
    // Address discovery reads length outside synchronized operations.
    private volatile int length;
    private final BitSet dirtyLines;

    public SimpleFramebufferDevice(final int width, final int height, final ByteBuffer buffer) {
        this.width = width;
        this.height = height;
        this.length = checkedLength(width, height);

        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("Buffer too small.");
        }

        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.dirtyLines = new BitSet((height + 1) / 2);
        this.dirtyLines.set(0, (height + 1) / 2);
    }

    /** Validates dimensions before callers map backing memory or reserve a snapshot. */
    public static int checkedLength(final int width, final int height) {
        final long required = (long) width * height * STRIDE;
        if (width <= 0 || height <= 0 || required > MAX_FRAMEBUFFER_BYTES) {
            throw new IllegalArgumentException("Invalid framebuffer dimensions: " + width + "x" + height);
        }
        return (int) required;
    }

    public synchronized void close() {
        if (length == 0) return;
        // Preserve partial draw pixels for a CPU resuming an interrupted presentation.
        publication.clear();
        length = 0;
        dirtyLines.clear();
        DirectByteBufferUtils.release(buffer);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean copyFrame(final ByteBuffer dst) {
        return copyFrame(dst, false);
    }

    /** Copies a clean framebuffer too when a newly watching client needs a baseline. */
    public synchronized boolean copyFrame(final ByteBuffer dst, final boolean force) {

        if (length == 0 || (!force && dirtyLines.isEmpty())) return false;
        dst.clear();
        final ByteBuffer src = publication.visible(buffer).duplicate().order(ByteOrder.LITTLE_ENDIAN);
        dst.put(src.clear().limit(length));
        dst.flip();
        dirtyLines.clear();
        return true;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public synchronized long load(final int offset, final int sizeLog2) throws MemoryAccessException {
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
    public synchronized void store(final int offset, final long value, final int sizeLog2)
            throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            switch (sizeLog2) {
                case 0 -> buffer.put(offset, (byte) value);
                case 1 -> buffer.putShort(offset, (short) value);
                case 2 -> buffer.putInt(offset, (int) value);
                case 3 -> buffer.putLong(offset, value);
                default -> throw new IllegalArgumentException();
            }
            if (!publication.isNegotiated()) setDirty(offset);
        }
    }

    /** Processes a presentation command without holding any lock between guest stores. */
    public synchronized void present(final int command) {
        if (length != 0 && publication.command(command, buffer, length)) dirtyLines.set(0);
    }

    /** Returns the negotiated/active protocol flags. */
    public synchronized int presentationStatus() { return publication.status(); }

    /** Copies the completed snapshot for persistence; legacy mode has no extra state. */
    public synchronized byte[] savePresentation() { return publication.save(); }

    /** Captures status and an independently owned completed image under one lock. */
    public synchronized FramePublication.Snapshot snapshotPresentation() {
        return publication.snapshot();
    }

    /** Restores only a matching completed image; malformed or old state stays legacy. */
    public synchronized void restorePresentation(final byte[] image, final int status) {
        publication.restore(image, status, length);
    }

    private void setDirty(final int offset) {
        final int pixelY = offset / (width * STRIDE);
        dirtyLines.set(pixelY / 2);
    }

    public synchronized boolean hasChanges() {
        return !dirtyLines.isEmpty();
    }
}
