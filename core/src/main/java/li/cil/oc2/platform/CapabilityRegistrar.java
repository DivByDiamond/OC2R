package li.cil.oc2.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-independent registrar for exposing capabilities through the loader's capability system,
 * valid only for the duration of loader setup (like {@link MessageRegistrar} this cannot be a
 * {@link java.util.ServiceLoader}-discovered singleton: NeoForge hands out its registration event
 * only once, during a one-shot setup callback).
 *
 * <p>Mod setup code funnels every registration through one implementation obtained from the loader
 * module — see {@code li.cil.oc2.common.capabilities.CapabilitySetup} — so no registration site
 * names a loader type.
 */
public interface CapabilityRegistrar {
    /** Exposes the value {@code provider} returns for any of {@code blocks}. */
    <T> void registerBlock(
            BlockCapability<T> capability, BlockCapabilityProvider<T> provider, Block... blocks);

    /** Exposes the value {@code provider} returns for entities of {@code entityType}. */
    <T> void registerEntity(
            EntityCapability<T> capability,
            EntityType<?> entityType,
            EntityCapabilityProvider<T> provider);

    /** Exposes the value {@code provider} returns for stacks of any of {@code items}. */
    <T> void registerItem(
            ItemCapability<T> capability, ItemCapabilityProvider<T> provider, ItemLike... items);

    /** Provides a capability value for a block position; {@code side} is {@code null} for void keys. */
    @FunctionalInterface
    interface BlockCapabilityProvider<T> {
        @Nullable
        T get(
                Level level,
                BlockPos pos,
                BlockState state,
                @Nullable BlockEntity blockEntity,
                @Nullable Direction side);
    }

    /** Provides a capability value for an entity; {@code side} is {@code null} for void keys. */
    @FunctionalInterface
    interface EntityCapabilityProvider<T> {
        @Nullable
        T get(Entity entity, @Nullable Direction side);
    }

    /** Provides a capability value for an item stack. */
    @FunctionalInterface
    interface ItemCapabilityProvider<T> {
        @Nullable
        T get(ItemStack stack);
    }
}
