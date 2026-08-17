package li.cil.oc2.common.energy;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.integration.ic2.Ic2EuBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class CableEnergyStorage extends EnergyStorage {
    public CableEnergyStorage() {
        super(Config.cableEnergyCapacity);
    }

    public int getTransferPerTick() {
        return Config.cableEnergyTransferPerTick;
    }

    public int pullFromNeighbors(final Level level, final BlockPos pos) {
        int pulled = 0;
        for (final Direction side : Direction.values()) {
            final BlockPos neighborPos = pos.relative(side);
            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            final IEnergyStorage neighbor =
                    level.getCapability(
                            Capabilities.EnergyStorage.BLOCK, neighborPos, side.getOpposite());
            if (neighbor == null || neighbor.equals(this)) {
                continue;
            }
            // Only pull from a pure source, never from a buffer or sink.
            if (!neighbor.canExtract() || neighbor.canReceive()) {
                continue;
            }
            final int amount =
                    Math.min(getMaxEnergyStored() - getEnergyStored(), getTransferPerTick());
            if (amount <= 0) {
                continue;
            }
            final int extracted = neighbor.extractEnergy(amount, false);
            if (extracted > 0) {
                receiveEnergy(extracted, false);
                pulled += extracted;
            }
        }
        if (Ic2EuBridge.getInstance().isAvailable()) {
            final int euPulled =
                    Ic2EuBridge.getInstance()
                            .pullEu(
                                    Math.min(
                                            (getMaxEnergyStored() - getEnergyStored())
                                                    / Ic2EuBridge.FE_PER_EU,
                                            getTransferPerTick() / Ic2EuBridge.FE_PER_EU),
                                    false);
            if (euPulled > 0) {
                receiveEnergy(euPulled * Ic2EuBridge.FE_PER_EU, false);
                pulled += euPulled * Ic2EuBridge.FE_PER_EU;
            }
        }
        return pulled;
    }

    public int transferToNeighbors(final Level level, final BlockPos pos) {
        int transferred = 0;
        if (getEnergyStored() <= 0) {
            return transferred;
        }
        for (final Direction side : Direction.values()) {
            final BlockPos neighborPos = pos.relative(side);
            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            final IEnergyStorage neighbor =
                    level.getCapability(
                            Capabilities.EnergyStorage.BLOCK, neighborPos, side.getOpposite());
            if (neighbor == null || neighbor.equals(this) || !neighbor.canReceive()) {
                continue;
            }
            final int amount = Math.min(getEnergyStored(), getTransferPerTick());
            final int accepted = neighbor.receiveEnergy(amount, false);
            if (accepted > 0) {
                extractEnergy(accepted, false);
                transferred += accepted;
            }
            if (Ic2EuBridge.getInstance().isAvailable()) {
                final int euAccepted =
                        Ic2EuBridge.getInstance()
                                .pushEu(
                                        Math.min(
                                                getEnergyStored() / Ic2EuBridge.FE_PER_EU,
                                                getTransferPerTick() / Ic2EuBridge.FE_PER_EU),
                                        false);
                if (euAccepted > 0) {
                    extractEnergy(euAccepted * Ic2EuBridge.FE_PER_EU, false);
                    transferred += euAccepted * Ic2EuBridge.FE_PER_EU;
                }
            }
        }
        return transferred;
    }
}