/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import li.cil.sedna.api.device.BlockDevice;

/**
 * A block device that loses power after a byte budget. Everything the guest writes up to the budget
 * reaches the wrapped device; every byte after it is silently discarded, as if the machine had
 * died between two disk writes. Sedna hands a virtio request to the device as one stream per
 * request, so counting bytes counts the 512 byte sectors the guest has issued. A budget that is
 * not a multiple of 512 tears one sector: only its first bytes land.
 */
final class PowerCutDisk implements BlockDevice {
    static final int SECTOR = 512;

    private final BlockDevice delegate;
    private final AtomicLong budget = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong requests = new AtomicLong();
    private final AtomicInteger flushes = new AtomicInteger();
    private volatile boolean cut;
    private volatile long cutOffset = -1;
    private volatile int[] trace = new int[0];
    private volatile boolean tracing;
    private int traced;

    PowerCutDisk(final BlockDevice delegate) {
        this.delegate = delegate;
    }

    /** Lose power once this many bytes have been written; Long.MAX_VALUE never cuts. */
    void setBudgetBytes(final long bytes) {
        budget.set(bytes);
    }

    /** Remember the 4096 byte block of every sector written, in order, for choosing cut points. */
    void startTrace(final int capacity) {
        trace = new int[capacity];
        traced = 0;
        tracing = true;
    }

    synchronized int[] traceBlocks() {
        return java.util.Arrays.copyOf(trace, traced);
    }

    boolean isCut() {
        return cut;
    }

    long cutOffset() {
        return cutOffset;
    }

    /** Whole and partial sectors written so far (a torn sector counts as one). */
    long sectorsWritten() {
        return (written.get() + SECTOR - 1) / SECTOR;
    }

    /** Bytes offered to the device so far, including the ones a cut discarded. */
    long bytesWritten() {
        return written.get();
    }

    long writeRequests() {
        return requests.get();
    }

    int flushes() {
        return flushes.get();
    }

    BlockDevice delegate() {
        return delegate;
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean isReadonly() {
        return delegate.isReadonly();
    }

    @Override
    public long getCapacity() {
        return delegate.getCapacity();
    }

    @Override
    public InputStream getInputStream(final long offset) {
        return delegate.getInputStream(offset);
    }

    @Override
    public OutputStream getOutputStream(final long offset) {
        requests.incrementAndGet();
        return new Cutting(offset, delegate.getOutputStream(offset));
    }

    @Override
    public void flush() {
        flushes.incrementAndGet();
        delegate.flush();
    }

    @Override
    public void close() {
        // The wrapped device outlives this wrapper; the machine under test may swap it back.
    }

    private final class Cutting extends OutputStream {
        private final OutputStream target;
        private long position;

        Cutting(final long offset, final OutputStream target) {
            this.position = offset;
            this.target = target;
        }

        @Override
        public void write(final int value) throws IOException {
            write(new byte[] {(byte) value}, 0, 1);
        }

        @Override
        public void write(final byte[] bytes, final int start, final int length) throws IOException {
            int done = 0;
            while (done < length) {
                // Never let a trace entry or a cut straddle a sector, whatever the chunk size.
                final long before = written.get();
                final int room = SECTOR - (int) (before % SECTOR);
                final int piece = Math.min(length - done, room);
                final long allowed = budget.get() - before;
                final int pass = (int) Math.max(0, Math.min(piece, allowed));
                if (pass > 0) {
                    target.write(bytes, start + done, pass);
                    if (tracing && before % SECTOR == 0) {
                        record((int) (position / OnyxFsImage.BLOCK));
                    }
                }
                if (pass < piece && !cut) {
                    cutOffset = position + pass;
                    cut = true;
                }
                written.addAndGet(piece);
                position += piece;
                done += piece;
            }
        }

        @Override
        public void flush() throws IOException {
            target.flush();
        }

        @Override
        public void close() throws IOException {
            target.close();
        }
    }

    private synchronized void record(final int block) {
        if (traced < trace.length) {
            trace[traced++] = block;
        }
    }
}
