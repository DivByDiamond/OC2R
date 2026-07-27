package li.cil.oc2.common.network;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class ProjectorLoadBalancer {
    static final long CACHE_EXPIRES_AFTER = 2000;

    private static final Map<ProjectorBlockEntity, ProjectorInfo> PROJECTOR_INFO = new HashMap<>();

    static final AtomicInteger BUDGET = new AtomicInteger(getMaxBudget());

    @Nullable private static ProjectorInfo lastSender;

    public static void updateWatcher(
            final ProjectorBlockEntity projector, final ServerPlayer player) {
        PROJECTOR_INFO
                .computeIfAbsent(projector, ProjectorLoadBalancer::addProjectorInfo)
                .handleWatchedBy(player);
    }

    public static void offerFrame(
            final ProjectorBlockEntity projector, final Supplier<ByteBuffer> messageSupplier) {
        final ProjectorInfo info = PROJECTOR_INFO.get(projector);
        if (info != null) {
            info.nextFrameSupplier = messageSupplier;
        }
    }

    @SubscribeEvent
    public static void handleServerTick(final ServerTickEvent.Pre event) {
        updateCache();

        if (BUDGET.updateAndGet(ProjectorLoadBalancer::replenishBudget) > 0) {
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
        final Iterator<ProjectorInfo> iterator = PROJECTOR_INFO.values().iterator();
        while (iterator.hasNext()) {
            final ProjectorInfo info = iterator.next();
            info.removeExpiredPlayers();
            if (info.isNoLongerWatched()) {
                iterator.remove();
                removeProjectorInfo(info);
            }
        }
    }

    private static ProjectorInfo addProjectorInfo(final ProjectorBlockEntity projector) {
        projector.setRequiresKeyframe();
        final ProjectorInfo info = new ProjectorInfo(projector.getBlockPos());
        if (lastSender == null) {
            lastSender = info;
        } else {
            lastSender.add(info);
        }
        return info;
    }

    private static void removeProjectorInfo(final ProjectorInfo info) {
        if (lastSender == info) {
            if (lastSender.next == lastSender) {
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

        final ProjectorInfo start = lastSender;
        do {
            lastSender = lastSender.next;
            if (lastSender.sendIfReady()) {
                return;
            }
        } while (lastSender != start);
    }
}