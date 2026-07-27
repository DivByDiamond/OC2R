package li.cil.oc2.common.network.loadbalancer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import li.cil.oc2.common.network.info.MonitorProjectorInfo;

@EventBusSubscriber(modid = API.MOD_ID)
public final class MonitorLoadBalancer {
    static final long CACHE_EXPIRES_AFTER = 2000;

    private static final Map<MonitorBlockEntity, MonitorProjectorInfo> PROJECTOR_INFO =
            new HashMap<>();

    static final AtomicInteger BUDGET = new AtomicInteger(getMaxBudget());

    @Nullable private static MonitorProjectorInfo lastSender;

    public static void updateWatcher(final MonitorBlockEntity monitor, final ServerPlayer player) {
        PROJECTOR_INFO
                .computeIfAbsent(monitor, MonitorLoadBalancer::addProjectorInfo)
                .handleWatchedBy(player);
    }

    public static void offerFrame(
            final MonitorBlockEntity monitor, final Supplier<ByteBuffer> messageSupplier) {
        final MonitorProjectorInfo info = PROJECTOR_INFO.get(monitor);
        if (info != null) {
            info.nextFrameSupplier = messageSupplier;
        }
    }

    @SubscribeEvent
    public static void handleServerTick(final ServerTickEvent.Pre event) {
        updateCache();

        if (BUDGET.updateAndGet(MonitorLoadBalancer::replenishBudget) > 0) {
            sendNextReadyPacket();
        }
    }

    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        PROJECTOR_INFO.clear();
    }

    private static int getMaxBudget() {
        return Config.projectorAverageMaxBytesPerSecond / 2;
    }

    private static int replenishBudget(final int budget) {
        return Math.min(
                getMaxBudget(),
                budget + Math.max(1, Config.projectorAverageMaxBytesPerSecond / 20));
    }

    private static void updateCache() {
        final Iterator<MonitorProjectorInfo> iterator = PROJECTOR_INFO.values().iterator();
        while (iterator.hasNext()) {
            final MonitorProjectorInfo info = iterator.next();
            info.removeExpiredPlayers();
            if (info.isNoLongerWatched()) {
                iterator.remove();
                removeProjectorInfo(info);
            }
        }
    }

    private static MonitorProjectorInfo addProjectorInfo(final MonitorBlockEntity monitor) {
        monitor.video.setRequiresKeyframe();
        final MonitorProjectorInfo info = new MonitorProjectorInfo(monitor.getBlockPos());
        if (lastSender == null) {
            lastSender = info;
        } else {
            lastSender.add(info);
        }
        return info;
    }

    private static void removeProjectorInfo(final MonitorProjectorInfo info) {
        if (java.util.Objects.equals(lastSender, info)) {
            if (java.util.Objects.equals(lastSender.next, lastSender)) {
                lastSender = null;
            } else {
                lastSender = info.next;
            }
        }
        info.remove();
    }

    private static void sendNextReadyPacket() {
        if (lastSender == null) {
            return;
        }

        final MonitorProjectorInfo start = lastSender;
        do {
            lastSender = lastSender.next;
            if (lastSender.sendIfReady()) {
                return;
            }
        } while (!lastSender.equals(start));
    }
}