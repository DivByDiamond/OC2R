package li.cil.oc2.common.blockentity.radio;

import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.radio.RadioTopology;
import li.cil.oc2.common.radio.security.*;
import li.cil.oc2.common.radio.service.RadioServiceBroker;
import li.cil.oc2.common.util.scheduler.ServerScheduler;

import net.minecraft.server.level.ServerLevel;

import java.util.*;

/** Reliable application stream over scheduled ciphertext; clients are leaves, never relays. */
final class OraApplicationLink implements RadioServiceBroker.Link {
    private record PairKey(UUID first, UUID second) {
        static PairKey of(UUID a, UUID b) {
            return a.compareTo(b) < 0 ? new PairKey(a, b) : new PairKey(b, a);
        }
    }

    private record Pending(byte[] bytes, Runnable delivery) {}

    private final RadioNetworkData data;
    private final ServerLevel level;
    private final Map<UUID, OraEndpoint> endpoints = new HashMap<>();
    private final Map<UUID, RadioTlsIdentity> identities = new HashMap<>();
    private final Map<PairKey, Pair> pairs = new HashMap<>();
    private final RadioTlsTrust trust = new RadioTlsTrust();
    private final Map<UUID, Integer> networkFrames = new HashMap<>(),
            deviceFrames = new HashMap<>();
    private final Map<UUID, Long> networkDelivery = new HashMap<>();
    private int queuedFrames;

    OraApplicationLink(RadioNetworkData data, ServerLevel level) {
        this.data = data;
        this.level = level;
    }

    void attach(OraEndpoint endpoint) {
        var member =
                data.registry()
                        .authenticate(endpoint.id(), endpoint.credential())
                        .orElseThrow(() -> new SecurityException("Endpoint is not enrolled"));
        if (!member.dimension().equals(endpoint.dimension()))
            throw new SecurityException("Endpoint changed dimension");
        var previous = endpoints.get(endpoint.id());
        if (previous != null && previous != endpoint && previous.online())
            throw new SecurityException("Endpoint identity is already active");
        endpoints.put(endpoint.id(), endpoint);
        var identity = identities.get(endpoint.id());
        if (identity == null || identity.generation() != member.generation()) {
            try {
                identity =
                        RadioTlsIdentity.restore(
                                member.id(), member.generation(), endpoint.tlsPrivateMaterial());
            } catch (IllegalArgumentException invalid) {
                identity = RadioTlsIdentity.create(member.id(), member.generation());
                endpoint.tlsPrivateMaterial(identity.encodedPrivateMaterial());
            }
            identities.put(member.id(), identity);
            trust.enroll(member.id(), member.generation(), identity.certificate());
        }
    }

    @Override
    public boolean online(UUID id) {
        var endpoint = endpoints.get(id);
        if (endpoint == null || !endpoint.online()) return false;
        var member = data.registry().authenticate(id, endpoint.credential());
        return member.isPresent()
                && member.get().dimension().equals(endpoint.dimension())
                && relay(id).isPresent();
    }

    private Optional<RadioTopology.Node> relay(UUID id) {
        var endpoint = endpoints.get(id);
        if (endpoint == null) return Optional.empty();
        var own = data.topology().node(id);
        if (own.isPresent() && own.get().online()) return own;
        return data.attachment(level, id, endpoint.credential(), endpoint.x(), endpoint.z());
    }

    private List<UUID> route(UUID source, UUID target) {
        if (!online(source) || !online(target)) return List.of();
        var first = relay(source).orElseThrow();
        var last = relay(target).orElseThrow();
        return data.topology().route(first.device(), last.device());
    }

    @Override
    public RadioServiceBroker.Estimate estimate(UUID source, UUID target) {
        var path = route(source, target);
        if (path.isEmpty()) return new RadioServiceBroker.Estimate(-1, -1);
        if (path.size() - 1 > Config.radioMaxHops) return new RadioServiceBroker.Estimate(-1, -1);
        int legs = path.size() - 1;
        if (!source.equals(path.getFirst())) legs++;
        if (!target.equals(path.getLast())) legs++;
        return new RadioServiceBroker.Estimate(
                legs,
                Math.max(
                        1,
                        Math.min(
                                Config.radioMaxDeliveryTicks,
                                (long) legs * Config.radioHopDelayTicks)));
    }

    @Override
    public String security() {
        return "TLSv1.3-mutual-authentication";
    }

    @Override
    public boolean send(
            UUID source, UUID target, UUID packet, byte[] payload, long tick, Runnable delivery) {
        if (source.equals(target) || route(source, target).isEmpty()) return false;
        var key = PairKey.of(source, target);
        var pair = pairs.get(key);
        if (pair != null && (!pair.valid() || pair.first.isClosed() || pair.second.isClosed())) {
            pair.close();
            pairs.remove(key);
            pair = null;
        }
        if (pair == null) {
            if (pairs.size() >= 256) return false;
            pair = new Pair(key);
            pairs.put(key, pair);
            pair.first.start();
            pair.second.start();
        }
        return pair.send(source, payload, delivery);
    }

    void tick() {
        for (var entry : List.copyOf(endpoints.entrySet())) {
            var member =
                    data.registry().authenticate(entry.getKey(), entry.getValue().credential());
            if (!entry.getValue().online() || member.isEmpty()) {
                trust.revoke(entry.getKey());
                endpoints.remove(entry.getKey());
                identities.remove(entry.getKey());
            } else if (identities.get(entry.getKey()).generation() != member.get().generation())
                attach(entry.getValue());
        }
        for (var entry : List.copyOf(pairs.entrySet())) {
            var pair = entry.getValue();
            if (!pair.valid() || pair.first.isClosed() || pair.second.isClosed()) {
                pair.close();
                pairs.remove(entry.getKey());
            } else {
                pair.first.tick();
                pair.second.tick();
            }
        }
    }

    private static void decrement(Map<UUID, Integer> counts, UUID key) {
        counts.computeIfPresent(key, (ignored, count) -> count <= 1 ? null : count - 1);
    }

    private final class Pair {
        final PairKey key;
        final long firstGeneration, secondGeneration;
        final RadioTlsChannel first, second;
        final ArrayDeque<Pending> toFirst = new ArrayDeque<>(), toSecond = new ArrayDeque<>();
        long firstDelivery, secondDelivery;
        int queuedCiphertext;
        boolean closed;

        Pair(PairKey key) {
            this.key = key;
            var a = identities.get(key.first());
            var b = identities.get(key.second());
            firstGeneration = a.generation();
            secondGeneration = b.generation();
            first =
                    new RadioTlsChannel(
                            a,
                            trust,
                            b.id(),
                            b.generation(),
                            true,
                            bytes -> ciphertext(true, bytes),
                            bytes -> received(toFirst, bytes));
            second =
                    new RadioTlsChannel(
                            b,
                            trust,
                            a.id(),
                            a.generation(),
                            false,
                            bytes -> ciphertext(false, bytes),
                            bytes -> received(toSecond, bytes));
        }

        boolean valid() {
            var a = data.registry().member(key.first());
            var b = data.registry().member(key.second());
            return !closed
                    && online(key.first())
                    && online(key.second())
                    && a.isPresent()
                    && b.isPresent()
                    && a.get().generation() == firstGeneration
                    && b.get().generation() == secondGeneration
                    && !route(key.first(), key.second()).isEmpty();
        }

        boolean send(UUID source, byte[] bytes, Runnable delivery) {
            var queue = source.equals(key.first()) ? toSecond : toFirst;
            if (queue.size() >= 256) return false;
            queue.add(new Pending(bytes.clone(), delivery));
            boolean accepted = (source.equals(key.first()) ? first : second).send(bytes);
            if (!accepted) close();
            return accepted;
        }

        void received(ArrayDeque<Pending> queue, byte[] plaintext) {
            if (!valid() || queue.isEmpty()) {
                close();
                return;
            }
            var pending = queue.remove();
            if (!Arrays.equals(pending.bytes(), plaintext)) {
                close();
                return;
            }
            pending.delivery().run();
        }

        void ciphertext(boolean fromFirst, byte[] ciphertext) {
            UUID source = fromFirst ? key.first() : key.second(),
                    target = fromFirst ? key.second() : key.first();
            var estimate = estimate(source, target);
            if (estimate.hops() < 0 || queuedCiphertext + ciphertext.length > 1048576) {
                close();
                return;
            }
            final int fragmentSize = Math.max(1, Math.min(16384, Config.radioMaxFrameBytes));
            final int fragments = (ciphertext.length + fragmentSize - 1) / fragmentSize;
            final UUID network = data.registry().member(source).orElseThrow().network();
            if (queuedFrames + fragments > Config.radioQueueLimit
                    || networkFrames.getOrDefault(network, 0) + fragments
                            > Config.radioNetworkQueueLimit
                    || deviceFrames.getOrDefault(source, 0) + fragments
                            > Config.radioDeviceQueueLimit) {
                close();
                return;
            }
            long now = level.getGameTime();
            long firstDue =
                    Math.max(
                            now + estimate.nominalTicks(),
                            Math.max(
                                    (fromFirst ? firstDelivery : secondDelivery) + 1,
                                    networkDelivery.getOrDefault(network, 0L) + 1));
            final long airtime =
                    (ciphertext.length + (long) Config.radioLinkBytesPerTick - 1)
                                    / Config.radioLinkBytesPerTick
                            + fragments
                            - 1;
            if (firstDue + airtime - 1 - now > Config.radioPacketLifetimeTicks) {
                close();
                return;
            }
            long dueCursor = firstDue;
            for (int offset = 0; offset < ciphertext.length; offset += fragmentSize) {
                byte[] copy =
                        Arrays.copyOfRange(
                                ciphertext,
                                offset,
                                Math.min(ciphertext.length, offset + fragmentSize));
                queuedCiphertext += copy.length;
                queuedFrames++;
                networkFrames.merge(network, 1, Integer::sum);
                deviceFrames.merge(source, 1, Integer::sum);
                final long occupied =
                        (copy.length + (long) Config.radioLinkBytesPerTick - 1)
                                / Config.radioLinkBytesPerTick;
                final long due = dueCursor + occupied - 1;
                dueCursor = due + 1;
                if (fromFirst) firstDelivery = due;
                else secondDelivery = due;
                networkDelivery.put(network, due);
                ServerScheduler.schedule(
                        level,
                        () -> {
                            queuedCiphertext -= copy.length;
                            queuedFrames--;
                            decrement(networkFrames, network);
                            decrement(deviceFrames, source);
                            if (!valid()) {
                                close();
                                return;
                            }
                            (fromFirst ? second : first).receive(copy);
                        },
                        (int) Math.min(Integer.MAX_VALUE, due - now));
            }
        }

        void close() {
            closed = true;
            first.close();
            second.close();
            toFirst.clear();
            toSecond.clear();
        }
    }
}
