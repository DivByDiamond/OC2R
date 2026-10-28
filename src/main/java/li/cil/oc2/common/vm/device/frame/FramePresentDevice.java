package li.cil.oc2.common.vm.device.frame;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.Resettable;

/** Optional v1 completed-frame command aperture, separate from pixel memory. */
public final class FramePresentDevice implements MemoryMappedDevice, Resettable {
    public static final int MAGIC = 0x3150464f;
    public static final int VERSION = 1;
    public static final int BEGIN = 1;
    public static final int COMMIT = 2;
    public static final int ABORT = 3;
    public static final int DISABLE = 4;
    private final SimpleFramebufferDevice pixels;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The companion shares its owning framebuffer for MMIO commands.")
    public FramePresentDevice(final SimpleFramebufferDevice framebuffer) {
        this.pixels = framebuffer;
    }

    /** Returns the associated independently mapped pixel device. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "FDT association requires the exact mapped framebuffer identity.")
    public SimpleFramebufferDevice framebuffer() { return pixels; }

    @Override
    public int getLength() { return 0x20; }

    @Override
    public int getSupportedSizes() { return 1 << 2; }

    @Override
    public long load(final int offset, final int sizeLog2) {
        if (sizeLog2 != 2) return 0;
        return switch (offset) {
            case 0 -> MAGIC;
            case 4 -> VERSION;
            case 12 -> pixels.presentationStatus();
            case 16 -> 1;
            default -> 0;
        };
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) {
        if (offset == 8 && sizeLog2 == 2) pixels.present((int) value);
    }

    @Override
    public void reset() { pixels.present(DISABLE); }
}
