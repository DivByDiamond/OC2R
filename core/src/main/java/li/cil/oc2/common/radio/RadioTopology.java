package li.cil.oc2.common.radio;

import java.util.*;

/** Saved relays remain traversable while unloaded; only online endpoints send and receive. */
public final class RadioTopology {
    public record Node(
            UUID device,
            UUID network,
            String dimension,
            double x,
            double y,
            double z,
            double range,
            boolean online) {
        public Node {
            Objects.requireNonNull(device);
            Objects.requireNonNull(network);
            Objects.requireNonNull(dimension);
            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(z)
                    || !Double.isFinite(range)
                    || range < 0) throw new IllegalArgumentException("Invalid radio geometry");
        }
    }

    private final Map<UUID, Node> nodes = new HashMap<>();
    private final LinkedHashMap<UUID, Map<UUID, UUID>> routeTrees = new LinkedHashMap<>(8, 0.75F, true);

    public void upsert(final Node node) {
        if (!nodes.containsKey(node.device()) && nodes.size() >= 1024)
            throw new IllegalStateException("Relay capacity exceeded");
        if (!node.equals(nodes.put(node.device(), node))) routeTrees.clear();
    }

    public void remove(final UUID device) {
        if (nodes.remove(device) != null) routeTrees.clear();
    }

    public Optional<Node> node(final UUID device) {
        return Optional.ofNullable(nodes.get(device));
    }

    public List<Node> nodes() {
        return nodes.values().stream().sorted(Comparator.comparing(Node::device)).toList();
    }

    public List<UUID> route(final UUID source, final UUID target) {
        final Node start = nodes.get(source), end = nodes.get(target);
        if (start == null
                || end == null
                || !start.online()
                || !end.online()
                || !start.network().equals(end.network())
                || !start.dimension().equals(end.dimension())) return List.of();
        Map<UUID, UUID> previous = routeTrees.get(source);
        if (previous == null) {
            previous = buildTree(source);
            if (routeTrees.size() >= 8) routeTrees.remove(routeTrees.keySet().iterator().next());
            routeTrees.put(source, previous);
        }
        if (!previous.containsKey(target)) return List.of();
        final ArrayList<UUID> path = new ArrayList<>();
        UUID next = target;
        while (!next.equals(source)) {
            path.add(next);
            next = previous.get(next);
        }
        path.add(source);
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private Map<UUID, UUID> buildTree(final UUID source) {
        final Map<UUID, UUID> previous = new HashMap<>();
        final ArrayDeque<UUID> queue = new ArrayDeque<>();
        previous.put(source, source);
        queue.add(source);
        final List<Node> ordered = nodes();
        while (!queue.isEmpty()) {
            final Node node = nodes.get(queue.remove());
            for (final Node other : ordered) {
                if (!previous.containsKey(other.device())
                        && node.network().equals(other.network())
                        && node.dimension().equals(other.dimension())
                        && connected(node, other)) {
                    previous.put(other.device(), node.device());
                    queue.add(other.device());
                }
            }
        }
        return Map.copyOf(previous);
    }

    private static boolean connected(final Node first, final Node second) {
        final double radius = Math.min(first.range(), second.range());
        final double dx = first.x() - second.x(), dz = first.z() - second.z();
        return dx * dx + dz * dz <= radius * radius;
    }
}
