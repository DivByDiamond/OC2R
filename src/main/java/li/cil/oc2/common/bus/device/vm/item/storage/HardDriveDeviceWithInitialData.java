package li.cil.oc2.common.bus.device.vm.item.storage;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.util.world.BlockLocation;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;

public final class HardDriveDeviceWithInitialData extends HardDriveDevice {
    private final BlockDeviceData base;

    public HardDriveDeviceWithInitialData(
            final ItemStack identity,
            final BlockDeviceData base,
            final boolean readonly,
            final Supplier<Optional<BlockLocation>> location) {
        super(identity, (int) base.getCapacity(), readonly, location);
        this.base = base;
    }

    @Override
    protected CompletableFuture<ByteBufferBlockDevice> createBlockDevice() {
        final boolean isInitializing = blobHandle == null;
        return super.createBlockDevice()
                .thenApplyAsync(
                        device -> {
                            if (isInitializing) {
                                // Resolved here, on the worker thread at mount/copy time, never
                                // while scanning inventories: image I/O is server-only.
                                try {
                                    try (InputStream input = base.getBlockDevice().getInputStream(0);
                                            OutputStream output = device.getOutputStream(0)) {
                                        ByteStreams.copy(input, output);
                                    }
                                } catch (final IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return device;
                        },
                        WORKERS);
    }
}