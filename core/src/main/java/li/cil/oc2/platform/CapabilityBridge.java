package li.cil.oc2.platform;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-independent entry point for querying the capabilities a neighboring block, entity or item
 * stack exposes. A stateless singleton discovered through {@link java.util.ServiceLoader} via
 * {@link Platform#capabilities()}, mirroring {@link EnergyBridge} and {@link NetworkBridge}.
 *
 * <p>Exposing <em>our own</em> capabilities is the separate, setup-scoped
 * {@link CapabilityRegistrar}.
 */
public interface CapabilityBridge {
    /** The capability the block at {@code pos} exposes on {@code side}, or {@code null}. */
    <T>
    @Nullable
    T getBlockCapability(
            BlockCapability<T> capability,
            Level level,
            BlockPos pos,
            @Nullable Direction side);

    /**
     * Like {@link #getBlockCapability(BlockCapability, Level, BlockPos, Direction)} but for callers
     * that already hold the block state and block entity, sparing the loader a re-lookup.
     */
    <T>
    @Nullable
    T getBlockCapability(
            BlockCapability<T> capability,
            Level level,
            BlockPos pos,
            @Nullable BlockState state,
            @Nullable BlockEntity blockEntity,
            @Nullable Direction side);

    /** The capability {@code entity} exposes on {@code side}, or {@code null}. */
    <T>
    @Nullable
    T getEntityCapability(
            EntityCapability<T> capability, Entity entity, @Nullable Direction side);

    /** The capability {@code stack} exposes, or {@code null}. */
    <T> @Nullable T getItemCapability(ItemCapability<T> capability, ItemStack stack);

    /**
     * Creates a lookup cache for a block capability, invalidating itself when the block at
     * {@code pos} changes or when {@link #invalidateBlock} runs for it.
     *
     * @param isValid consulted before every {@link BlockCapabilityCache#get()}; a {@code false}
     *     return makes the cache report {@code null} instead of querying the level
     * @param onInvalidation invoked on the server thread whenever the cached lookup is invalidated
     */
    <T> BlockCapabilityCache<T> createBlockCapabilityCache(
            BlockCapability<T> capability,
            ServerLevel level,
            BlockPos pos,
            @Nullable Direction side,
            BooleanSupplier isValid,
            Runnable onInvalidation);

    /**
     * Drops every capability cached for the block at {@code pos} and notifies invalidation
     * listeners registered for it. Callers use this after mutating a block entity's exposed state.
     */
    void invalidateBlock(Level level, BlockPos pos);

    /**
     * Registers {@code listener} to run whenever capabilities at {@code pos} are invalidated, so
     * cached lookups of a neighbor can be refreshed.
     *
     * @return {@code true} to keep the listener registered after it ran
     */
    boolean registerBlockCapabilityListener(
            ServerLevel level, BlockPos pos, CapabilityInvalidationListener listener);
}
