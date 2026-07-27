package li.cil.oc2.common.bus.device.vm.block;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.device.PciRootPortDevice;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PciCardCageDevice extends IdentityProxy<BlockEntity> implements VMDevice {
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    public static final int WINDOW_SIZE = 16 * 1024 * 1024;

    private final BooleanConsumer onMountedChanged;

    @Nullable private PciRootPortDevice device;

    private final OptionalAddress address = new OptionalAddress();
    @Nullable private UUID blobHandle;

    public PciCardCageDevice(final BlockEntity identity, final BooleanConsumer onMountedChanged) {
        super(identity);
        this.onMountedChanged = onMountedChanged;
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
        final PciRootPortDevice pciRootPortDevice = device;
        device = null;
        if (pciRootPortDevice != null) {
            pciRootPortDevice.close();
        }

        if (blobHandle != null) {
            BlobStorage.closeAsync(blobHandle).join();
        }

        onMountedChanged.accept(false);
    }

    @Override
    public void dispose() {
        if (blobHandle != null) {
            BlobStorage.deleteAsync(blobHandle).join();
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
            device = createPciRootPortDevice();
        } catch (final IOException e) {
            return false;
        }

        return true;
    }

    private PciRootPortDevice createPciRootPortDevice() throws IOException {
        blobHandle = BlobStorage.validateHandle(blobHandle);
        final FileChannel channel = BlobStorage.getOrOpenAsync(blobHandle).join();
        final MappedByteBuffer buffer =
                channel.map(FileChannel.MapMode.READ_WRITE, 0, WINDOW_SIZE * 2);
        return new PciRootPortDevice(WINDOW_SIZE, buffer);
    }
}