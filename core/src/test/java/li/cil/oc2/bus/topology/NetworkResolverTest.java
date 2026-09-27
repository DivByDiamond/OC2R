package li.cil.oc2.bus.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkResolverTest {
    private static Map<Integer, Collection<Integer>> line(final int n) {
        final var graph = new java.util.HashMap<Integer, Collection<Integer>>();
        for (int i = 0; i < n; i++) {
            final var list = new java.util.ArrayList<Integer>(); // NOPMD: one fresh list per node is the whole point of the fixture graph.
            if (i > 0) list.add(i - 1);
            if (i < n - 1) list.add(i + 1);
            graph.put(i, list);
        }
        return graph;
    }

    @Test
    void resolvesWholeConnectedComponentRootFirst() {
        final var graph = line(5);
        final var result = NetworkResolver.resolve(2, n -> Optional.of(graph.get(n)), 100);
        assertEquals(Set.of(0, 1, 2, 3, 4), result.nodes());
        assertEquals(2, result.nodes().iterator().next());
        assertTrue(result.overflow().isEmpty());
        assertFalse(result.incomplete());
    }

    @Test
    void doesNotCrossIntoDisconnectedComponent() {
        final var graph = new java.util.HashMap<>(line(3));
        graph.put(10, List.of(11));
        graph.put(11, List.of(10));
        final var result = NetworkResolver.resolve(0, n -> Optional.of(graph.get(n)), 100);
        assertEquals(Set.of(0, 1, 2), result.nodes());
    }

    @Test
    void overLimitNodesAreFlaggedNotFatal() {
        final var graph = line(10);
        final var result = NetworkResolver.resolve(0, n -> Optional.of(graph.get(n)), 4);
        assertEquals(4, result.nodes().size());
        assertEquals(Set.of(0, 1, 2, 3), result.nodes());
        // Everything past the limit is reported, not just the first hop beyond it.
        assertEquals(Set.of(4, 5, 6, 7, 8, 9), result.overflow());
        assertFalse(result.incomplete());
    }

    @Test
    void overflowKeepsWalkingTheWholeComponent() {
        // Branchy component: overflow must contain every reachable node beyond the limit,
        // reached by expanding other overflow nodes, not only limit-boundary neighbors.
        final var graph = new java.util.HashMap<Integer, Collection<Integer>>();
        for (int i = 0; i < 8; i++) {
            graph.put(i, List.of(i + 1, i + 2));
        }
        graph.put(8, List.of());
        graph.put(9, List.of());
        final var result = NetworkResolver.resolve(0, n -> Optional.of(graph.get(n)), 3);
        assertEquals(Set.of(0, 1, 2), result.nodes());
        assertEquals(Set.of(3, 4, 5, 6, 7, 8, 9), result.overflow());
        assertFalse(result.incomplete());
    }

    @Test
    void unknownNeighborsMarkResultIncomplete() {
        final var graph = line(4);
        final var result = NetworkResolver.resolve(0, n -> n == 2 ? Optional.empty() : Optional.of(graph.get(n)), 100);
        assertTrue(result.incomplete());
    }

    @Test
    void unknownNeighborsBeyondLimitDoNotInvalidate() {
        // Node 5 is beyond the limit of 4, so it is excluded from the network: not knowing its
        // neighbors must not invalidate a network whose in-limit part resolved fine.
        final var graph = line(10);
        final var result =
                NetworkResolver.resolve(
                        0, n -> n == 5 ? Optional.empty() : Optional.of(graph.get(n)), 4);
        assertEquals(Set.of(0, 1, 2, 3), result.nodes());
        assertFalse(result.incomplete());
        assertTrue(result.overflow().contains(4));
        // Not expandable, so its own neighbors stay undiscovered; 5 itself is still reported.
        assertTrue(result.overflow().contains(5));
        assertFalse(result.overflow().contains(6));
    }

    @Test
    void handlesCyclesAndNullNeighbors() {
        final var graph = Map.<Integer, Collection<Integer>>of(
                0, java.util.Arrays.asList(1, null), 1, List.of(2, 0), 2, List.of(0, 1));
        final var result = NetworkResolver.resolve(0, n -> Optional.of(graph.get(n)), 100);
        assertEquals(Set.of(0, 1, 2), result.nodes());
    }

    @Test
    void cyclesBeyondLimitTerminateAndStayOutOfNodes() {
        final var graph = Map.<Integer, Collection<Integer>>of(
                0, List.of(1), 1, List.of(2), 2, List.of(3, 0), 3, List.of(2, 4), 4, List.of(3));
        final var result = NetworkResolver.resolve(0, n -> Optional.of(graph.get(n)), 3);
        assertEquals(Set.of(0, 1, 2), result.nodes());
        assertEquals(Set.of(3, 4), result.overflow());
        assertFalse(result.incomplete());
    }

    @Test
    void limitOfZeroStillKeepsRoot() {
        final var result = NetworkResolver.resolve(0, n -> Optional.of(List.of(1)), 0);
        assertEquals(Set.of(0), result.nodes());
        assertEquals(Set.of(1), result.overflow());
    }
}
