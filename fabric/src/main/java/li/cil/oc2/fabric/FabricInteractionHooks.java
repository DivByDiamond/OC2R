package li.cil.oc2.fabric;

import li.cil.oc2.common.block.cable.BusCableBlock;
import li.cil.oc2.common.item.tool.WrenchItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * Fabric counterparts of NeoForge-only interaction hooks that shared code implements without
 * {@code @Override}: sneak-bypass of item use ({@code Item.doesSneakBypassUse}), the pick-block overload
 * with hit result and player ({@code Block.getCloneItemStack}) and the persistent entity data copy on
 * respawn.
 */
public final class FabricInteractionHooks {
    private FabricInteractionHooks() {
    }

    public static void register() {
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> copyPersistentData(oldPlayer, newPlayer));
    }

    /** NeoForge keeps the persistent data of a player over death, dimension change and the End exit. */
    static void copyPersistentData(final ServerPlayer from, final ServerPlayer to) {
        final CompoundTag source = ((PersistentDataHolder) from).oc2r$getPersistentData();
        final CompoundTag target = ((PersistentDataHolder) to).oc2r$getPersistentData();
        if (source == target) {
            return;
        }
        for (final String key : source.getAllKeys()) {
            target.put(key, source.get(key).copy());
        }
    }

    /**
     * NeoForge's rule: while sneaking, block interaction is only skipped if a held, non-empty item does
     * not bypass it (both hands are asked).
     */
    public static boolean sneakSkipsBlockUse(final Player player, final boolean vanillaDecision) {
        if (!vanillaDecision) {
            return false;
        }
        return !(bypasses(player, player.getMainHandItem()) && bypasses(player, player.getOffhandItem()));
    }

    private static boolean bypasses(final Player player, final ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return stack.getItem() instanceof final WrenchItem wrench
                && wrench.doesSneakBypassUse(stack, player.level(), player.blockPosition(), player);
    }

    /** Pick-block result: the cable's context-aware stack for bus cables, otherwise vanilla's. */
    public static ItemStack pickBlock(
            final BlockState state, final HitResult hit, final LevelReader level, final BlockPos pos,
            final Player player) {
        if (state.getBlock() instanceof final BusCableBlock cable) {
            return cable.getCloneItemStack(state, hit, level, pos, player);
        }
        return state.getBlock().getCloneItemStack(level, pos, state);
    }
}
