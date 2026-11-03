package li.cil.oc2.common.blockentity.gps;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Radio topology independent of chunk loading; callers own the server-thread lifetime. */
public final class GpsNetwork {
    private final Map<UUID, GpsNetworkNode> nodes = new HashMap<>();

    public boolean put(final GpsNetworkNode node) {
        final boolean replaced = nodes.values().removeIf(other -> !other.id().equals(node.id())
                && other.position().equals(node.position()));
        return !node.equals(nodes.put(node.id(), node)) || replaced;
    }

    public boolean remove(final UUID id) {
        return nodes.remove(id) != null;
    }

    public Collection<GpsNetworkNode> nodes() {
        return List.copyOf(nodes.values());
    }

    public GpsNetworkNode requireReference(final UUID network) {
        GpsNetworkNode found = null;
        for (final GpsNetworkNode node : nodes.values()) {
            if (!node.network().equals(network) || !node.reference()) continue;
            if (found != null) throw new IllegalStateException("GPS network has multiple references");
            found = node;
        }
        if (found == null) throw new IllegalStateException("GPS reference is not configured");
        return found;
    }

    public boolean hasReference(final UUID network) {
        return nodes.values().stream().anyMatch(node -> node.network().equals(network) && node.reference());
    }

    public BlockPos offset(final UUID station, final int range) {
        final GpsNetworkNode from = nodes.get(station);
        if (from == null) throw new IllegalStateException("GPS station is not registered");
        final GpsNetworkNode reference = requireReference(from.network());
        final var pending = new ArrayDeque<GpsNetworkNode>();
        final var visited = new HashSet<UUID>();
        pending.add(from);
        visited.add(from.id());
        final double distanceSquared = (double) range * range;
        while (!pending.isEmpty()) {
            final GpsNetworkNode current = pending.remove();
            if (current.id().equals(reference.id())) return from.position().subtract(reference.position());
            for (final GpsNetworkNode next : nodes.values()) {
                if (!next.network().equals(from.network()) || visited.contains(next.id())
                        || horizontalDistanceSquared(current.position(), next.position()) > distanceSquared) continue;
                visited.add(next.id());
                pending.add(next);
            }
        }
        throw new IllegalStateException("GPS signal is unavailable");
    }

    private static double horizontalDistanceSquared(final BlockPos first, final BlockPos second) {
        final double dx = (double) first.getX() - second.getX();
        final double dz = (double) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

}
