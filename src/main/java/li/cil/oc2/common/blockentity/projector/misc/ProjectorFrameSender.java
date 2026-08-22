package li.cil.oc2.common.blockentity.projector.misc;

import static li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice.HEIGHT;
import static li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice.WIDTH;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.projector.ProjectorFramebufferMessage;
import li.cil.oc2.common.network.message.projector.ProjectorRequestFramebufferMessage;
import li.cil.oc2.common.network.util.frame.FrameChunker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class ProjectorFrameSender {

    private static final long WATCHER_TIMEOUT_MS = 2000;
    private static final long KEEP_ALIVE_INTERVAL_MS = 1000;

    private final Map<ServerPlayer, Long> watchers =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
    private final byte[] frameBuffer = new byte[WIDTH * HEIGHT * 2];

    @Nullable private FrameConsumer frameConsumer;
    private long lastKeepAliveSentAt;
    private long lastSentAt;
    private final ProjectorBlockEntity projector;

    public ProjectorFrameSender(final ProjectorBlockEntity projector) {
        this.projector = projector;
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        this.frameConsumer = consumer;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void sendFrame(final ProjectorDevice device) {
        final long now = System.currentTimeMillis();
        if (now - lastSentAt < 1000 / Config.monitorFps) return;
        if (!evictWatchers(now)) return;

        if (!device.copyFrame(ByteBuffer.wrap(frameBuffer))) return;
        lastSentAt = now;

        final BlockPos pos = projector.getBlockPos();
        final int count = FrameChunker.chunkCount(frameBuffer.length);
        for (int i = 0; i < count; i++) {
            final var message = new ProjectorFramebufferMessage(
                    pos, WIDTH, HEIGHT, i, count, FrameChunker.slice(frameBuffer, i));
            for (final ServerPlayer player : watchers.keySet()) {
                NetworkMessages.sendToClient(message, player);
            }
        }
    }

    public void handleWatchedBy(final ServerPlayer player) {
        watchers.put(player, System.currentTimeMillis());
    }

    public void applyChunk(
            final int width,
            final int height,
            final int chunkIndex,
            final int chunkCount,
            final byte[] data) {
        final FrameChunker.Reassembler.CompletedFrame completed =
                reassembler.offer(projector.getBlockPos(), width, height, chunkIndex, chunkCount, data);
        if (completed != null) {
            applyClientFrame(completed.width(), completed.height(), completed.data());
        }
    }

    public void applyClientFrame(final int width, final int height, final byte[] data) {
        if (frameConsumer != null) {
            frameConsumer.processFrame(width, height, ByteBuffer.wrap(data));
        }
    }

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > KEEP_ALIVE_INTERVAL_MS) {
            lastKeepAliveSentAt = now;
            NetworkMessages.sendToServer(new ProjectorRequestFramebufferMessage(projector));
        }
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private boolean evictWatchers(final long now) {
        synchronized (watchers) {
            watchers.entrySet().removeIf(entry -> now - entry.getValue() > WATCHER_TIMEOUT_MS);
            return !watchers.isEmpty();
        }
    }
}
