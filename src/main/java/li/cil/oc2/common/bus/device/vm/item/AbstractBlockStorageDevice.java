package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.eventbus.Subscribe;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractBlockStorageDevice<TBlock extends BlockDevice, TIdentity>
        extends IdentityProxy<TIdentity> implements VMDevice, ItemDevice {
    protected static final Logger LOGGER = LogManager.getLogger();

    protected static final ExecutorService WORKERS = DeviceLifecycle.WORKERS;

    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    protected final boolean readonly;
    private final DeviceLifecycle lifecycle = new DeviceLifecycle();

    protected VirtIOBlockDevice device;

    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundTag deviceTag;

    @Nullable protected UUID blobHandle;

    protected AbstractBlockStorageDevice(final TIdentity identity, final boolean readonly) {
        super(identity);
        this.readonly = readonly;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!lifecycle.allocate(context, readonly, createBlockDevice(), this::handleDataAccess)) {
            return VMDeviceLoadResult.fail();
        }
        device = lifecycle.device;

        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            return VMDeviceLoadResult.fail();
        }

        context.getEventBus().register(this);

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        lifecycle.close();
        device = null;

        if (blobHandle != null) {
            if (AsyncConfig.SERVER.asyncStorageOperations.get()) {
                BlobStorage.closeAsync(blobHandle)
                        .exceptionally(
                                e -> {
                                    LOGGER.error(
                                            "Error closing blob asynchronously: " + blobHandle, e);
                                    return null;
                                });
            } else {
                try {
                    BlobStorage.closeAsync(blobHandle).join();
                } catch (final java.util.concurrent.CompletionException e) {
                    LOGGER.error("Error in close operation for blob: " + blobHandle, e);
                }
            }
        }
    }

    @Override
    public void dispose() {
        deviceTag = null;
        address.clear();
        interrupt.clear();
    }

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
        if (blobHandle != null) {
            nbt.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
        if (nbt.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = nbt.getUUID(BLOB_HANDLE_TAG_NAME);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }

        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (deviceTag != null) {
            tag.put(DEVICE_TAG_NAME, deviceTag);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            tag.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
        }

        if (tag.contains(DEVICE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceTag = tag.getCompound(DEVICE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
        if (tag.contains(INTERRUPT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(tag.getInt(INTERRUPT_TAG_NAME));
        }
    }

    public static void unmount(final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            final UUID blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
            if (AsyncConfig.SERVER.asyncStorageOperations.get()) {
                BlobStorage.closeAsync(blobHandle)
                        .exceptionally(
                                e -> {
                                    LOGGER.error(
                                            "Error closing blob asynchronously during unmount: "
                                                    + blobHandle,
                                            e);
                                    return null;
                                });
            } else {
                try {
                    BlobStorage.closeAsync(blobHandle).join();
                } catch (final java.util.concurrent.CompletionException e) {
                    LOGGER.error("Error closing blob asynchronously during unmount: " + blobHandle, e);
                }
            }
        }
    }

    @Subscribe
    public void handleResumedRunningEvent(final VMResumedRunningEvent event) {
        lifecycle.joinOpenJob();
    }

    protected final void joinOpenJob() {
        lifecycle.joinOpenJob();
    }

    protected final void setOpenJob(final CompletableFuture<Void> job) {
        lifecycle.setOpenJob(job);
    }

    protected abstract CompletableFuture<TBlock> createBlockDevice();

    protected void handleDataAccess() {}
}