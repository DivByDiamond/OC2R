package li.cil.oc2.common.bus.device.vm.block.misc;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.CompletionException;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ProjectorDevice extends IdentityProxy<BlockEntity> implements VMDevice {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ADDRESS_TAG_NAME = "address";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final BooleanConsumer onMountedChanged;

    @Nullable private SimpleFramebufferDevice device;

    private final OptionalAddress address = new OptionalAddress();
    @Nullable private UUID blobHandle;

    public ProjectorDevice(final BlockEntity identity, final BooleanConsumer onMountedChanged) {
        super(identity);
        this.onMountedChanged = onMountedChanged;
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
            try {
                BlobStorage.closeAsync(blobHandle).join();
            } catch (final CompletionException e) {
                LOGGER.error("Error in close operation for blob: " + blobHandle, e);
            }
        }

        onMountedChanged.accept(false);
    }

    @Override
    public void dispose() {
        if (blobHandle != null) {
            try {
                BlobStorage.deleteAsync(blobHandle).join();
            } catch (final CompletionException e) {
                LOGGER.error("Error in delete operation for blob: " + blobHandle, e);
            }
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
        blobHandle = BlobStorage.validateHandle(blobHandle);
        final FileChannel channel;
        try {
            channel = BlobStorage.getOrOpenAsync(blobHandle).join();
        } catch (final CompletionException e) {
            if (e.getCause() instanceof IOException) {
                throw new IOException("Failed to open blob: " + blobHandle, e);
            }
            throw new IOException("Failed to open blob: " + blobHandle, e);
        }
        final MappedByteBuffer buffer =
                channel.map(
                        FileChannel.MapMode.READ_WRITE,
                        0,
                        WIDTH * HEIGHT * SimpleFramebufferDevice.STRIDE);
        return new SimpleFramebufferDevice(WIDTH, HEIGHT, buffer);
    }
}