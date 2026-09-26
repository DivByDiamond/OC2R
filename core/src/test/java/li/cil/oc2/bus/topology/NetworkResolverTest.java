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
            final var list = new java.util.ArrayList<Integer>();
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
        assertTrue(result.overflow().contains(4));
        assertFalse(result.incomplete());
    }

    @Test
    void unknownNeighborsMarkResultIncomplete() {
        final var graph = line(4);
        final var result = NetworkResolver.resolve(0, n -> n == 2 ? Optional.empty() : Optional.of(graph.get(n)), 100);
        assertTrue(result.incomplete());
    }

    @Test
    void handlesCyclesAndNullNeighbors() {
        final var graph = Map.<Integer, Collection<Integer>>of(
                0, java.util.Arrays.asList(1, null), 1, List.of(2, 0), 2, List.of(0, 1));
        final var result = NetworkResolver.resolve(0, n -> Optional.of(graph.get(n)), 100);
        assertEquals(Set.of(0, 1, 2), result.nodes());
    }

    @Test
    void limitOfZeroStillKeepsRoot() {
        final var result = NetworkResolver.resolve(0, n -> Optional.of(List.of(1)), 0);
        assertEquals(Set.of(0), result.nodes());
    }
}
