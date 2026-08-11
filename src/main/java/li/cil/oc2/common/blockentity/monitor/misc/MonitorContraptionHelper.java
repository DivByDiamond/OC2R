package li.cil.oc2.common.blockentity.monitor.misc;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;

public final class MonitorContraptionHelper {
    static final ConcurrentMap<UUID, MonitorBlockEntity> PRIMARY_BY_DEVICE_ID =
            new ConcurrentHashMap<>();
    static final long VIRTUAL_POSITION_THRESHOLD = 1_000_000L;

    public static boolean isContraptionVirtualClone(final MonitorBlockEntity monitor) {
        final var pos = monitor.getBlockPos();
        return Math.abs(pos.getX()) > VIRTUAL_POSITION_THRESHOLD
                || Math.abs(pos.getZ()) > VIRTUAL_POSITION_THRESHOLD;
    }

    @Nullable
    public static MonitorBlockEntity getPrimaryForContraptionRendering(
            final MonitorBlockEntity monitor) {
        if (!isContraptionVirtualClone(monitor)) {
            return monitor;
        }
        final MonitorBlockEntity primary = PRIMARY_BY_DEVICE_ID.get(monitor.getDeviceId());
        if (primary != null && !primary.isRemoved()) {
            return primary;
        }
        return monitor;
    }

    public static void registerInClientRegistry(final MonitorBlockEntity monitor) {
        final var level = monitor.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        if (isContraptionVirtualClone(monitor)) {
            return;
        }
        PRIMARY_BY_DEVICE_ID.put(monitor.getDeviceId(), monitor);
    }

    public static void unregisterFromClientRegistry(final MonitorBlockEntity monitor) {
        final var level = monitor.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        PRIMARY_BY_DEVICE_ID.remove(monitor.getDeviceId(), monitor);
    }
}