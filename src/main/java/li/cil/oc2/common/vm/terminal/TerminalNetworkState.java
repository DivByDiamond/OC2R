package li.cil.oc2.common.vm.terminal;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.BitSet;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

/**
 * Network diff dirty-tracking for a {@link Terminal}: which absolute buffer rows changed since
 * the last consume, pending scrollback shift ops, and the 256-color palette revision. Extracted
 * from {@link Terminal} (А1); geometry inputs (height, lastRowToDisplay, alt-buffer state) are
 * passed in by the caller rather than held here, since this class outlives no single geometry.
 */
final class TerminalNetworkState {
    private static final int MAX_PENDING_SHIFT_OPS = 32;
    private static final int SHIFT_OP_FIELDS = 5;

    private BitSet networkDirtyRows;
    private final IntArrayList networkShiftOps = new IntArrayList();
    private final ReentrantLock networkDirtyLock = new ReentrantLock();
    private boolean networkNeedsFullRefresh;
    private int paletteRevision = 0;
    private int lastSentPaletteRevision = -1;

    TerminalNetworkState(final int height) {
        networkDirtyRows = new BitSet(height * Terminal.SCROLL_BACK_COUNT);
    }

    /**
     * Reallocates the dirty-row sink for a new buffer capacity and arms a full refresh.
     *
     * <p>Takes the new screen height rather than the derived row count so this and
     * {@link #recordDirtyScreenRows} always compute the same capacity from the same input —
     * a caller passing a pre-computed row count could drift from {@code height} if the two
     * were ever derived separately.
     */
    void reallocate(final int newHeight) {
        networkDirtyLock.lock();
        try {
            networkDirtyRows = new BitSet(newHeight * Terminal.SCROLL_BACK_COUNT);
            // Shift ops hold row indices of the old geometry; they are meaningless after a
            // reallocation, and the full refresh below repaints everything anyway.
            networkShiftOps.clear();
            networkNeedsFullRefresh = true;
        } finally {
            networkDirtyLock.unlock();
        }
    }

    void markAllDirty() {
        networkDirtyLock.lock();
        try {
            networkNeedsFullRefresh = true;
        } finally {
            networkDirtyLock.unlock();
        }
    }

    void markAllBufferRowsDirty(final int height) {
        networkDirtyLock.lock();
        try {
            networkDirtyRows.set(0, height * Terminal.SCROLL_BACK_COUNT);
            networkNeedsFullRefresh = true;
        } finally {
            networkDirtyLock.unlock();
        }
    }

    /**
     * Converts a screen-row dirty bit mask into absolute buffer rows. Alt-buffer rows are
     * indexed by screen row directly; main-buffer screen row {@code s} lives at absolute
     * buffer row {@code s + lastRowToDisplay - height}.
     */
    void recordDirtyScreenRows(final long mask, final int height, final boolean alt, final int lastRowToDisplay) {
        if (mask == 0) return;
        networkDirtyLock.lock();
        try {
            for (int s = 0; s < height; s++) {
                if ((mask & (1L << s)) == 0) continue;
                final int row = alt ? s : s + lastRowToDisplay - height;
                // Bound is the LOGICAL capacity (height * SCROLL_BACK_COUNT), not
                // networkDirtyRows.size() — a BitSet rounds its capacity up to 64-word
                // multiples, so .size() would admit rows beyond the buffer's real end.
                if (row >= 0 && row < height * Terminal.SCROLL_BACK_COUNT) {
                    networkDirtyRows.set(row);
                }
            }
        } finally {
            networkDirtyLock.unlock();
        }
    }

    void recordShift(
            final int copySrcRow,
            final int copyDstRow,
            final int copyRows,
            final int blankStartRow,
            final int blankRows,
            final int height) {
        networkDirtyLock.lock();
        try {
            if (networkShiftOps.size() >= MAX_PENDING_SHIFT_OPS * SHIFT_OP_FIELDS) {
                // Degraded mode: drop the backlog and mark the WHOLE buffer dirty, not just
                // the visible window — see Terminal.recordNetworkShift javadoc for rationale.
                networkShiftOps.clear();
                networkDirtyRows.set(0, height * Terminal.SCROLL_BACK_COUNT);
                networkNeedsFullRefresh = true;
                return;
            }
            networkShiftOps.add(copySrcRow);
            networkShiftOps.add(copyDstRow);
            networkShiftOps.add(copyRows);
            networkShiftOps.add(blankStartRow);
            networkShiftOps.add(blankRows);
        } finally {
            networkDirtyLock.unlock();
        }
    }

    Terminal.NetworkDirty consume() {
        networkDirtyLock.lock();
        try {
            final boolean full = networkNeedsFullRefresh;
            final int[] rows = networkDirtyRows.stream().toArray();
            final int[] ops = networkShiftOps.toIntArray();
            networkNeedsFullRefresh = false;
            networkDirtyRows.clear();
            // Shift ops hold row indices of the old geometry; they are meaningless after a
            // reallocation, and the full refresh below repaints everything anyway.
            networkShiftOps.clear();
            return new Terminal.NetworkDirty(full, rows, ops);
        } finally {
            networkDirtyLock.unlock();
        }
    }

    void markPaletteDirty() {
        networkDirtyLock.lock();
        try {
            paletteRevision++;
        } finally {
            networkDirtyLock.unlock();
        }
    }

    @SuppressWarnings({
        "PMD.ReturnEmptyCollectionRatherThanNull", // null is a load-bearing sentinel — see Terminal.consumePaletteDirty
        "PMD.UseVarargs" // palette256 is a fixed-size buffer, not a variadic argument list
    })
    @Nullable
    int[] consumePaletteDirty(final boolean force, final int[] palette256) {
        networkDirtyLock.lock();
        try {
            if (!force && paletteRevision == lastSentPaletteRevision) {
                return null;
            }
            lastSentPaletteRevision = paletteRevision;
            return palette256.clone();
        } finally {
            networkDirtyLock.unlock();
        }
    }
}
