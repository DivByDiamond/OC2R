package li.cil.oc2.common.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class RadioAccessPointsTest {
    private static final UUID NETWORK = UUID.randomUUID();
    private static final String DIM = "minecraft:overworld";
    private static final double TOWER_RANGE = 45;
    private static final double DOCK_RANGE = 10;

    private static RadioTopology.Node tower(final double x, final double z) {
        return new RadioTopology.Node(UUID.randomUUID(), NETWORK, DIM, x, 64, z, TOWER_RANGE, true);
    }

    private static RadioAccessPoints.Dock dock(final double x, final double z) {
        return new RadioAccessPoints.Dock(DIM, x, 64, z, DOCK_RANGE);
    }

    private static Optional<RadioAccessPoints.Attachment> resolve(
            final List<RadioTopology.Node> towers,
            final List<RadioAccessPoints.Dock> docks,
            final double x,
            final double z) {
        return RadioAccessPoints.resolve(towers, docks, NETWORK, DIM, x, z);
    }

    private static RadioTopology.Node towerAt(final double x, final double y, final double z) {
        return new RadioTopology.Node(UUID.randomUUID(), NETWORK, DIM, x, y, z, TOWER_RANGE, true);
    }

    @Test
    void towerInRangeAttachesDirectlyEvenWithADockNearby() {
        final var tower = tower(0, 0);
        final var result = resolve(List.of(tower), List.of(dock(40, 0)), 44, 0).orElseThrow();
        assertFalse(result.viaDock());
        assertEquals(1, result.accessLegs());
        assertSame(tower, result.tower());
    }

    @Test
    void dockAttachesAClientNoTowerCoversAndUplinksThroughTheTower() {
        final var tower = tower(0, 0);
        final var dock = dock(40, 0);
        final var result = resolve(List.of(tower), List.of(dock), 49, 0).orElseThrow();
        assertTrue(result.viaDock());
        assertEquals(dock, result.dock());
        assertSame(tower, result.tower());
        assertEquals(2, result.accessLegs());
    }

    @Test
    void dockRadiusBoundaryIsInclusiveAndCircular() {
        final var towers = List.of(tower(0, 0));
        final var docks = List.of(dock(40, 0));
        // Exactly 10 blocks from the dock attaches, a hair further does not.
        assertTrue(resolve(towers, docks, 50, 0).isPresent());
        assertTrue(resolve(towers, docks, 50.001, 0).isEmpty());
        // 6/8/10 triangle: exactly on the circle diagonally.
        assertTrue(resolve(towers, docks, 46, 8).isPresent());
        // The corner of the bounding square (7.5, 7.5) lies outside the circle (10.6 blocks).
        assertTrue(resolve(towers, docks, 47.5, 7.5).isEmpty());
    }

    @Test
    void heightNeverLimitsAttachment() {
        // Towers and docks at extreme heights still cover the client by X/Z distance alone.
        final double[] heights = {-2048, -64, 320, 4096};
        for (final double towerY : heights) {
            for (final double dockY : heights) {
                final var tower = towerAt(0, towerY, 0);
                final var dock = new RadioAccessPoints.Dock(DIM, 40, dockY, 0, DOCK_RANGE); // NOPMD - one per case
                final var through = resolve(List.of(tower), List.of(dock), 50, 0);
                assertTrue(through.isPresent() && through.get().viaDock(), "dock at y=" + dockY + ", tower at y=" + towerY);
                assertTrue(resolve(List.of(tower), List.of(dock), 50.001, 0).isEmpty());
            }
            final var direct = resolve(List.of(towerAt(0, towerY, 0)), List.of(), 45, 0);
            assertTrue(direct.isPresent() && !direct.get().viaDock(), "tower at y=" + towerY);
        }
    }

    @Test
    void dockWithoutATowerInRangeIsIsolated() {
        // The dock is 46 blocks from the only tower.
        assertTrue(resolve(List.of(tower(0, 0)), List.of(dock(46, 0)), 52, 0).isEmpty());
        assertTrue(resolve(List.of(), List.of(dock(0, 0)), 1, 0).isEmpty());
        final var offline = new RadioTopology.Node(UUID.randomUUID(), NETWORK, DIM, 0, 64, 0, TOWER_RANGE, false);
        assertTrue(resolve(List.of(offline), List.of(dock(40, 0)), 49, 0).isEmpty());
    }

    @Test
    void radiiAreNeverAddedTogether() {
        final var tower = tower(0, 0);
        // 46 blocks from the tower with no dock: unattached.
        assertTrue(resolve(List.of(tower), List.of(), 46, 0).isEmpty());
        // The dock is at the edge of the tower radius; 55.5 is 10.5 from the dock.
        assertTrue(resolve(List.of(tower), List.of(dock(45, 0)), 55.5, 0).isEmpty());
        assertTrue(resolve(List.of(tower), List.of(dock(45, 0)), 55, 0).isPresent());
    }

    @Test
    void dockUplinksThroughTheNearestTowerOfTheClientsNetwork() {
        // Both own towers reach the dock at 50 (45 and 40 blocks); the foreign tower is nearer still.
        final var far = tower(5, 0);
        final var near = tower(10, 0);
        final var foreign = new RadioTopology.Node(UUID.randomUUID(), UUID.randomUUID(), DIM, 14, 64, 0, TOWER_RANGE, true);
        final var result = resolve(List.of(far, foreign, near), List.of(dock(50, 0)), 59, 0).orElseThrow();
        assertSame(near, result.tower());
        assertTrue(resolve(List.of(far), List.of(dock(50, 0)), 59, 0).isPresent());
    }

    @Test
    void nearestDockWithATowerUplinkCarriesTheClient() {
        final var tower = tower(0, 0);
        // The closer dock at 46.5 has no tower uplink, so the farther dock at 44 carries the client.
        final var viable = resolve(List.of(tower), List.of(dock(46.5, 0), dock(44, 0)), 52, 0).orElseThrow();
        assertEquals(44, viable.dock().x());
        // Among two viable docks the nearer one wins.
        final var nearer = resolve(List.of(tower), List.of(dock(40, 0), dock(44, 0)), 49, 0).orElseThrow();
        assertEquals(44, nearer.dock().x());
        // Only the dock without an uplink is in range: the client stays unattached.
        assertTrue(resolve(List.of(tower), List.of(dock(46.5, 0)), 52, 0).isEmpty());
    }

    @Test
    void otherDimensionsAndNetworksNeverAttach() {
        final var tower = tower(0, 0);
        final var foreignDock = new RadioAccessPoints.Dock("minecraft:the_nether", 40, 64, 0, DOCK_RANGE);
        assertTrue(resolve(List.of(tower), List.of(foreignDock), 49, 0).isEmpty());
        final var netherTower = new RadioTopology.Node(UUID.randomUUID(), NETWORK, "minecraft:the_nether", 0, 64, 0, TOWER_RANGE, true);
        assertTrue(resolve(List.of(netherTower), List.of(dock(40, 0)), 49, 0).isEmpty());
        final var foreignTower = new RadioTopology.Node(UUID.randomUUID(), UUID.randomUUID(), DIM, 0, 64, 0, TOWER_RANGE, true);
        assertTrue(resolve(List.of(foreignTower), List.of(dock(40, 0)), 41, 0).isEmpty());
    }

    @Test
    void rejectsInvalidDockGeometry() {
        assertThrows(IllegalArgumentException.class, () -> new RadioAccessPoints.Dock(DIM, Double.NaN, 0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new RadioAccessPoints.Dock(DIM, 0, 0, 0, -1));
        assertThrows(NullPointerException.class, () -> new RadioAccessPoints.Dock(null, 0, 0, 0, 10));
    }
}
