package li.cil.oc2.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

public final class ClientBlockEntityLookup {
    private static final Logger LOGGER = LogManager.getLogger();

    private static long lastMissingBeLog = 0;
    private static java.util.Set<BlockPos> loggedMissingBe = new java.util.HashSet<>();

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withClientBlockEntityAt(
            final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (type.isInstance(blockEntity)) {
            callback.accept((T) blockEntity);
            return;
        }

        boolean found = false;
        final java.util.List<ClientLevel> allLevels = findAllClientLevels(mc, level);
        for (final ClientLevel otherLevel : allLevels) {
            if (otherLevel == level) {
                continue;
            }
            try {
                final BlockEntity otherBlockEntity = otherLevel.getBlockEntity(pos);
                if (type.isInstance(otherBlockEntity)) {
                    callback.accept((T) otherBlockEntity);
                    found = true;
                    return;
                }
            } catch (final Throwable ignored) {
            }
        }

        if (!found) {
            final long now = System.currentTimeMillis();
            if (now - lastMissingBeLog > 5000) {
                lastMissingBeLog = now;
                loggedMissingBe.clear();
            }
            if (loggedMissingBe.add(pos)) {
                LOGGER.warn(
                        "[ClientBlockEntityLookup] withClientBlockEntityAt: BlockEntity of type {}"
                            + " not found at pos {} in main level or any of {} sub-level(s). Main"
                            + " level class: {}. This is likely a Sable/VS2 ship world the ship's"
                            + " ClientLevel is not reachable via reflection from Minecraft or the"
                            + " main ClientLevel.",
                        type.getSimpleName(),
                        pos,
                        allLevels.size() - 1,
                        level.getClass().getName());
            }
        }
    }

    private static java.util.List<ClientLevel> findAllClientLevels(
            final Minecraft mc, final ClientLevel mainLevel) {
        final java.util.List<ClientLevel> result = new java.util.ArrayList<>();
        result.add(mainLevel);

        scanForClientLevels(mc, result);
        scanForClientLevels(mainLevel, result);

        return result;
    }

    private static void scanForClientLevels(
            final Object root, final java.util.List<ClientLevel> result) {
        if (root == null) {
            return;
        }
        Class<?> cls = root.getClass();
        while (cls != null && cls != Object.class) {
            for (final Field field : cls.getDeclaredFields()) {
                if (!Level.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    final Object value = field.get(root);
                    if (value instanceof final ClientLevel cl && !result.contains(cl)) {
                        result.add(cl);
                    }
                } catch (final Throwable ignored) {
                }
            }
            cls = cls.getSuperclass();
        }
    }
}
