package li.cil.oc2.common.vm.device.frame;

import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.bus.device.util.optional.OptionalAddress;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.nbt.CompoundTag;

/** Mount and bounded optional NBT state shared by monitor and projector adapters. */
public final class FramePresentAttachment {
    private final OptionalAddress address = new OptionalAddress();
    private byte[] completed = new byte[0];
    private int status;
    private boolean attached;

    /** Claims a separate aperture without extending or moving framebuffer pixels. */
    // Early failure exposes this field reset to save/unmount before the success assignment.
    @SuppressWarnings("PMD.UnusedAssignment")
    public synchronized boolean mount(final VMContext context, final SimpleFramebufferDevice framebuffer) {
        attached = false;
        final boolean previouslyExposed = address.isPresent() || completed.length != 0 || status != 0;
        if (!context.getMemoryAllocator().claimMemory(framebuffer.getLength() + 4096)) {
            return !previouslyExposed;
        }
        if (!address.claim(context, new FramePresentDevice(framebuffer))) return !previouslyExposed;
        framebuffer.restorePresentation(completed, status);
        attached = true;
        completed = new byte[0];
        status = 0;
        return true;
    }

    /** Captures the completed image before the backing blob is closed. */
    public synchronized void unmount(final SimpleFramebufferDevice framebuffer) {
        if (!attached) return;
        attached = false;
        final FramePublication.Snapshot snapshot = framebuffer.snapshotPresentation();
        completed = snapshot.image();
        status = snapshot.status();
    }

    /** Drops addresses and retained completed images on device disposal. */
    public synchronized void clear() {
        attached = false;
        address.clear();
        completed = new byte[0];
        status = 0;
    }

    /** Saves a coherent completed image; VM CPU/RAM snapshots remain independent. */
    public synchronized CompoundTag save(final SimpleFramebufferDevice framebuffer) {
        final CompoundTag tag = new CompoundTag();
        if (address.isPresent()) tag.putLong("address", address.getAsLong());
        if (attached && framebuffer != null) {
            final FramePublication.Snapshot snapshot = framebuffer.snapshotPresentation();
            tag.putByteArray("completed", snapshot.image());
            tag.putInt("status", snapshot.status());
        } else {
            tag.putByteArray("completed", completed);
            tag.putInt("status", status);
        }
        return tag;
    }

    /** Old saves have no presentation state and resume the legacy protocol. */
    public synchronized void load(final CompoundTag tag) {
        attached = false;
        address.clear();
        //? if >=26.1 {
        /*if (tag.contains("address")) address.set(tag.getLongOr("address", 0L));
        completed = tag.getByteArray("completed").orElse(new byte[0]);
        status = tag.getIntOr("status", 0);
        *///?} else {
        if (tag.contains("address")) address.set(tag.getLong("address"));
        completed = tag.getByteArray("completed");
        status = tag.getInt("status");
        //?}
        if (completed.length > 32 * 1024 * 1024) {
            completed = new byte[0];
            status = 0;
        }
    }
}
