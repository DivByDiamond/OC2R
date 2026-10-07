package li.cil.oc2.platform;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * {@link CapabilityBridge} for Fabric. Fabric has no capability system of its own, so the mod's own
 * capabilities (device bus elements, devices, ...) are looked up in provider tables filled by
 * {@link FabricCapabilityRegistrar}; those never need to be visible to other mods. The standard
 * cross-mod capability, energy, is additionally bridged to Team Reborn Energy.
 *
 * <p>Lookups are not cached: {@link #createBlockCapabilityCache} re-queries on every {@code get()}.
 * Invalidation listeners still fire when a block changes (see {@code LevelChunkMixin}), so callers
 * that rescan on invalidation behave as on NeoForge.
 */
public final class FabricCapabilityBridge implements CapabilityBridge {
    private static final Map<Long, List<Listener>> LISTENERS = new ConcurrentHashMap<>();
    private static final int SWEEP_INTERVAL = 1024;
    private static final AtomicLong registrations = new AtomicLong();

    @Override
    @Nullable
    public <T> T getBlockCapability(
            final BlockCapability<T> capability,
            final Level level,
            final BlockPos pos,
            @Nullable final Direction side) {
        final BlockState state = level.getBlockState(pos);
        return getBlockCapability(capability, level, pos, state, level.getBlockEntity(pos), side);
    }

    @Override
    @Nullable
    public <T> T getBlockCapability(
            final BlockCapability<T> capability,
            final Level level,
            final BlockPos pos,
            @Nullable final BlockState state,
            @Nullable final BlockEntity blockEntity,
            @Nullable final Direction side) {
        final BlockState actualState = state != null ? state : level.getBlockState(pos);
        final BlockEntity actualEntity = blockEntity != null ? blockEntity : level.getBlockEntity(pos);
        final Direction actualSide = capability.isSided() ? side : null;

        final T own = FabricCapabilities.findBlock(capability, level, pos, actualState, actualEntity, actualSide);
        if (own != null) {
            return own;
        }
        return FabricEnergy.findBlock(capability, level, pos, actualSide);
    }

    @Override
    @Nullable
    public <T> T getEntityCapability(
            final EntityCapability<T> capability, final Entity entity, @Nullable final Direction side) {
        return FabricCapabilities.findEntity(capability, entity, capability.isSided() ? side : null);
    }

    @Override
    @Nullable
    public <T> T getItemCapability(final ItemCapability<T> capability, final ItemStack stack) {
        return FabricCapabilities.findItem(capability, stack);
    }

    @Override
    public <T> BlockCapabilityCache<T> createBlockCapabilityCache(
            final BlockCapability<T> capability,
            final ServerLevel level,
            final BlockPos pos,
            @Nullable final Direction side,
            final BooleanSupplier isValid,
            final Runnable onInvalidation) {
        final CapabilityInvalidationListener listener = new CapabilityInvalidationListener(() -> {
            onInvalidation.run();
            return true;
        });
        registerBlockCapabilityListener(level, pos, listener);
        return new BlockCapabilityCache<>() {
            // Strong reference: the bridge holds listeners weakly, so the cache keeps its own alive.
            @SuppressWarnings("unused")
            private final CapabilityInvalidationListener keepAlive = listener;

            @Override
            @Nullable
            public T get() {
                return isValid.getAsBoolean() ? getBlockCapability(capability, level, pos, side) : null;
            }
        };
    }

    @Override
    public void invalidateBlock(final Level level, final BlockPos pos) {
        invalidate(level, pos);
    }

    @Override
    public boolean registerBlockCapabilityListener(
            final ServerLevel level, final BlockPos pos, final CapabilityInvalidationListener listener) {
        final List<Listener> listeners = LISTENERS.computeIfAbsent(pos.asLong(), k -> new CopyOnWriteArrayList<>());
        listeners.removeIf(FabricCapabilityBridge::isDead);
        listeners.add(new Listener(new WeakReference<>(level), new WeakReference<>(listener)));
        // Entries whose position is never invalidated again would otherwise stay forever.
        if (registrations.incrementAndGet() % SWEEP_INTERVAL == 0) {
            sweep();
        }
        return true;
    }

    private static boolean isDead(final Listener entry) {
        return entry.listener().get() == null || entry.level().get() == null;
    }

    private static void sweep() {
        LISTENERS.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(FabricCapabilityBridge::isDead);
            return entry.getValue().isEmpty();
        });
    }

    /** Invalidates every position in {@code chunk}, like NeoForge does when a chunk loads or unloads. */
    public static void invalidateChunk(final Level level, final ChunkPos chunk) {
        for (final Long key : List.copyOf(LISTENERS.keySet())) {
            final BlockPos pos = BlockPos.of(key);
            if ((pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z) {
                invalidate(level, pos);
            }
        }
    }

    /** Notifies the listeners registered for {@code pos} in {@code level}; used by the chunk mixin too. */
    public static void invalidate(final Level level, final BlockPos pos) {
        final long key = pos.asLong();
        final List<Listener> listeners = LISTENERS.get(key);
        if (listeners == null) {
            return;
        }
        for (final Listener entry : listeners) {
            final CapabilityInvalidationListener listener = entry.listener().get();
            if (listener == null || entry.level().get() == null) {
                listeners.remove(entry);
            } else if (entry.level().get() == level && !listener.onInvalidate()) {
                listeners.remove(entry);
            }
        }
        if (listeners.isEmpty()) {
            LISTENERS.remove(key, listeners);
        }
    }

    private record Listener(WeakReference<Level> level, WeakReference<CapabilityInvalidationListener> listener) {}

    /** Capability ids shared with other mods keep their loader-neutral {@code neoforge:} namespace. */
    static ResourceLocation standardId(final String path) {
        return ResourceLocation.fromNamespaceAndPath("neoforge", path);
    }
}
