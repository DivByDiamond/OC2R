package li.cil.oc2.common.blockentity.computer.contraption;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;

public final class ComputerContraptionHandler {
    private static final ConcurrentHashMap<UUID, ComputerBlockEntity> PRIMARY_BY_DEVICE_ID =
            new ConcurrentHashMap<>();
    private static final long VIRTUAL_POSITION_THRESHOLD = 1_000_000L;

    public static boolean isContraptionVirtualClone(final ComputerBlockEntity computer) {
        final var pos = computer.getBlockPos();
        return Math.abs(pos.getX()) > VIRTUAL_POSITION_THRESHOLD
                || Math.abs(pos.getZ()) > VIRTUAL_POSITION_THRESHOLD;
    }

    @Nullable
    public static ComputerBlockEntity getPrimaryForContraptionRendering(
            final ComputerBlockEntity computer) {
        if (!isContraptionVirtualClone(computer)) {
            return computer;
        }
        final ComputerBlockEntity primary = PRIMARY_BY_DEVICE_ID.get(computer.getDeviceId());
        if (primary != null && !primary.isRemoved()) {
            return primary;
        }
        return computer;
    }

    public static void registerInClientRegistry(final ComputerBlockEntity computer) {
        var level = computer.getLevel();
        if (level == null || !level.isClientSide()) return;
        if (isContraptionVirtualClone(computer)) return;
        PRIMARY_BY_DEVICE_ID.put(computer.getDeviceId(), computer);
    }

    public static void unregisterFromClientRegistry(final ComputerBlockEntity computer) {
        var level = computer.getLevel();
        if (level == null || !level.isClientSide()) return;
        PRIMARY_BY_DEVICE_ID.remove(computer.getDeviceId(), computer);
    }
}