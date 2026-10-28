package li.cil.oc2.common.vm.device.frame;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;

/** State guarded by the owning framebuffer's monitor, never across guest calls. */
public final class FramePublication {
    private ByteBuffer published;
    private boolean active;
    private boolean negotiated;

    /** Returns the last complete image after negotiation, or live legacy pixels. */
    public ByteBuffer visible(final ByteBuffer draw) { return (negotiated ? published : draw).asReadOnlyBuffer(); }

    /** Reports whether writes are isolated from readers. */
    public boolean isNegotiated() { return negotiated; }

    /** Returns bit zero for an active frame and bit one for negotiation. */
    public int status() { return (active ? 1 : 0) | (isNegotiated() ? 2 : 0); }

    /** Executes BEGIN, COMMIT, ABORT, or DISABLE; true means a new visible frame. */
    public boolean command(final int command, final ByteBuffer draw, final int length) {
        switch (command) {
            case 1 -> begin(draw, length);
            case 2 -> {
                if (active) {
                    copy(draw, published, length);
                    active = false;
                    return true;
                }
            }
            case 3 -> {
                if (negotiated) copy(published, draw, length);
                active = false;
            }
            case 4 -> disable(draw, length);
            default -> { }
        }
        return false;
    }

    private void begin(final ByteBuffer draw, final int length) {
        if (active) return;
        if (!negotiated) {
            if (published == null) published = ByteBuffer.allocate(length);
            copy(draw, published, length);
            negotiated = true;
        }
        active = true;
    }

    /** Independent persistence value; image ownership transfers to the caller. */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "This detached snapshot transfers its newly copied image to the persistence caller.")
    public record Snapshot(byte[] image, int status) { }

    /** Returns completed pixels and flags from the same instant. */
    public Snapshot snapshot() { return new Snapshot(save(), status()); }

    /** Restores completed pixels when explicitly returning to legacy mode. */
    public void disable(final ByteBuffer draw, final int length) {
        if (negotiated) copy(published, draw, length);
        negotiated = false;
        active = false;
    }

    /** Returns an independent bounded persistence snapshot. */
    public byte[] save() { return negotiated ? published.array().clone() : new byte[0]; }

    /** Restores a completed frame without claiming coherence with separately persisted VM RAM. */
    public void restore(final byte[] image, final int status, final int length) {
        if (length > 0 && image.length == length && (status & 2) != 0) {
            published = ByteBuffer.wrap(image.clone());
            negotiated = true;
            active = (status & 1) != 0;
        }
    }

    /** Releases the cached bounded snapshot when the framebuffer closes. */
    public void clear() {
        published = null;
        negotiated = false;
        active = false;
    }

    private static void copy(final ByteBuffer from, final ByteBuffer to, final int length) {
        to.duplicate().clear().put(from.duplicate().clear().limit(length));
    }
}
