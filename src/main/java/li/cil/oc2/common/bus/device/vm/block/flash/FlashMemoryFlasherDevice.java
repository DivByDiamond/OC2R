package li.cil.oc2.common.bus.device.vm.block.flash;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CompletableFuture;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.item.storage.AbstractStorageItem;
import li.cil.oc2.common.item.storage.HardDriveItem;
import li.cil.oc2.common.item.storage.flash.FlashMemoryItem;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FlashMemoryFlasherDevice<T extends BlockEntity & FlashMemoryFlasherContainer>
        extends AbstractBlockStorageDevice<BlockDevice, T> {
    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE =
            ByteBufferBlockDevice.create(0, false);

    public FlashMemoryFlasherDevice(final T container) {
        super(container, false);
    }

    public boolean isMounted() {
        return device != null;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        synchronized (identity) {
            if (identity.isWriting()) return VMDeviceLoadResult.fail();
            return super.mount(context);
        }
    }

    @Override
    public void unmount() {
        synchronized (identity) {
            super.unmount();
        }
    }

    public void updateBlockDevice(final CompoundTag tag) {
        joinOpenJob();

        if (device != null) {
            try {
                device.setBlock(EMPTY_BLOCK_DEVICE);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        if (blobHandle != null) {
            BlobStorage.closeAsync(blobHandle).join();
            blobHandle = null;
        }

        importFromItemStack(tag);
        if (device == null) return;

        setOpenJob(
                createBlockDevice()
                        .thenAcceptAsync(
                                blockDevice -> {
                                    try {
                                        device.setBlock(blockDevice);
                                    } catch (final IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                },
                                WORKERS));
    }

    public void removeBlockDevice() {
        joinOpenJob();

        if (device != null) {
            try {
                device.setBlock(EMPTY_BLOCK_DEVICE);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        if (blobHandle != null) {
            BlobStorage.closeAsync(blobHandle).join();
            blobHandle = null;
        }
    }

    @Override
    protected CompletableFuture<BlockDevice> createBlockDevice() {
        final ItemStack stack = identity.getDiskItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractStorageItem)
                || !(stack.getItem() instanceof FlashMemoryItem || stack.getItem() instanceof HardDriveItem)) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        final int capacity = ((AbstractStorageItem) stack.getItem()).getCapacity(stack);
        if (capacity <= 0) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        blobHandle = BlobStorage.validateHandle(blobHandle);
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        final FileChannel channel = BlobStorage.getOrOpenAsync(blobHandle).join();
                        final MappedByteBuffer buffer =
                                channel.map(FileChannel.MapMode.READ_WRITE, 0, capacity);
                        return ByteBufferBlockDevice.wrap(buffer, false);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                WORKERS);
    }

    @Override
    protected void handleDataAccess() {
        identity.handleDataAccess();
    }
}