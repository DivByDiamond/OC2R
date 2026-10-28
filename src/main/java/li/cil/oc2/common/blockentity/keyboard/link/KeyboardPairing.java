package li.cil.oc2.common.blockentity.keyboard.link;

import java.util.Map;
import java.util.WeakHashMap;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-authoritative, dimension-scoped selection made by Shift+use on a keyboard. */
@SuppressWarnings("PMD.CompareObjectsWithEquals") // Selections bind exact live world/block instances.
public final class KeyboardPairing {
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Server-thread-only weak keys discard disconnected players.
    private static final Map<Player, Selection> SELECTIONS = new WeakHashMap<>();

    private KeyboardPairing() {}

    /** Selects a keyboard; a later endpoint interaction consumes the selection. */
    public static void select(final Player player, final KeyboardBlockEntity keyboard) {
        SELECTIONS.put(player, new Selection(keyboard, player.level().getGameTime()));
        message(player, "gui.oc2r.keyboard.selected");
    }

    private static void message(final Player player, final String key) {
        if (player instanceof final net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(key), true);
        }
    }

    private record Selection(KeyboardBlockEntity keyboard, long at) {}

    /** Shows the selection only to its owner and expires abandoned selections. */
    public static void tick(final KeyboardBlockEntity keyboard) {
        SELECTIONS.entrySet().removeIf(entry -> {
            final Player player = entry.getKey();
            final Selection selection = entry.getValue();
            if (selection.keyboard != keyboard) return false;
            if (!keyboard.isValid() || player.isRemoved() || player.level() != keyboard.getLevel()
                    || player.level().getGameTime() - selection.at > 200
                    || !keyboard.getBlockPos().closerToCenterThan(player.position(), 8)) return true;
            if (player instanceof final net.minecraft.server.level.ServerPlayer serverPlayer
                    && player.level().getGameTime() % 5 == 0) {
                final var pos = keyboard.getBlockPos();
                for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) {
                    ((net.minecraft.server.level.ServerLevel) player.level()).sendParticles(serverPlayer,
                            net.minecraft.core.particles.ParticleTypes.END_ROD, true,
                            //? if >=26.1 {
                            /*false,
                            *///?}
                            pos.getX() + 0.05 + x * 0.9, pos.getY() + 0.15,
                            pos.getZ() + 0.05 + z * 0.9, 1, 0, 0, 0, 0);
                }
            }
            return false;
        });
    }

    /** Links a selected keyboard and suppresses the target block's ordinary interaction. */
    public static boolean tryLink(final Player player, final BlockPos pos) {
        if (player.level().isClientSide()) return false;
        final Selection selection = SELECTIONS.get(player);
        if (selection == null) return false;
        final KeyboardBlockEntity keyboard = selection.keyboard;
        if (!keyboard.isValid() || keyboard.getLevel() != player.level()
                || !keyboard.getBlockPos().closerToCenterThan(player.position(), 8)
                || !pos.closerToCenterThan(player.position(), 8)) {
            SELECTIONS.remove(player);
            return false;
        }
        final BlockEntity endpoint = KeyboardTargets.resolve(player.level(), pos);
        if (endpoint == null) return false;
        keyboard.link(endpoint);
        SELECTIONS.remove(player);
        message(player, "gui.oc2r.keyboard.linked");
        return true;
    }
}
