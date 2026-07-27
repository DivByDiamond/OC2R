package li.cil.oc2.common.blockentity.projector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

final class ProjectorContraptionHelper {
    static final ConcurrentHashMap<UUID, ProjectorBlockEntity> PRIMARY_BY_DEVICE_ID =
            new ConcurrentHashMap<>();
    static final long VIRTUAL_POSITION_THRESHOLD = 1_000_000L;

    static boolean isContraptionVirtualClone(final ProjectorBlockEntity projector) {
        final var pos = projector.getBlockPos();
        return Math.abs(pos.getX()) > VIRTUAL_POSITION_THRESHOLD
                || Math.abs(pos.getZ()) > VIRTUAL_POSITION_THRESHOLD;
    }

    @Nullable
    static ProjectorBlockEntity getPrimaryForContraptionRendering(
            final ProjectorBlockEntity projector) {
        if (!isContraptionVirtualClone(projector)) {
            return projector;
        }
        final ProjectorBlockEntity primary = PRIMARY_BY_DEVICE_ID.get(projector.deviceId);
        if (primary != null && !primary.isRemoved()) {
            return primary;
        }
        return projector;
    }

    static void registerInClientRegistry(final ProjectorBlockEntity projector) {
        final var level = projector.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        if (isContraptionVirtualClone(projector)) {
            return;
        }
        PRIMARY_BY_DEVICE_ID.put(projector.deviceId, projector);
    }

    static void unregisterFromClientRegistry(final ProjectorBlockEntity projector) {
        final var level = projector.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        PRIMARY_BY_DEVICE_ID.remove(projector.deviceId, projector);
    }
}