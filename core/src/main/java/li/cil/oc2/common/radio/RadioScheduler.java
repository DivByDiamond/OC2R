package li.cil.oc2.common.radio;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.*;

/** Tick-thread-only trusted local Ethernet scheduler. This transport does not provide TLS. */
@SuppressWarnings("PMD.UseConcurrentHashMap") // Tick thread only, see above.
public final class RadioScheduler {
    public record Config(
            int hopDelay,
            int maxTtl,
            int maxFrameBytes,
            int globalQueue,
            int networkQueue,
            int deviceQueue,
            int dedupEntries,
            long lifetimeTicks,
            int bytesPerTick,
            int maxDelayTicks) {
        public Config {
            if (hopDelay < 1
                    || maxTtl < 1
                    || maxFrameBytes < 1
                    || globalQueue < 1
                    || networkQueue < 1
                    || deviceQueue < 1
                    || dedupEntries < 1
                    || lifetimeTicks < 1
                    || bytesPerTick < 1
                    || maxDelayTicks < 1)
                throw new IllegalArgumentException("Invalid radio limits");
        }

        /** Unlimited-bandwidth form kept for callers that do not model link speed. */
        public Config(
                int hopDelay,
                int maxTtl,
                int maxFrameBytes,
                int globalQueue,
                int networkQueue,
                int deviceQueue,
                int dedupEntries,
                long lifetimeTicks) {
            this(hopDelay, maxTtl, maxFrameBytes, globalQueue, networkQueue, deviceQueue,
                    dedupEntries, lifetimeTicks, Integer.MAX_VALUE);
        }

        /** Hop-proportional delay form (no cap on the total delivery delay). */
        public Config(
                int hopDelay,
                int maxTtl,
                int maxFrameBytes,
                int globalQueue,
                int networkQueue,
                int deviceQueue,
                int dedupEntries,
                long lifetimeTicks,
                int bytesPerTick) {
            this(hopDelay, maxTtl, maxFrameBytes, globalQueue, networkQueue, deviceQueue,
                    dedupEntries, lifetimeTicks, bytesPerTick, Integer.MAX_VALUE);
        }

        public static Config defaults() {
            return new Config(1, 64, 2048, 4096, 1024, 256, 8192, 200);
        }
    }

    public enum Result {
        ACCEPTED,
        INVALID_FRAME,
        UNAUTHORIZED,
        NO_ROUTE,
        TTL_EXCEEDED,
        DUPLICATE,
        QUEUE_FULL
    }

    @FunctionalInterface
    public interface Delivery {
        void deliver(UUID source, UUID target, byte[] frame, int remainingTtl);
    }

    private record Key(UUID packet, UUID source, UUID target) {}

    private record Pending(
            Key key,
            UUID network,
            long sourceGeneration,
            long targetGeneration,
            List<UUID> route,
            List<Long> generations,
            byte[] frame,
            int remainingTtl,
            long due,
            long deadline,
            long sequence) {}

    private final RadioRegistry registry;
    private final RadioTopology topology;
    private final Config config;
    private final PriorityQueue<Pending> queue =
            new PriorityQueue<>(
                    Comparator.comparingLong(Pending::due).thenComparingLong(Pending::sequence));
    private final LinkedHashMap<Key, Long> seen = new LinkedHashMap<>();
    /** Tick at which each sender's radio link finishes transmitting what it already accepted. */
    private final Map<UUID, Long> linkBusyUntil = new HashMap<>();
    private long sequence;
    private long clock = -1;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The scheduler works on the live registry and topology of its level.")
    public RadioScheduler(
            final RadioRegistry registry, final RadioTopology topology, final Config config) {
        this.registry = Objects.requireNonNull(registry);
        this.topology = Objects.requireNonNull(topology);
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Source identity must come from the server-owned, physically attached endpoint, never a guest
     * payload.
     */
    @SuppressWarnings("PMD.CyclomaticComplexity") // One guard per admission limit.
    public Result submit(
            final UUID packet,
            final UUID source,
            final UUID target,
            final byte[] frame,
            final int ttl,
            final long tick) {
        advance(tick);
        if (packet == null
                || frame == null
                || frame.length < 1
                || frame.length > config.maxFrameBytes()) return Result.INVALID_FRAME;
        final var from = registry.member(source);
        final var to = registry.member(target);
        if (from.isEmpty()
                || to.isEmpty()
                || !from.get().network().equals(to.get().network())
                || !from.get().dimension().equals(to.get().dimension())) return Result.UNAUTHORIZED;
        final List<UUID> route = topology.route(source, target);
        if (route.isEmpty() || !validMembers(route, from.get().network(), from.get().dimension()))
            return Result.NO_ROUTE;
        final int hops = route.size() - 1;
        if (ttl < hops || ttl < 1 || ttl > config.maxTtl()) return Result.TTL_EXCEEDED;
        final Key key = new Key(packet, source, target);
        if (seen.containsKey(key)) return Result.DUPLICATE;
        if (queue.size() >= config.globalQueue()
                || queue.stream().filter(p -> p.network().equals(from.get().network())).count()
                        >= config.networkQueue()
                || queue.stream()
                                .filter(
                                        p ->
                                                p.key().source().equals(source)
                                                        || p.key().target().equals(source)
                                                        || p.key().source().equals(target)
                                                        || p.key().target().equals(target))
                                .count()
                        >= config.deviceQueue()
                || seen.size() >= config.dedupEntries()) return Result.QUEUE_FULL;
        final long deadline = add(tick, config.lifetimeTicks());
        // Serialize on the sender's link: a frame occupies ceil(bytes / bytesPerTick) ticks, so
        // throughput is capped and order is preserved. A backlog past the packet lifetime is refused.
        long lastTxTick = tick;
        if (config.bytesPerTick() != Integer.MAX_VALUE) {
            final long occupied =
                    (frame.length + (long) config.bytesPerTick() - 1) / config.bytesPerTick();
            final long start = Math.max(tick, linkBusyUntil.getOrDefault(source, tick));
            lastTxTick = add(start, occupied - 1);
            if (lastTxTick - tick > config.lifetimeTicks()) return Result.QUEUE_FULL;
            linkBusyUntil.put(source, add(start, occupied));
        }
        seen.put(key, deadline);
        queue.add(
                new Pending(
                        key,
                        from.get().network(),
                        from.get().generation(),
                        to.get().generation(),
                        route,
                        route.stream()
                                .map(id -> registry.member(id).orElseThrow().generation())
                                .toList(),
                        frame.clone(),
                        ttl - hops,
                        add(lastTxTick, Math.min(config.maxDelayTicks(), (long) Math.max(1, hops) * config.hopDelay())),
                        deadline,
                        sequence++));
        return Result.ACCEPTED;
    }

    public void tick(final long tick, final Delivery delivery) {
        advance(tick);
        while (!queue.isEmpty() && queue.peek().due() <= tick) {
            final Pending pending = queue.remove();
            final var from = registry.member(pending.key().source());
            final var to = registry.member(pending.key().target());
            if (pending.deadline() < tick
                    || from.isEmpty()
                    || to.isEmpty()
                    || from.get().generation() != pending.sourceGeneration()
                    || to.get().generation() != pending.targetGeneration()
                    || !pending.generations()
                            .equals(
                                    pending.route().stream()
                                            .map(
                                                    id ->
                                                            registry.member(id)
                                                                    .map(
                                                                            RadioRegistry.Member
                                                                                    ::generation)
                                                                    .orElse(-1L))
                                            .toList())
                    || !validMembers(pending.route(), pending.network(), from.get().dimension())
                    || !topology.route(pending.key().source(), pending.key().target())
                            .equals(pending.route())) continue;
            delivery.deliver(
                    pending.key().source(),
                    pending.key().target(),
                    pending.frame().clone(),
                    pending.remainingTtl());
        }
    }

    public int queued() {
        return queue.size();
    }

    private boolean validMembers(
            final List<UUID> route, final UUID network, final String dimension) {
        return route.stream()
                .allMatch(
                        id ->
                                registry.member(id)
                                        .filter(
                                                member ->
                                                        member.network().equals(network)
                                                                && member.dimension()
                                                                        .equals(dimension))
                                        .isPresent());
    }

    private void advance(final long tick) {
        if (tick < 0 || tick < clock)
            throw new IllegalArgumentException("Radio ticks must be monotonic");
        clock = tick;
        seen.entrySet().removeIf(entry -> entry.getValue() < tick);
        linkBusyUntil.values().removeIf(until -> until <= tick);
        queue.removeIf(pending -> pending.deadline() < tick);
    }

    private static long add(final long tick, final long delay) {
        return tick > Long.MAX_VALUE - delay ? Long.MAX_VALUE : tick + delay;
    }
}
