package li.cil.oc2.common.bus.device.vm.block;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.optional.OptionalAddress;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MonitorDevice extends IdentityProxy<BlockEntity> implements VMDevice {
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final BooleanConsumer onMountedChanged;

    @Nullable private SimpleFramebufferDevice device;
    // Resolution of the GPU installed in the hosting computer; 0 means "no GPU on the bus".
    // Updated from CommonDeviceBusController after-device-scan listeners (MonitorGpuLink)
    // before the VM mounts devices, so mount() always sees fresh values.
    private int gpuWidth;
    private int gpuHeight;

    private final OptionalAddress address = new OptionalAddress();
    @Nullable private UUID blobHandle;

    public MonitorDevice(final BlockEntity identity, final BooleanConsumer onMountedChanged) {
        super(identity);
        this.onMountedChanged = onMountedChanged;
    }

    public void setGpuResolution(final int width, final int height) {
        this.gpuWidth = width;
        this.gpuHeight = height;
    }

    public void clearGpuResolution() {
        this.gpuWidth = 0;
        this.gpuHeight = 0;
    }

    /** Framebuffer dimensions once mounted; falls back to the legacy default resolution. */
    public int getWidth() {
        final SimpleFramebufferDevice framebufferDevice = device;
        return framebufferDevice != null ? framebufferDevice.getWidth() : WIDTH;
    }

    /** @see #getWidth() */
    public int getHeight() {
        final SimpleFramebufferDevice framebufferDevice = device;
        return framebufferDevice != null ? framebufferDevice.getHeight() : HEIGHT;
    }

    public boolean hasChanges() {
        final SimpleFramebufferDevice framebufferDevice = device;
        return framebufferDevice != null && framebufferDevice.hasChanges();
    }

    public boolean copyFrame(final ByteBuffer dst) {
        final SimpleFramebufferDevice framebufferDevice = device;
        return framebufferDevice != null && framebufferDevice.copyFrame(dst);
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (gpuWidth <= 0 || gpuHeight <= 0) {
            // No GPU on the bus: the monitor mounts without a framebuffer and stays
            // dark. The rest of the device set (keyboard, ...) is unaffected.
            return VMDeviceLoadResult.success();
        }

        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        assert device != null;
        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        onMountedChanged.accept(true);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        final SimpleFramebufferDevice framebufferDevice = device;
        device = null;
        if (framebufferDevice != null) {
            framebufferDevice.close();
        }

        if (blobHandle != null) {
            BlobStorage.closeAsync(blobHandle).join();
        }

        onMountedChanged.accept(false);
    }

    @Override
    public void dispose() {
        if (blobHandle != null) {
            BlobStorage.deleteAsync(blobHandle);
            blobHandle = null;
        }

        address.clear();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
    }

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return false;
        }

        try {
            device = createFrameBufferDevice();
        } catch (final IOException e) {
            return false;
        }

        return true;
    }

    private SimpleFramebufferDevice createFrameBufferDevice() throws IOException {
        final long required = (long) gpuWidth * gpuHeight * SimpleFramebufferDevice.STRIDE;
        blobHandle = BlobStorage.validateHandle(blobHandle);
        FileChannel channel = BlobStorage.getOrOpenAsync(blobHandle).join();
        if (channel.size() != required) {
            // GPU (or its resolution) changed since the last mount: the stored framebuffer
            // has the wrong size for the new mode, discard it and start with a fresh blob.
            BlobStorage.deleteAsync(blobHandle);
            blobHandle = BlobStorage.validateHandle(null);
            channel = BlobStorage.getOrOpenAsync(blobHandle).join();
        }
        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, required);
        return new SimpleFramebufferDevice(gpuWidth, gpuHeight, buffer);
    }
}