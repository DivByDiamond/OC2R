package li.cil.oc2.common.radio;

import java.util.*;

/**
 * Chooses how a wireless client reaches the tower network. A tower in range attaches the client
 * directly. Otherwise a robot dock station in range attaches it and uplinks through the nearest
 * tower within tower range of the dock. A dock never relays between towers and radii are never
 * added together: the client must be inside the dock radius, and the dock inside a tower radius.
 * Coverage is circular in X/Z. The client is located by X/Z only, and the heights of towers and
 * docks are carried but never limit attachment.
 */
public final class RadioAccessPoints {
    /** A short-range access point at a block position. It belongs to no network. */
    public record Dock(String dimension, double x, double y, double z, double range) {
        public Dock {
            Objects.requireNonNull(dimension);
            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(z)
                    || !Double.isFinite(range)
                    || range < 0) throw new IllegalArgumentException("Invalid dock geometry");
        }
    }

    /** The tower that carries the client's traffic and, when present, the dock the client uses. */
    public record Attachment(RadioTopology.Node tower, Dock dock) {
        public Attachment {
            Objects.requireNonNull(tower);
        }

        public boolean viaDock() {
            return dock != null;
        }

        /** Radio legs between the client and its tower: one for a tower, two through a dock. */
        public int accessLegs() {
            return dock == null ? 1 : 2;
        }
    }

    private RadioAccessPoints() {}

    public static Optional<Attachment> resolve(
            final Collection<RadioTopology.Node> towers,
            final Collection<Dock> docks,
            final UUID network,
            final String dimension,
            final double x,
            final double z) {
        final var direct = nearestTower(towers, network, dimension, x, z);
        if (direct.isPresent()) return Optional.of(new Attachment(direct.get(), null));
        return docks.stream()
                .filter(
                        dock ->
                                dock.dimension().equals(dimension)
                                        && within(dock.x(), dock.z(), x, z, dock.range()))
                .sorted(
                        Comparator.<Dock>comparingDouble(
                                        dock -> distanceSquared(dock.x(), dock.z(), x, z))
                                .thenComparingDouble(Dock::x)
                                .thenComparingDouble(Dock::z)
                                .thenComparingDouble(Dock::y))
                .map(
                        dock ->
                                nearestTower(towers, network, dimension, dock.x(), dock.z())
                                        .map(tower -> new Attachment(tower, dock)))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<RadioTopology.Node> nearestTower(
            final Collection<RadioTopology.Node> towers,
            final UUID network,
            final String dimension,
            final double x,
            final double z) {
        return towers.stream()
                .filter(
                        node ->
                                node.online()
                                        && node.network().equals(network)
                                        && node.dimension().equals(dimension)
                                        && within(node.x(), node.z(), x, z, node.range()))
                .min(
                        Comparator.<RadioTopology.Node>comparingDouble(
                                        node -> distanceSquared(node.x(), node.z(), x, z))
                                .thenComparing(RadioTopology.Node::device));
    }

    private static boolean within(
            final double ax,
            final double az,
            final double bx,
            final double bz,
            final double range) {
        return distanceSquared(ax, az, bx, bz) <= range * range;
    }

    private static double distanceSquared(
            final double ax, final double az, final double bx, final double bz) {
        final double dx = ax - bx, dz = az - bz;
        return dx * dx + dz * dz;
    }
}
