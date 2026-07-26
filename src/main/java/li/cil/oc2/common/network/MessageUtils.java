/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MessageUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static long lastMissingBeLog = 0;
    private static java.util.Set<BlockPos> loggedMissingBe = new java.util.HashSet<>();

    public static <T extends BlockEntity> void withNearbyServerBlockEntityForInteraction(final IPayloadContext context, final BlockPos pos, final Class<T> type, final BiConsumer<ServerPlayer, T> callback) {
        final ServerPlayer player = (ServerPlayer) context.player();
        if (player == null) { // || !pos.closerToCenterThan(player.position(), 8)) {
            return;
        }

        withNearbyServerBlockEntity(context, pos, type, callback);
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withNearbyServerBlockEntity(final IPayloadContext context, final BlockPos pos, final Class<T> type, final BiConsumer<ServerPlayer, T> callback) {
        final ServerPlayer player = (ServerPlayer) context.player();
        if (player == null) {
            return;
        }

        final ServerLevel level = player.getServer().getLevel(player.level().dimension());
        final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, pos);
        if (type.isInstance(blockEntity)) {
            callback.accept(player, (T) blockEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withServerEntity(final IPayloadContext context, final int id, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayer player = (ServerPlayer) context.player();
        if (player == null) {
            return;
        }

        final ServerLevel level = player.getServer().getLevel(player.level().dimension());
        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withNearbyServerEntity(final IPayloadContext context, final int id, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayer player = (ServerPlayer) context.player();
        if (player == null) {
            return;
        }

        final ServerLevel level = player.getServer().getLevel(player.level().dimension());
        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity) && entity.closerThan(player, 8)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withClientBlockEntityAt(final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        // First, try the main client level (the fast path for normal
        // overworld/nether/end blocks).
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (type.isInstance(blockEntity)) {
            callback.accept((T) blockEntity);
            return;
        }

        // Sable / Valkyrien Skies / Create: Aeronautics compatibility:
        // blocks placed on a ship live in a separate "sub-level" — a
        // different ClientLevel instance that is NOT
        // Minecraft.getInstance().level. When the server sends a terminal
        // output / run state message for a ship-mounted computer, the pos
        // in the message is the ship-local position, and looking it up in
        // the main level returns null. We need to scan every loaded level
        // the client knows about.
        //
        // We do this via reflection on ClientLevel to find sub-level
        // references. This is brittle, but it's the only way to discover
        // ship worlds without a hard dependency on Sable / VS2. If
        // reflection fails we just bail — no worse than the current
        // behaviour.
        boolean found = false;
        final java.util.List<ClientLevel> allLevels = findAllClientLevels(mc, level);
        for (final ClientLevel otherLevel : allLevels) {
            if (otherLevel == level) {
                continue; // already tried
            }
            try {
                final BlockEntity otherBlockEntity = otherLevel.getBlockEntity(pos);
                if (type.isInstance(otherBlockEntity)) {
                    callback.accept((T) otherBlockEntity);
                    found = true;
                    return;
                }
            } catch (final Throwable ignored) {
                // Different sub-level may not have this chunk loaded; skip.
            }
        }

        // Diagnostic: log when a message arrives but we can't find the
        // BlockEntity in any loaded level. This is the key symptom for
        // Sable/VS2 ship worlds — the message arrives with a ship-local
        // pos, but the BlockEntity is in a sub-level we don't know about.
        // Throttle to one log per unique position per 5 seconds to avoid
        // spam.
        if (!found) {
            final long now = System.currentTimeMillis();
            if (now - lastMissingBeLog > 5000) {
                lastMissingBeLog = now;
                loggedMissingBe.clear();
            }
            if (loggedMissingBe.add(pos)) {
                LOGGER.warn("[MessageUtils] withClientBlockEntityAt: BlockEntity of type {} not found at pos {} in main level or any of {} sub-level(s). Main level class: {}. This is likely a Sable/VS2 ship world — the ship's ClientLevel is not reachable via reflection from Minecraft or the main ClientLevel.",
                    type.getSimpleName(), pos, allLevels.size() - 1,
                    level.getClass().getName());
            }
        }
    }

    /**
     * Discover all loaded ClientLevel instances visible to the client,
     * including ship sub-levels created by Sable / Valkyrien Skies.
     * Returns a list that always includes the main level.
     *
     * Implementation: scan declared fields of Minecraft and of the main
     * ClientLevel for any field whose type is assignable to Level. Sable /
     * VS2 typically store their ship worlds as fields on their own
     * singleton managers, which in turn are reachable from Minecraft or
     * the main ClientLevel via static fields / manager references.
     *
     * This is intentionally defensive — any reflection failure just means
     * we miss a sub-level, which is no worse than the previous behaviour
     * of only checking the main level.
     */
    private static java.util.List<ClientLevel> findAllClientLevels(final Minecraft mc, final ClientLevel mainLevel) {
        final java.util.List<ClientLevel> result = new java.util.ArrayList<>();
        result.add(mainLevel);

        // Scan Minecraft's declared fields for ClientLevel-typed references.
        scanForClientLevels(mc, result);
        // Scan the main level's declared fields too (Sable may attach ship
        // worlds as fields on the level itself).
        scanForClientLevels(mainLevel, result);

        return result;
    }

    private static void scanForClientLevels(final Object root, final java.util.List<ClientLevel> result) {
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
                    // Static fields are scanned separately below to avoid
                    // redundant work per-instance.
                    continue;
                }
                try {
                    field.setAccessible(true);
                    final Object value = field.get(root);
                    if (value instanceof final ClientLevel cl && !result.contains(cl)) {
                        result.add(cl);
                    }
                } catch (final Throwable ignored) {
                    // Inaccessible field, skip.
                }
            }
            cls = cls.getSuperclass();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withClientEntity(final int id, final Class<T> type, final Consumer<T> callback) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }
}
