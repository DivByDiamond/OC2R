package li.cil.oc2.common.energy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.ic2.Ic2EuBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Moves energy between external blocks and the internal buffers of bus cables.
 *
 * <p>The network operates on a pull/push model: every tick one pass pulls energy from
 * adjacent blocks into the cables' buffers, then pushes energy from those buffers back
 * out to adjacent consumers. Neighbors are classified purely by their capability flags:
 * <ul>
 *     <li><em>Pure source</em>: can extract but not receive &mdash; only pulled from.</li>
 *     <li><em>Sink</em>: can receive &mdash; pushed to (a sink that can also extract would
 *     be both pushed to and, since it cannot receive, never pulled from).</li>
 *     <li><em>Buffer</em> (can both extract and receive): deliberately ignored, otherwise
 *     networks would drain each other's storage indefinitely.</li>
 * </ul>
 */
public final class EnergyTransferManager {
    /**
     * How often stored energy is re-balanced across the cables of a network. Re-balancing
     * every tick is not worth the quadratic cost; once a second is plenty.
     */
    private static final int REDISTRIBUTE_INTERVAL_TICKS = 20;

    private EnergyTransferManager() {}

    /**
     * Performs one distribution pass for the network the given cable belongs to.
     *
     * <p>Although every cable ticks, only the first cable to tick in a given game tick
     * actually runs a pass: it stamps {@code energyDistributionTick} on every cable of
     * the network up front, so all other cables bail out immediately afterwards.
     *
     * <p>The pass itself is pull &rarr; redistribute &rarr; push. The total amount moved
     * per pass is capped by the network transfer rate.
     */
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

        final List<BusCableBlockEntity> cables =
                EnergyNetworkCache.getCables((ServerLevel) level, cable.getBlockPos());
        if (cables.isEmpty()) {
            return;
        }
        for (final BusCableBlockEntity networkCable : cables) {
            networkCable.energyDistributionTick = gameTime;
        }

        final int networkRate = cables.get(0).energy.getTransferPerTick();

        // Pull from pure sources adjacent to the network, remembering where we pulled from
        // so we don't immediately push the energy back (ping-pong protection).
        final Set<BlockPos> sourcesPulled = new HashSet<>();
        pullFromAllSources(level, cables, networkRate, sourcesPulled);

        // Spread stored energy evenly across the network so it is reachable from any sink.
        if (gameTime - cable.energyRedistributeTick >= REDISTRIBUTE_INTERVAL_TICKS) {
            redistribute(cables);
            for (final BusCableBlockEntity networkCable : cables) {
                networkCable.energyRedistributeTick = gameTime;
            }
        }

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

    /**
     * Computes a fair-share target for every cable, proportional to its capacity, then
     * moves energy so each cable holds its target. The last cable absorbs the rounding
     * remainder so the total amount of energy is conserved exactly.
     */
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

    /**
     * Moves energy between cables until each holds at least {@code target[i]}.
     *
     * <p>{@code target[i]} is the desired amount of stored energy for cable {@code i};
     * cables above their target have a surplus, those below a deficit. For every deficit
     * cable the method walks all other cables and transfers {@code min(deficit, surplus)}
     * from each surplus cable. This is quadratic in network size, which is acceptable
     * because it only runs once per {@link #REDISTRIBUTE_INTERVAL_TICKS}. Transfers are
     * clamped by the storages themselves, so overshooting targets cannot overfill a cable.
     */
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
}
