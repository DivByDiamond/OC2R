package li.cil.oc2.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Provider tables behind {@link FabricCapabilityBridge}, filled by {@link FabricCapabilityRegistrar}. */
final class FabricCapabilities {
    private static final Map<BlockCapability<?>, Map<Block, List<CapabilityRegistrar.BlockCapabilityProvider<?>>>>
            BLOCKS = new ConcurrentHashMap<>();
    private static final Map<EntityCapability<?>,
                    Map<EntityType<?>, List<CapabilityRegistrar.EntityCapabilityProvider<?>>>>
            ENTITIES = new ConcurrentHashMap<>();
    private static final Map<ItemCapability<?>, Map<Item, List<CapabilityRegistrar.ItemCapabilityProvider<?>>>> ITEMS =
            new ConcurrentHashMap<>();

    private FabricCapabilities() {}

    static <T> void addBlock(
            final BlockCapability<T> capability,
            final CapabilityRegistrar.BlockCapabilityProvider<T> provider,
            final Block block) {
        BLOCKS.computeIfAbsent(capability, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(block, k -> new CopyOnWriteArrayList<>())
                .add(provider);
    }

    static <T> void addEntity(
            final EntityCapability<T> capability,
            final EntityType<?> type,
            final CapabilityRegistrar.EntityCapabilityProvider<T> provider) {
        ENTITIES.computeIfAbsent(capability, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(provider);
    }

    static <T> void addItem(
            final ItemCapability<T> capability,
            final CapabilityRegistrar.ItemCapabilityProvider<T> provider,
            final Item item) {
        ITEMS.computeIfAbsent(capability, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(item, k -> new CopyOnWriteArrayList<>())
                .add(provider);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T> T findBlock(
            final BlockCapability<T> capability,
            final Level level,
            final BlockPos pos,
            final BlockState state,
            @Nullable final BlockEntity blockEntity,
            @Nullable final Direction side) {
        final Map<Block, List<CapabilityRegistrar.BlockCapabilityProvider<?>>> byBlock = BLOCKS.get(capability);
        if (byBlock == null) {
            return null;
        }
        final List<CapabilityRegistrar.BlockCapabilityProvider<?>> providers = byBlock.get(state.getBlock());
        if (providers == null) {
            return null;
        }
        for (final CapabilityRegistrar.BlockCapabilityProvider<?> provider : providers) {
            final T value = (T) provider.get(level, pos, state, blockEntity, side);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T> T findEntity(
            final EntityCapability<T> capability, final Entity entity, @Nullable final Direction side) {
        final Map<EntityType<?>, List<CapabilityRegistrar.EntityCapabilityProvider<?>>> byType =
                ENTITIES.get(capability);
        if (byType == null) {
            return null;
        }
        final List<CapabilityRegistrar.EntityCapabilityProvider<?>> providers = byType.get(entity.getType());
        if (providers == null) {
            return null;
        }
        for (final CapabilityRegistrar.EntityCapabilityProvider<?> provider : providers) {
            final T value = (T) provider.get(entity, side);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T> T findItem(final ItemCapability<T> capability, final ItemStack stack) {
        final Map<Item, List<CapabilityRegistrar.ItemCapabilityProvider<?>>> byItem = ITEMS.get(capability);
        if (byItem == null) {
            return null;
        }
        final List<CapabilityRegistrar.ItemCapabilityProvider<?>> providers = byItem.get(stack.getItem());
        if (providers == null) {
            return null;
        }
        for (final CapabilityRegistrar.ItemCapabilityProvider<?> provider : providers) {
            final T value = (T) provider.get(stack);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
