package li.cil.oc2.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-independent entry point for querying the {@link EnergyStorage} exposed by a neighboring
 * block, entity or item. A stateless singleton discovered through {@link java.util.ServiceLoader}
 * via {@link Platform#energy()}, mirroring {@link NetworkBridge}. Registering our own storages so
 * other mods can query them back is the separate, event-scoped {@link EnergyCapabilityRegistrar}.
 */
public interface EnergyBridge {
    /** The energy storage the block at {@code pos} exposes on {@code side}, or {@code null}. */
    @Nullable EnergyStorage getBlockEnergy(Level level, BlockPos pos, @Nullable Direction side);

    /** The energy storage {@code entity} exposes on {@code side}, or {@code null}. */
    @Nullable EnergyStorage getEntityEnergy(Entity entity, @Nullable Direction side);

    /** The energy storage {@code stack} exposes, or {@code null}. */
    @Nullable EnergyStorage getItemEnergy(ItemStack stack);
}
