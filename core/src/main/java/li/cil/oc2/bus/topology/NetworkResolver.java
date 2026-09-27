package li.cil.oc2.bus.topology;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Resolves the network a node belongs to with a fresh breadth-first walk (docs/CABLE-SYSTEM.md).
 *
 * <p>Nothing here is persisted or cached: callers run a resolve after a topology event and discard the
 * result when the next event arrives, so there is no network state that can go stale.
 */
public final class NetworkResolver {
    private NetworkResolver() {}

    /**
     * Outcome of a resolve.
     *
     * @param nodes    nodes reachable from the root within the limit, in breadth-first order, root first;
     *                 exactly {@code min(limit, size of the component)} nodes
     * @param overflow every other node reachable from the root, i.e. the rest of the component beyond
     *                 the limit; they are excluded from the network instead of invalidating it, so one
     *                 device over the limit never takes the rest down
     * @param incomplete {@code true} if a node <em>inside</em> {@code nodes} could not report its
     *                 neighbors yet (for example because its chunk is not loaded); the network is not
     *                 usable until a later topology event. Nodes beyond the limit that cannot report
     *                 their neighbors are simply left out of {@code overflow}
     */
    // resolve() hands out freshly built sets that no code mutates afterwards; the record only
    // shares them with read-only callers. SpotBugs's own UselessSuppression detector claims this
    // is unneeded on a first (uncached) run of :core:spotbugsMain, then reports the real
    // EI_EXPOSE_REP/REP2 findings once it is removed — see the matching exclude-filter.xml entry
    // for the UselessSuppression false positive that goes with this annotation.
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "sets are created per resolve and never mutated")
    public record Result<N>(Set<N> nodes, Set<N> overflow, boolean incomplete) {}

    /**
     * @param root      the node the walk starts at
     * @param neighbors neighbors of a node, or empty if they cannot be determined right now
     * @param limit     maximum number of nodes in {@link Result#nodes()}, including the root; at least 1
     */
    public static <N> Result<N> resolve(
            final N root, final Function<N, Optional<Collection<N>>> neighbors, final int limit) {
        final int max = Math.max(1, limit);
        final Set<N> nodes = new LinkedHashSet<>();
        final Set<N> overflow = new LinkedHashSet<>();
        final Set<N> seen = new HashSet<>();
        final Deque<N> open = new ArrayDeque<>();

        seen.add(root);
        nodes.add(root);
        open.add(root);

        while (!open.isEmpty()) {
            final N node = open.poll();
            final Optional<Collection<N>> found = neighbors.apply(node);
            if (found.isEmpty()) {
                // Only a node inside the limit can invalidate the result: past the limit the node is
                // excluded from the network anyway, so an unexpandable one just leaves a hole in the
                // overflow set instead of taking down the elements that were found.
                if (nodes.contains(node)) {
                    return new Result<>(nodes, overflow, true);
                }
                continue;
            }
            for (final N neighbor : found.get()) {
                if (neighbor == null || !seen.add(neighbor)) {
                    continue;
                }
                if (nodes.size() < max) {
                    nodes.add(neighbor);
                } else {
                    overflow.add(neighbor);
                }
                // Keep walking past the limit so overflow() ends up holding the whole remainder of
                // the component; this never grows nodes(), which stays at the limit.
                open.add(neighbor);
            }
        }
        return new Result<>(nodes, overflow, false);
    }
}
