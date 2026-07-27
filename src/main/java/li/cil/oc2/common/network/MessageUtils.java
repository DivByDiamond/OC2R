package li.cil.oc2.common.network;

import li.cil.oc2.common.util.LevelUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MessageUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static <T extends BlockEntity> void withNearbyServerBlockEntityForInteraction(
            final IPayloadContext context,
            final BlockPos pos,
            final Class<T> type,
            final BiConsumer<ServerPlayer, T> callback) {
        final ServerPlayer player = (ServerPlayer) context.player();
        if (player == null) {
            return;
        }

        withNearbyServerBlockEntity(context, pos, type, callback);
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withNearbyServerBlockEntity(
            final IPayloadContext context,
            final BlockPos pos,
            final Class<T> type,
            final BiConsumer<ServerPlayer, T> callback) {
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
    public static <T extends Entity> void withServerEntity(
            final IPayloadContext context,
            final int id,
            final Class<T> type,
            final Consumer<T> callback) {
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
    public static <T extends Entity> void withNearbyServerEntity(
            final IPayloadContext context,
            final int id,
            final Class<T> type,
            final Consumer<T> callback) {
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
    public static <T extends Entity> void withClientEntity(
            final int id, final Class<T> type, final Consumer<T> callback) {
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
