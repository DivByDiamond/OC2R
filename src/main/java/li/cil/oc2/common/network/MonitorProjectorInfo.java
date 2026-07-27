package li.cil.oc2.common.network;

import li.cil.oc2.common.network.message.MonitorFramebufferMessage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.annotation.Nullable;

final class MonitorProjectorInfo {
    private static final ExecutorService ENCODER_WORKERS =
            Executors.newCachedThreadPool(
                    r -> {
                        final Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        thread.setName("Monitor Frame Encoder");
                        return thread;
                    });

    MonitorProjectorInfo next, previous;
    private final BlockPos projectorPos;
    private final WeakHashMap<ServerPlayer, Long> players = new WeakHashMap<>();
    private int skipCount;
    @Nullable Supplier<ByteBuffer> nextFrameSupplier;
    @Nullable private Future<?> runningEncode;

    public MonitorProjectorInfo(final BlockPos projectorPos) {
        next = previous = this;
        this.projectorPos = projectorPos;
    }

    public void add(final MonitorProjectorInfo info) {
        info.next = next;
        next.previous = info;
        next = info;
        info.previous = this;
    }

    public void remove() {
        if (previous == null) {
            return;
        }
        previous.next = next;
        next.previous = previous;
        previous = null;
        next = null;
    }

    public void handleWatchedBy(final ServerPlayer player) {
        players.put(player, System.currentTimeMillis());
    }

    public void removeExpiredPlayers() {
        players.entrySet()
                .removeIf(
                        entry ->
                                System.currentTimeMillis() - entry.getValue()
                                        > MonitorLoadBalancer.CACHE_EXPIRES_AFTER);
    }

    public boolean isNoLongerWatched() {
        return players.isEmpty();
    }

    public boolean sendIfReady() {
        if (skipCount > 0) {
            skipCount--;
            return false;
        }
        final boolean isReady =
                !players.isEmpty()
                        && nextFrameSupplier != null
                        && (runningEncode == null || runningEncode.isDone());
        if (isReady) {
            sendAsync();
            updateSkipCount();
        }
        return isReady;
    }

    private void sendAsync() {
        assert nextFrameSupplier != null;
        final Supplier<ByteBuffer> frameSupplier = nextFrameSupplier;
        nextFrameSupplier = null;
        assert runningEncode == null || runningEncode.isDone();
        runningEncode =
                ENCODER_WORKERS.submit(
                        () -> {
                            final ByteBuffer frame = frameSupplier.get();
                            if (frame == null) {
                                return;
                            }
                            final int budgetCost = frame.limit() * players.size();
                            MonitorLoadBalancer.BUDGET.accumulateAndGet(
                                    budgetCost, (budget, cost) -> budget - cost);
                            final MonitorFramebufferMessage message =
                                    new MonitorFramebufferMessage(projectorPos, frame);
                            for (final ServerPlayer player : players.keySet()) {
                                Network.sendToClient(message, player);
                            }
                        });
    }

    private void updateSkipCount() {
        skipCount = 0;
        double closestPlayerDistanceSqr = Double.MAX_VALUE;
        final Vec3 blockCenter = Vec3.atCenterOf(projectorPos);
        for (final ServerPlayer player : players.keySet()) {
            skipCount++;
            final double distance = player.distanceToSqr(blockCenter);
            closestPlayerDistanceSqr = Math.min(closestPlayerDistanceSqr, distance);
        }
        final double closestPlayerDistance = Math.sqrt(closestPlayerDistanceSqr);
        if (closestPlayerDistance > 16) {
            skipCount++;
        }
    }
}
