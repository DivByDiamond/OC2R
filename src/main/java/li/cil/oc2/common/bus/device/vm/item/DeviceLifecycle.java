package li.cil.oc2.common.bus.device.vm.item;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;

final class DeviceLifecycle {
    static final ExecutorService WORKERS =
            Executors.newCachedThreadPool(
                    r -> {
                        final Thread thread = new Thread(r, "Block Device Initializer");
                        thread.setDaemon(false);
                        return thread;
                    });

    VirtIOBlockDevice device;
    private CompletableFuture<Void> openJob;

    boolean allocate(
            final VMContext context,
            final boolean readonly,
            final CompletableFuture<? extends BlockDevice> blockFuture,
            final Runnable onDataAccess) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return false;
        }

        device = new VirtIOBlockDevice(context.getMemoryMap(), readonly);

        initBlock(blockFuture, onDataAccess);

        return true;
    }

    void close() {
        joinOpenJob();

        if (device == null) {
            return;
        }

        try {
            device.close();
        } catch (final IOException ignored) {
            // logged in caller
        }

        device = null;
    }

    void joinOpenJob() {
        if (openJob != null) {
            try {
                openJob.join();
            } catch (final CompletionException ignored) {
                // logged in caller
            } finally {
                openJob = null;
            }
        }
    }

    void setOpenJob(final CompletableFuture<Void> job) {
        joinOpenJob();
        openJob = job;
    }

    private void initBlock(
            final CompletableFuture<? extends BlockDevice> future, final Runnable onDataAccess) {
        joinOpenJob();
        openJob =
                future.thenAcceptAsync(
                        blockDevice -> {
                            try {
                                final ListenableBlockDevice listenableData =
                                        new ListenableBlockDevice(blockDevice);
                                listenableData.accessCallbacks.add(onDataAccess);
                                device.setBlock(listenableData);
                            } catch (final IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        WORKERS);
    }
}