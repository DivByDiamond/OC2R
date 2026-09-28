package li.cil.oc2.platform;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** Stateless {@link EnergyBridge} backed by NeoForge's {@code Capabilities.EnergyStorage}. */
public final class NeoForgeEnergyBridge implements EnergyBridge {
    @Override
    @Nullable
    public EnergyStorage getBlockEnergy(final Level level, final BlockPos pos, @Nullable final Direction side) {
        return wrap(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side));
    }

    @Override
    @Nullable
    public EnergyStorage getEntityEnergy(final Entity entity, @Nullable final Direction side) {
        return wrap(entity.getCapability(Capabilities.EnergyStorage.ENTITY, side));
    }

    @Override
    @Nullable
    public EnergyStorage getItemEnergy(final ItemStack stack) {
        return wrap(stack.getCapability(Capabilities.EnergyStorage.ITEM));
    }

    @Nullable
    private static EnergyStorage wrap(@Nullable final IEnergyStorage neoForge) {
        if (neoForge == null) {
            return null;
        }
        return new EnergyStorage() {
            @Override
            public int receiveEnergy(final int maxReceive, final boolean simulate) {
                return neoForge.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(final int maxExtract, final boolean simulate) {
                return neoForge.extractEnergy(maxExtract, simulate);
            }

            @Override
            public int getEnergyStored() {
                return neoForge.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return neoForge.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return neoForge.canExtract();
            }

            @Override
            public boolean canReceive() {
                return neoForge.canReceive();
            }
        };
    }
}
