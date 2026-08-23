package li.cil.oc2.common.blockentity.monitor.video;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.monitor.misc.FrameConsumer;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorFramebufferMessage;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorRequestFramebufferMessage;
import li.cil.oc2.common.network.util.frame.FrameChunker;
import li.cil.oc2.common.vm.video.FrameCodec;
import li.cil.oc2.common.vm.video.VideoCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class MonitorVideoController {

    private static final long WATCHER_TIMEOUT_MS = 2000;
    private static final long KEEP_ALIVE_INTERVAL_MS = 1000;

    private final Map<ServerPlayer, Long> watchers =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
    private final FrameCodec codec = new FrameCodec();
    // Lazily resized to the mounted framebuffer's dimensions.
    private byte[] frameBuffer = new byte[0];
    // Resolution of the last frame received on the client; used by renderers that need
    // the aspect ratio before/without a texture. Defaults to the legacy resolution.
    private volatile int clientFrameWidth = MonitorDevice.WIDTH;
    private volatile int clientFrameHeight = MonitorDevice.HEIGHT;

    @Nullable FrameConsumer frameConsumer;
    private long lastKeepAliveSentAt;
    private long lastSentAt;
    private final MonitorBlockEntity monitor;

    public MonitorVideoController(final MonitorBlockEntity monitor) {
        this.monitor = monitor;
    }

    public int getClientFrameWidth() {
        return clientFrameWidth;
    }

    public int getClientFrameHeight() {
        return clientFrameHeight;
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        this.frameConsumer = consumer;
    }

    public void sendFrame(final MonitorDevice device) {
        final long now = System.currentTimeMillis();
        if (now - lastSentAt < 1000 / Config.monitorFps) return;
        if (!evictWatchers(now)) return;

        final int width = device.getWidth();
        final int height = device.getHeight();
        final int frameLength = width * height * 2;
        if (frameBuffer.length != frameLength) {
            frameBuffer = new byte[frameLength];
        }

        final byte[] frame = frameBuffer;
        if (!device.copyFrame(ByteBuffer.wrap(frame))) return;
        lastSentAt = now;

        final VideoCodec configured = VideoCodec.fromId(Config.videoCodec);
        final FrameCodec.EncodedFrame result = codec.encode(configured, frame, width, height);
        final int codecId = result.codec().id;
        final byte[] encoded = result.data();
        final int frameSize = encoded.length;
        final var pos = monitor.getBlockPos();
        final int count = FrameChunker.chunkCount(frameSize);
        for (int i = 0; i < count; i++) {
            sendChunk(pos, codecId, width, height, frameSize, i, count, FrameChunker.slice(encoded, i));
        }
    }

    private void sendChunk(
            final BlockPos pos,
            final int codecId,
            final int width,
            final int height,
            final int frameSize,
            final int chunkIndex,
            final int chunkCount,
            final byte[] data) {
        final var message =
                new MonitorFramebufferMessage(
                        pos, codecId, width, height, frameSize, chunkIndex, chunkCount, data);
        for (final ServerPlayer player : watchers.keySet()) {
            NetworkMessages.sendToClient(message, player);
        }
    }

    public void handleWatchedBy(final ServerPlayer player) {
        watchers.put(player, System.currentTimeMillis());
    }

    public void applyChunk(
            final int codec,
            final int width,
            final int height,
            final int frameSize,
            final int chunkIndex,
            final int chunkCount,
            final byte[] data) {
        final FrameChunker.Reassembler.CompletedFrame completed =
                reassembler.offer(
                        monitor.getBlockPos(), codec, width, height, frameSize, chunkIndex, chunkCount, data);
        if (completed != null) {
            applyClientFrame(
                    VideoCodec.fromId(completed.codec()),
                    completed.width(),
                    completed.height(),
                    completed.data());
        }
    }

    public void applyClientFrame(
            final VideoCodec codecType, final int width, final int height, final byte[] data) {
        clientFrameWidth = width;
        clientFrameHeight = height;
        if (frameConsumer == null) {
            return;
        }
        codec.decode(codecType, data, width, height)
                .ifPresent(decoded -> frameConsumer.processFrame(width, height, ByteBuffer.wrap(decoded)));
    }

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > KEEP_ALIVE_INTERVAL_MS) {
            lastKeepAliveSentAt = now;
            NetworkMessages.sendToServer(new MonitorRequestFramebufferMessage(monitor));
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
