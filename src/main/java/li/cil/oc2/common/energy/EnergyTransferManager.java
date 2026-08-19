package li.cil.oc2.common.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.ic2.Ic2EuBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EnergyTransferManager {
    private EnergyTransferManager() {}

    public static void distribute(final BusCableBlockEntity cable) {
        final Level level = cable.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        final long gameTime = level.getGameTime();
        // The whole network is handled once per tick by whichever cable ticks first.
        if (cable.energyDistributionTick == gameTime) {
            return;
        }

        final List<BusCableBlockEntity> cables = collectNetworkCables(level, cable.getBlockPos());
        if (cables.isEmpty()) {
            return;
        }
        for (final BusCableBlockEntity networkCable : cables) {
            networkCable.energyDistributionTick = gameTime;
        }

        final int networkRate = cables.get(0).energy.getTransferPerTick();

        // Pull from pure sources adjacent to the network, remembering where we pulled from
        // so we don't immediately push the energy back.
        final Set<BlockPos> sourcesPulled = new HashSet<>();
        pullFromAllSources(level, cables, networkRate, sourcesPulled);

        // Spread stored energy evenly across the network so it is reachable from any sink.
        redistribute(cables);

        // Push to sinks adjacent to the network.
        pushToAllSinks(level, cables, networkRate, sourcesPulled);
    }

    private static void pullFromAllSources(
            final Level level,
            final List<BusCableBlockEntity> cables,
            final int networkRate,
            final Set<BlockPos> sourcesPulled) {
        int pulled = 0;
        for (final BusCableBlockEntity networkCable : cables) {
            if (pulled >= networkRate) {
                break;
            }
            pulled += pullFromSources(level, networkCable, networkRate - pulled, sourcesPulled);
        }
    }

    private static void pushToAllSinks(
            final Level level,
            final List<BusCableBlockEntity> cables,
            final int networkRate,
            final Set<BlockPos> sourcesPulled) {
        int pushed = 0;
        for (final BusCableBlockEntity networkCable : cables) {
            if (pushed >= networkRate) {
                break;
            }
            pushed += pushToSinks(level, networkCable, networkRate - pushed, sourcesPulled);
        }
    }

    public static Set<BlockPos> collectNetwork(final Level level, final BlockPos origin) {
        final Set<BlockPos> visited = new LinkedHashSet<>();
        final Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        while (!queue.isEmpty()) {
            final BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            for (final Direction side : Direction.values()) {
                final BlockPos neighborPos = pos.relative(side);
                if (isCable(level, neighborPos, side.getOpposite())) {
                    queue.add(neighborPos);
                }
            }
        }
        return visited;
    }

    private static List<BusCableBlockEntity> collectNetworkCables(
            final Level level, final BlockPos origin) {
        final List<BusCableBlockEntity> cables = new ArrayList<>();
        for (final BlockPos pos : collectNetwork(level, origin)) {
            if (level.getBlockEntity(pos) instanceof final BusCableBlockEntity cable) {
                cables.add(cable);
            }
        }
        return cables;
    }

    private static int pullFromSources(
            final Level level,
            final BusCableBlockEntity cable,
            final int budget,
            final Set<BlockPos> sourcesPulled) {
        final BlockPos pos = cable.getBlockPos();
        final int pulled = pullFromSides(level, cable, pos, budget, sourcesPulled);
        return pullFromEu(cable, budget, pulled);
    }

    private static int pullFromSides(
            final Level level,
            final BusCableBlockEntity cable,
            final BlockPos pos,
            final int budget,
            final Set<BlockPos> sourcesPulled) {
        int pulled = 0;
        for (final Direction side : Direction.values()) {
            if (pulled >= budget) {
                break;
            }
            final BlockPos neighborPos = pos.relative(side);
            final IEnergyStorage neighbor = getExternalEnergy(level, neighborPos, side.getOpposite());
            // Only pull from a pure source, never from a buffer or sink.
            if (neighbor == null || !neighbor.canExtract() || neighbor.canReceive()) {
                continue;
            }
            final int amount =
                    Math.min(
                            budget - pulled,
                            Math.min(
                                    cable.energy.getMaxEnergyStored() - cable.energy.getEnergyStored(),
                                    cable.energy.getTransferPerTick()));
            if (amount <= 0) {
                continue;
            }
            final int extracted = neighbor.extractEnergy(amount, false);
            if (extracted > 0) {
                cable.energy.receiveEnergy(extracted, false);
                sourcesPulled.add(neighborPos);
                pulled += extracted;
            }
        }
        return pulled;
    }

    private static int pullFromEu(
            final BusCableBlockEntity cable, final int budget, final int pulled) {
        int total = pulled;
        if (Ic2EuBridge.getInstance().isAvailable() && total < budget) {
            final int euPulled =
                    Ic2EuBridge.getInstance()
                            .pullEu(
                                    Math.min(
                                            (cable.energy.getMaxEnergyStored()
                                                            - cable.energy.getEnergyStored())
                                                    / Ic2EuBridge.FE_PER_EU,
                                            (budget - total) / Ic2EuBridge.FE_PER_EU),
                                    false);
            if (euPulled > 0) {
                cable.energy.receiveEnergy(euPulled * Ic2EuBridge.FE_PER_EU, false);
                total += euPulled * Ic2EuBridge.FE_PER_EU;
            }
        }
        return total;
    }

    private static int pushToSinks(
            final Level level,
            final BusCableBlockEntity cable,
            final int budget,
            final Set<BlockPos> sourcesPulled) {
        final BlockPos pos = cable.getBlockPos();
        if (cable.energy.getEnergyStored() <= 0) {
            return 0;
        }
        final int pushed = pushToSides(level, cable, pos, budget, sourcesPulled);
        return pushToEu(cable, budget, pushed);
    }

    private static int pushToSides(
            final Level level,
            final BusCableBlockEntity cable,
            final BlockPos pos,
            final int budget,
            final Set<BlockPos> sourcesPulled) {
        int pushed = 0;
        for (final Direction side : Direction.values()) {
            if (pushed >= budget) {
                break;
            }
            final BlockPos neighborPos = pos.relative(side);
            // Never give energy back to a source we just pulled from in the same tick.
            if (sourcesPulled.contains(neighborPos)) {
                continue;
            }
            final IEnergyStorage neighbor = getExternalEnergy(level, neighborPos, side.getOpposite());
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }
            final int amount =
                    Math.min(
                            budget - pushed,
                            Math.min(cable.energy.getEnergyStored(), cable.energy.getTransferPerTick()));
            if (amount <= 0) {
                continue;
            }
            final int accepted = neighbor.receiveEnergy(amount, false);
            if (accepted > 0) {
                cable.energy.extractEnergy(accepted, false);
                pushed += accepted;
            }
        }
        return pushed;
    }

    private static int pushToEu(
            final BusCableBlockEntity cable, final int budget, final int pushed) {
        int total = pushed;
        if (Ic2EuBridge.getInstance().isAvailable() && total < budget) {
            final int euAccepted =
                    Ic2EuBridge.getInstance()
                            .pushEu(
                                    Math.min(
                                            cable.energy.getEnergyStored() / Ic2EuBridge.FE_PER_EU,
                                            (budget - total) / Ic2EuBridge.FE_PER_EU),
                                    false);
            if (euAccepted > 0) {
                cable.energy.extractEnergy(euAccepted * Ic2EuBridge.FE_PER_EU, false);
                total += euAccepted * Ic2EuBridge.FE_PER_EU;
            }
        }
        return total;
    }

    private static void redistribute(final List<BusCableBlockEntity> cables) {
        if (cables.size() < 2) {
            return;
        }
        long total = 0;
        long totalCapacity = 0;
        for (final BusCableBlockEntity cable : cables) {
            total += cable.energy.getEnergyStored();
            totalCapacity += cable.energy.getMaxEnergyStored();
        }
        final long[] target = new long[cables.size()];
        long assigned = 0;
        for (int i = 0; i < cables.size(); i++) {
            final long capacity = cables.get(i).energy.getMaxEnergyStored();
            if (i == cables.size() - 1) {
                target[i] = total - assigned;
            } else {
                target[i] = Math.min(capacity, Math.round(capacity * (double) total / totalCapacity));
                assigned += target[i];
            }
        }
        redistributeDeficits(cables, target);
    }

    private static void redistributeDeficits(
            final List<BusCableBlockEntity> cables, final long... target) {
        for (int i = 0; i < cables.size(); i++) {
            long deficit = target[i] - cables.get(i).energy.getEnergyStored();
            if (deficit <= 0) {
                continue;
            }
            for (int j = 0; j < cables.size() && deficit > 0; j++) {
                if (i == j) {
                    continue;
                }
                final long surplus = cables.get(j).energy.getEnergyStored() - target[j];
                if (surplus <= 0) {
                    continue;
                }
                final long take = Math.min(deficit, surplus);
                cables.get(j).energy.extractEnergy((int) take, false);
                cables.get(i).energy.receiveEnergy((int) take, false);
                deficit -= take;
            }
        }
    }

    private static IEnergyStorage getExternalEnergy(
            final Level level, final BlockPos pos, final Direction side) {
        if (level.getBlockState(pos).getBlock().equals(Blocks.BUS_CABLE.get())) {
            return null;
        }
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
    }

    private static boolean isCable(
            final Level level, final BlockPos pos, final Direction facing) {
        final BlockState state = level.getBlockState(pos);
        if (!state.getBlock().equals(Blocks.BUS_CABLE.get())
                || !state.getValue(BusCableStateProperties.HAS_CABLE)) {
            return false;
        }
        final ConnectionType connectionType =
                BusCableStateProperties.getConnectionType(state, facing);
        return connectionType == ConnectionType.CABLE || connectionType == ConnectionType.INTERFACE;
    }
}
