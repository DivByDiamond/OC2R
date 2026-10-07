package li.cil.oc2.fabric;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import li.cil.oc2.common.blockentity.PlatformBlockEntity;
import li.cil.oc2.common.item.tool.WrenchItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Calls the hooks NeoForge patches into vanilla and that the shared code relies on: {@code onLoad} of
 * block entities and {@code Item.onItemUseFirst}. ({@code onChunkUnloaded} is in {@code LevelChunkMixin},
 * the client side update tag hooks are in the client mixins.)
 *
 * <p>NeoForge calls {@code onLoad} at the start of the first block entity tick of the level after the
 * block entity was added, not while the chunk is still being assembled, so neighbours are accessible.
 * The same deferral is done here: loaded block entities are queued and handed out at the start of the next
 * tick of their level.
 */
public final class FabricBlockEntityHooks {
    private static final Map<Level, Queue<PlatformBlockEntity>> FRESH =
            Collections.synchronizedMap(new WeakHashMap<>());

    private FabricBlockEntityHooks() {
    }

    /** Registers the hooks that apply to servers (and the integrated server). */
    public static void register() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> enqueue(blockEntity, level));
        ServerTickEvents.START_WORLD_TICK.register(FabricBlockEntityHooks::drain);
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            final ItemStack stack = player.getItemInHand(hand);
            if (!player.isSpectator() && stack.getItem() instanceof final WrenchItem wrench) {
                return wrench.onItemUseFirst(stack, new UseOnContext(player, hand, hitResult));
            }
            return InteractionResult.PASS;
        });
    }

    static void enqueue(final BlockEntity blockEntity, final Level level) {
        if (blockEntity instanceof final PlatformBlockEntity platform) {
            synchronized (FRESH) {
                FRESH.computeIfAbsent(level, key -> new ArrayDeque<>()).add(platform);
            }
        }
    }

    static void drain(final Level level) {
        final Queue<PlatformBlockEntity> queue = FRESH.get(level);
        if (queue == null) {
            return;
        }
        while (true) {
            final PlatformBlockEntity blockEntity;
            synchronized (FRESH) {
                blockEntity = queue.poll();
            }
            if (blockEntity == null) {
                return;
            }
            // Same condition as NeoForge: skip block entities that were discarded in the meantime.
            if (!blockEntity.isRemoved() && blockEntity.hasLevel()) {
                blockEntity.onLoad();
            }
        }
    }
}
