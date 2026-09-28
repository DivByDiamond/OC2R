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
 * Loader-independent registrar for exposing {@link EnergyStorage} instances through the loader's
 * capability system, valid only for the duration of loader setup (like {@link MessageRegistrar}, this
 * cannot be a {@link java.util.ServiceLoader}-discovered singleton: NeoForge hands out its capability
 * registration event only once, during a one-shot setup event).
 */
public interface EnergyCapabilityRegistrar {
    /** Exposes the energy storage {@code provider} returns for any of {@code blocks}. */
    void registerBlock(BlockEnergyProvider provider, Block... blocks);

    /** Exposes the energy storage {@code provider} returns for entities of {@code type}. */
    void registerEntity(EntityType<?> type, EntityEnergyProvider provider);

    /** Exposes the energy storage {@code provider} returns for stacks of any of {@code items}. */
    void registerItem(ItemEnergyProvider provider, ItemLike... items);

    @FunctionalInterface
    interface BlockEnergyProvider {
        @Nullable EnergyStorage get(
                Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, @Nullable Direction side);
    }

    @FunctionalInterface
    interface EntityEnergyProvider {
        @Nullable EnergyStorage get(Entity entity, @Nullable Direction side);
    }

    @FunctionalInterface
    interface ItemEnergyProvider {
        @Nullable EnergyStorage get(ItemStack stack);
    }
}
