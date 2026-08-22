package li.cil.oc2.common.blockentity.monitor.video;

import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.HEIGHT;
import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.WIDTH;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
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
import net.minecraft.server.level.ServerPlayer;

public final class MonitorVideoController {

    private static final long WATCHER_TIMEOUT_MS = 2000;
    private static final long KEEP_ALIVE_INTERVAL_MS = 1000;

    private final Map<ServerPlayer, Long> watchers =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();

    @Nullable FrameConsumer frameConsumer;
    private long lastKeepAliveSentAt;
    private long lastSentAt;
    private final MonitorBlockEntity monitor;

    public MonitorVideoController(final MonitorBlockEntity monitor) {
        this.monitor = monitor;
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        this.frameConsumer = consumer;
    }

    public void sendFrame(final MonitorDevice device) {
        final long now = System.currentTimeMillis();
        if (now - lastSentAt < 1000 / Config.monitorFps) return;
        if (!evictWatchers(now)) return;

        final byte[] frame = new byte[WIDTH * HEIGHT * 2];
        final ByteBuffer buffer = ByteBuffer.wrap(frame);
        if (!device.copyFrame(buffer)) return;
        lastSentAt = now;

        final var pos = monitor.getBlockPos();
        final int count = FrameChunker.chunkCount(frame.length);
        for (int i = 0; i < count; i++) {
            final var message = new MonitorFramebufferMessage(
                    pos, WIDTH, HEIGHT, i, count, FrameChunker.slice(frame, i));
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
                reassembler.offer(monitor.getBlockPos(), width, height, chunkIndex, chunkCount, data);
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
            NetworkMessages.sendToServer(new MonitorRequestFramebufferMessage(monitor));
        }
    }

    private boolean evictWatchers(final long now) {
        synchronized (watchers) {
            final Iterator<Map.Entry<ServerPlayer, Long>> iterator =
                    watchers.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue() > WATCHER_TIMEOUT_MS) {
                    iterator.remove();
                }
            }
            return !watchers.isEmpty();
        }
    }
}
