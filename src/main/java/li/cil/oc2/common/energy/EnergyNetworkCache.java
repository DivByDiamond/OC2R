package li.cil.oc2.common.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Caches the cable topology of energy networks so that the flood-fill does not run
 * every tick. The whole cache is invalidated whenever any cable topology changes
 * (connection change, block placed/broken, chunk unload); events are rare compared
 * to ticks, and cached entries are additionally validated against removed BEs.
 */
public final class EnergyNetworkCache {
    private static final Map<ServerLevel, Map<BlockPos, List<BusCableBlockEntity>>> CACHE =
            new HashMap<>();

    private EnergyNetworkCache() {}

    public static void invalidate() {
        CACHE.clear();
    }

    public static List<BusCableBlockEntity> getCables(
            final ServerLevel level, final BlockPos origin) {
        final Map<BlockPos, List<BusCableBlockEntity>> networks =
                CACHE.computeIfAbsent(level, it -> new HashMap<>());
        final List<BusCableBlockEntity> cached = networks.get(origin);
        if (cached != null && isValid(cached)) {
            return cached;
        }

        final List<BusCableBlockEntity> cables = collectNetworkCables(level, origin);
        networks.put(origin, cables);
        return cables;
    }

    private static boolean isValid(final List<BusCableBlockEntity> cables) {
        for (final BusCableBlockEntity cable : cables) {
            if (cable.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    private static List<BusCableBlockEntity> collectNetworkCables(
            final ServerLevel level, final BlockPos origin) {
        final List<BusCableBlockEntity> cables = new ArrayList<>();
        for (final BlockPos pos : collectNetwork(level, origin)) {
            if (level.getBlockEntity(pos) instanceof final BusCableBlockEntity cable) {
                cables.add(cable);
            }
        }
        return cables;
    }

    private static Set<BlockPos> collectNetwork(final ServerLevel level, final BlockPos origin) {
        // LinkedHashSet keeps insertion order for stable iteration.
        final Set<BlockPos> visited = new java.util.LinkedHashSet<>();
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

    private static boolean isCable(
            final ServerLevel level, final BlockPos pos, final Direction facing) {
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
