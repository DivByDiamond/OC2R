package li.cil.oc2.common.vm.terminal.render;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

/**
 * One frame's consistent view of the terminal state the row renderers need (§36 M4). Captured
 * once per frame in {@link TerminalRenderer#render} under the geometry seqlock — the buffer
 * arrays and geometry mutate lock-free from the VM/network threads, so reading them per-cell
 * or per-renderer could mix pre/post-resize state within a single frame (the crash class was
 * already bounded by the row guards; this closes the tear class itself).
 *
 * <p>Pure terminal-state data — no Minecraft or GL types — so the capture (and its seqlock
 * semantics) is unit-testable; only the tessellation that consumes a frame is client-only.
 *
 * <p>Known accepted residue: per-cell CONTENT can still be written by a diff application mid-
 * tessellation (rows are shared arrays — closing that needs copy-on-write buffers), and the
 * palette array's elements mutate in place server-side (OSC 4), so a palette entry can change
 * between two cells of one frame. Both are transient one-frame artifacts; the seqlock covers
 * the structural geometry/array-reference class.
 */
public record FrameState(
        boolean useAltBuffer,
        boolean decscnm,
        int width,
        int height,
        int lastRowToDisplay,
        int lastRowToDisplayMax,
        int[] buffer,
        ColorData[] colors,
        ColorData[] colorsBackground,
        byte[] styles,
        int[] altBuffer,
        ColorData[] altColors,
        ColorData[] altColorsBackground,
        byte[] altStyles,
        int[] palette) {

    /** Index of cell (x, row) in the ACTIVE buffer arrays for this frame. */
    public int index(final int x, final int row) {
        return useAltBuffer
                ? x + row * width
                : x + (row + lastRowToDisplay - height) * width;
    }

    /**
     * Capture one consistent frame of {@code terminal}'s state between two even seqlock
     * readings, or null if the geometry moved mid-capture (the caller retries once, then
     * renders the possibly-mixed capture rather than dropping the frame — a tear is one
     * frame and the next frame repaints).
     */
    public static FrameState capture(final Terminal terminal) {
        final int version = terminal.getGeometryVersion();
        if ((version & 1) != 0) {
            return null; // mid-commit
        }
        final FrameState frame = new FrameState(
                terminal.currentPrivateModeState.isAltBufferEnabled(),
                terminal.currentPrivateModeState.DECSCNM,
                terminal.width,
                terminal.height,
                terminal.lastRowToDisplay,
                terminal.lastRowToDisplayMax,
                terminal.buffer,
                terminal.colors,
                terminal.colorsBackground,
                terminal.styles,
                terminal.altBuffer,
                terminal.altColors,
                terminal.altColorsBackground,
                terminal.altStyles,
                terminal.palette256);
        return terminal.getGeometryVersion() == version ? frame : null;
    }
}
