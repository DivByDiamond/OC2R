package li.cil.oc2.common.vm.misc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import li.cil.ceres.api.Serialized;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.block.SparseBlockDevice;

/** Per-VM writable root disk; only modified blocks are included in the VM snapshot. */
public final class BuiltinRootFilesystem implements BlockDevice {
    private static final int SHADOW_BLOCK_SIZE = 64 * 1024;

    @Serialized private final SparseBlockDevice overlay;
    @Serialized private String baseImageId;

    public BuiltinRootFilesystem(final InputStream image) throws IOException {
        this(ByteBufferBlockDevice.createFromStream(image, true));
    }

    public BuiltinRootFilesystem(final BlockDevice image) {
        this(image, null);
    }

    public BuiltinRootFilesystem(final BlockDevice image, final String baseImageId) {
        overlay = new SparseBlockDevice(image, false, SHADOW_BLOCK_SIZE);
        this.baseImageId = baseImageId;
    }

    public String getBaseImageId() {
        return baseImageId;
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public long getCapacity() {
        return overlay.getCapacity();
    }

    @Override
    public InputStream getInputStream(final long offset) {
        if (offset < 0 || offset > getCapacity()) throw new IllegalArgumentException("Invalid root disk offset");
        return new FilterInputStream(overlay.getInputStream(offset)) {
            private long position = offset;

            @Override
            public int read() throws IOException {
                final int value = super.read();
                if (value >= 0) position++;
                return value;
            }

            @Override
            public int read(final byte[] bytes, final int start, final int length) throws IOException {
                Objects.checkFromIndexSize(start, length, bytes.length);
                if (length == 0) return 0;
                if (position >= getCapacity()) return -1;
                // Sedna 5.2 bulk reads from an unchanged block can otherwise pass through the
                // next shadow block and return stale image bytes instead of the guest's writes.
                final int boundedLength = Math.min(length,
                        SHADOW_BLOCK_SIZE - (int) (position % SHADOW_BLOCK_SIZE));
                final int count = in.read(bytes, start, boundedLength);
                if (count > 0) position += count;
                return count;
            }

            @Override
            public long skip(final long length) throws IOException {
                final long skipped = super.skip(length);
                position += skipped;
                return skipped;
            }
        };
    }

    @Override
    public OutputStream getOutputStream(final long offset) {
        if (offset < 0 || offset > getCapacity()) throw new IllegalArgumentException("Invalid root disk offset");
        return overlay.getOutputStream(offset);
    }
}
