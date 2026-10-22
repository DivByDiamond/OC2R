package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import org.jetbrains.annotations.Nullable;

/** Stateless {@link CapabilityBridge} backed by NeoForge's capability system. */
@SuppressFBWarnings(
        value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "NP_NONNULL_PARAM_VIOLATION"},
        justification = "NeoForge capability contexts are null for void keys and item lookups")
public final class NeoForgeCapabilityBridge implements CapabilityBridge {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBlockCapability(
            final BlockCapability<T> capability,
            final Level level,
            final BlockPos pos,
            @Nullable final Direction side) {
        return (T)
                NeoForgeCapabilities.fromNeoForge(
                        capability.getType(),
                        level.getCapability(
                                NeoForgeCapabilities.blockKey(capability),
                                pos,
                                context(capability.isSided(), side)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBlockCapability(
            final BlockCapability<T> capability,
            final Level level,
            final BlockPos pos,
            @Nullable final BlockState state,
            @Nullable final BlockEntity blockEntity,
            @Nullable final Direction side) {
        return (T)
                NeoForgeCapabilities.fromNeoForge(
                        capability.getType(),
                        level.getCapability(
                                NeoForgeCapabilities.blockKey(capability),
                                pos,
                                state,
                                blockEntity,
                                context(capability.isSided(), side)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getEntityCapability(
            final EntityCapability<T> capability,
            final Entity entity,
            @Nullable final Direction side) {
        return (T)
                NeoForgeCapabilities.fromNeoForge(
                        capability.getType(),
                        entity.getCapability(
                                NeoForgeCapabilities.entityKey(capability),
                                context(capability.isSided(), side)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getItemCapability(
            final ItemCapability<T> capability, final ItemStack stack) {
        return (T)
                NeoForgeCapabilities.fromNeoForge(
                //? if >=26.1 {
/*                        capability.getType(),
                        stack.getCapability(
                                NeoForgeCapabilities.itemKey(capability),
                                NeoForgeCapabilities.itemContext(capability.getType(), stack)));
*///?} else {
                        capability.getType(), stack.getCapability(NeoForgeCapabilities.itemKey(capability), null));
                //?}
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> BlockCapabilityCache<T> createBlockCapabilityCache(
            final BlockCapability<T> capability,
            final ServerLevel level,
            final BlockPos pos,
            @Nullable final Direction side,
            final BooleanSupplier isValid,
            final Runnable onInvalidation) {
        final var cache =
                net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                        NeoForgeCapabilities.blockKey(capability),
                        level,
                        pos,
                        context(capability.isSided(), side),
                        isValid,
                        onInvalidation);
        return () -> (T) NeoForgeCapabilities.fromNeoForge(capability.getType(), cache.getCapability());
    }

    @Override
    public void invalidateBlock(final Level level, final BlockPos pos) {
        level.invalidateCapabilities(pos);
    }

    @Override
    public boolean registerBlockCapabilityListener(
            final ServerLevel level,
            final BlockPos pos,
            final CapabilityInvalidationListener listener) {
        // NeoForge keeps registered listeners through a weak reference: an adapter built here and
        // referenced nowhere else would be collected on the next GC cycle and silently stop
        // firing. Storing it on the listener ties its lifetime to the caller's (typically a block
        // entity field), which is what the previous NeoForge-typed call sites provided.
        var adapter = (ICapabilityInvalidationListener) listener.getNativeListener();
        if (adapter == null) {
            adapter = listener::onInvalidate;
            listener.setNativeListener(adapter);
        }
        level.registerCapabilityListener(pos, adapter);
        return true;
    }

    @Nullable
    private static Object context(final boolean sided, @Nullable final Direction side) {
        return sided ? side : null;
    }
}
