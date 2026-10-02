package li.cil.oc2.platform;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Stateless {@link EnergyBridge} delegating to the generic {@link CapabilityBridge}: energy is a
 * capability like any other, addressed through the same core key as every other lookups.
 */
public final class NeoForgeEnergyBridge implements EnergyBridge {
    @Override
    @Nullable
    public EnergyStorage getBlockEnergy(
            final Level level, final BlockPos pos, @Nullable final Direction side) {
        return Platform.capabilities()
                .getBlockCapability(Capabilities.EnergyStorage.BLOCK, level, pos, side);
    }

    @Override
    @Nullable
    public EnergyStorage getEntityEnergy(final Entity entity, @Nullable final Direction side) {
        return Platform.capabilities()
                .getEntityCapability(Capabilities.EnergyStorage.ENTITY, entity, side);
    }

    @Override
    @Nullable
    public EnergyStorage getItemEnergy(final ItemStack stack) {
        return Platform.capabilities()
                .getItemCapability(Capabilities.EnergyStorage.ITEM, stack);
    }
}
