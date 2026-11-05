package li.cil.oc2.common.radio.service;

import java.util.*;
import java.util.function.LongSupplier;
import li.cil.oc2.common.radio.RadioAddress;
import li.cil.oc2.common.radio.RadioRegistry;

/** Server-thread application broker. Link implementations must schedule both directions. */
public final class RadioServiceBroker {
    public static final int MAX_ENDPOINTS = 4096,
            MAX_EVENTS = 256,
            MAX_REQUESTS = 4096,
            MAX_SUBSCRIPTIONS = 1024,
            MAX_FIELDS = 64,
            MAX_REQUESTS_PER_DEVICE = 256;

    /**
     * Ticks a finished command stays known so that a retried request ID returns the same command.
     */
    public static final long FINISHED_RETENTION_TICKS = 1200;

    public enum State {
        ACCEPTED,
        RUNNING,
        COMPLETED,
        FAILED,
        TIMED_OUT,
        DISCONNECTED
    }

    public enum Freshness {
        ONLINE,
        STALE,
        OFFLINE
    }

    public record Event(
            UUID requestId,
            State state,
            RadioMessage message,
            long sequence,
            Freshness freshness,
            long sampleTick,
            long roundTripNanos) {}

    public record Estimate(int hops, long nominalTicks) {}

    /** One member of the caller's radio network; the address is a label, never a credential. */
    public record Peer(String address, boolean online, boolean self) {}

    /** This boundary must be backed by authenticated encrypted links for secure remote control. */
    public interface Link {
        boolean online(UUID device);

        boolean send(
                UUID source,
                UUID target,
                UUID packet,
                byte[] payload,
                long tick,
                Runnable delivery);

        Estimate estimate(UUID source, UUID target);

        String security();
    }

    public interface Endpoint {
        default void message(RadioMessage message) {}

        default void command(RadioMessage message, Command command) {
            command.failed(new byte[0]);
        }
    }

    private record EndpointKey(UUID device, String service) {}

    private record RequestKey(UUID source, UUID request) {}

    private record Ping(
            Session session, UUID target, long generation, long started, long deadline) {}

    private record Sample(byte[] bytes, long tick, long sequence) {
        Sample {
            bytes = bytes.clone();
        }
    }

    private record Subscription(
            Session session,
            UUID target,
            long generation,
            String service,
            Set<String> fields,
            UUID id,
            long staleTicks) {}

    private final RadioRegistry registry;
    private final Link link;
    private final LongSupplier nanos;
    private final Map<EndpointKey, Long> endpointGenerations = new HashMap<>();
    private final Map<EndpointKey, Endpoint> endpoints = new HashMap<>();
    private final Map<EndpointKey, Map<String, Sample>> samples = new HashMap<>();
    private final Map<RequestKey, Command> requests = new LinkedHashMap<>();
    private final Map<RequestKey, Ping> pings = new HashMap<>();
    private final Map<String, Freshness> freshness = new HashMap<>();
    private final Map<UUID, Long> deliveredSequences = new HashMap<>();
    private final Map<UUID, Subscription> subscriptions = new LinkedHashMap<>();
    private long tick, sequence;

    public RadioServiceBroker(RadioRegistry registry, Link link, LongSupplier nanos) {
        this.registry = Objects.requireNonNull(registry);
        this.link = Objects.requireNonNull(link);
        this.nanos = Objects.requireNonNull(nanos);
    }

    public String security() {
        return link.security();
    }

    public Estimate estimate(Session session, RadioAddress target) {
        return link.estimate(session.device, resolve(session, target, null, null).id());
    }

    public Session connect(UUID device, String secret) {
        var member =
                registry.authenticate(device, secret)
                        .orElseThrow(() -> new SecurityException("Authentication failed"));
        return new Session(device, secret, member.generation());
    }

    /** Trusted host registration only: never expose directly as a guest RPC. */
    public void register(UUID device, String service, Endpoint endpoint) {
        RadioMessage.name(service);
        if (registry.member(device).isEmpty()) throw new SecurityException("Unknown endpoint");
        var key = new EndpointKey(device, service);
        if (!endpoints.containsKey(key) && endpoints.size() >= MAX_ENDPOINTS)
            throw new IllegalStateException("Endpoint capacity");
        endpoints.put(key, Objects.requireNonNull(endpoint));
        endpointGenerations.put(key, registry.member(device).orElseThrow().generation());
    }

    public void unregister(UUID device, String service) {
        var key = new EndpointKey(device, service);
        endpoints.remove(key);
        endpointGenerations.remove(key);
        samples.remove(key);
    }

    public void publish(UUID device, String service, String field, byte[] payload) {
        RadioMessage.name(field);
        bounded(payload);
        var key = new EndpointKey(device, service);
        if (endpoint(device, service) == null)
            throw new IllegalStateException("Unregistered endpoint");
        var fields = samples.computeIfAbsent(key, ignored -> new HashMap<>());
        if (!fields.containsKey(field) && fields.size() >= MAX_FIELDS)
            throw new IllegalStateException("Field capacity");
        var sample = new Sample(payload, tick, ++sequence);
        fields.put(field, sample);
        for (var subscription : List.copyOf(subscriptions.values()))
            if (subscription.target.equals(device)
                    && subscription.service.equals(service)
                    && subscription.fields.contains(field)) telemetry(subscription, field, sample);
    }

    public final class Session implements AutoCloseable {
        private final UUID device;
        private final String secret;
        private final long generation;
        private final ArrayDeque<Event> events = new ArrayDeque<>();
        private boolean closed;

        private Session(UUID device, String secret, long generation) {
            this.device = device;
            this.secret = secret;
            this.generation = generation;
        }

        public UUID device() {
            return device;
        }

        /** Every device in the caller's own network and dimension, with current reachability. */
        public List<Peer> peers() {
            valid(this);
            return registry.peers(device, secret).stream()
                    .limit(MAX_ENDPOINTS)
                    .map(
                            member ->
                                    new Peer(
                                            registry.address(member.id()).toString(),
                                            link.online(member.id()),
                                            member.id().equals(device)))
                    .toList();
        }

        /**
         * Bounded directory listing; only services covered by a current application scope appear.
         */
        public List<String> services(RadioAddress address) {
            var target = resolve(this, address, null, null);
            return endpoints.keySet().stream()
                    .filter(key -> key.device.equals(target.id()))
                    .filter(key -> endpoint(key.device, key.service) != null)
                    .filter(
                            key ->
                                    Arrays.stream(
                                                    new RadioRegistry.Permission[] {
                                                        RadioRegistry.Permission.MESSAGE_SEND,
                                                        RadioRegistry.Permission.TELEMETRY_READ,
                                                        RadioRegistry.Permission.COMMAND_SEND
                                                    })
                                            .anyMatch(
                                                    permission ->
                                                            registry.authorized(
                                                                    device,
                                                                    secret,
                                                                    permission,
                                                                    target.id(),
                                                                    key.service)))
                    .map(EndpointKey::service)
                    .sorted()
                    .toList();
        }

        public List<Event> poll() {
            valid(this);
            var result = List.copyOf(events);
            events.clear();
            return result;
        }

        public void message(
                RadioAddress target, String service, String channel, UUID request, byte[] payload) {
            var member = resolve(this, target, RadioRegistry.Permission.MESSAGE_SEND, service);
            var envelope =
                    envelope(
                            this,
                            member.id(),
                            service,
                            channel,
                            request,
                            RadioMessage.Type.MESSAGE,
                            payload);
            if (!link.send(
                    device,
                    member.id(),
                    UUID.randomUUID(),
                    RadioMessageCodec.encode(envelope),
                    tick,
                    () -> {
                        if (allowed(
                                this,
                                member.id(),
                                member.generation(),
                                RadioRegistry.Permission.MESSAGE_SEND,
                                service)) {
                            var endpoint = endpoint(member.id(), service);
                            if (endpoint != null) endpoint.message(envelope);
                        }
                    })) throw new IllegalStateException("Transport unavailable");
        }

        public Command command(
                RadioAddress target,
                String service,
                String channel,
                UUID request,
                byte[] payload,
                long deadline) {
            var member = resolve(this, target, RadioRegistry.Permission.COMMAND_SEND, service);
            var key = new RequestKey(device, Objects.requireNonNull(request));
            var previous = requests.get(key);
            if (previous != null) {
                if (!previous.target.equals(member.id())
                        || !previous.message.service().equals(service)
                        || !previous.message.channel().equals(channel)
                        || !Arrays.equals(previous.message.payload(), payload))
                    throw new IllegalArgumentException("Request ID reused with different command");
                return previous;
            }
            if (deadline <= tick || deadline - tick > 1200)
                throw new IllegalArgumentException("Invalid deadline");
            if (requests.size() >= MAX_REQUESTS
                    || requests.keySet().stream()
                                    .filter(other -> other.source().equals(device))
                                    .count()
                            >= MAX_REQUESTS_PER_DEVICE)
                throw new IllegalStateException("Request capacity");
            var message =
                    envelope(
                            this,
                            member.id(),
                            service,
                            channel,
                            request,
                            RadioMessage.Type.COMMAND,
                            payload);
            var command = new Command(this, member.id(), member.generation(), message, deadline);
            requests.put(key, command);
            command.emit(new byte[0]);
            if (!link.send(
                    device,
                    member.id(),
                    UUID.randomUUID(),
                    RadioMessageCodec.encode(message),
                    tick,
                    () -> {
                        if (!command.active()) return;
                        if (!allowed(
                                this,
                                member.id(),
                                member.generation(),
                                RadioRegistry.Permission.COMMAND_SEND,
                                service)) {
                            command.disconnect();
                            return;
                        }
                        var endpoint = endpoint(member.id(), service);
                        if (endpoint == null) command.disconnect();
                        else {
                            try {
                                endpoint.command(message, command);
                            } catch (RuntimeException exception) {
                                command.failed(new byte[0]);
                            }
                        }
                    })) command.disconnect();
            return command;
        }

        public UUID subscribe(
                RadioAddress target, String service, Set<String> fields, long staleTicks) {
            var member = resolve(this, target, RadioRegistry.Permission.TELEMETRY_READ, service);
            if (fields.isEmpty()
                    || fields.size() > MAX_FIELDS
                    || staleTicks < 1
                    || staleTicks > 1200) throw new IllegalArgumentException("Subscription limits");
            fields.forEach(RadioMessage::name);
            if (subscriptions.size() >= MAX_SUBSCRIPTIONS)
                throw new IllegalStateException("Subscription capacity");
            var id = UUID.randomUUID();
            var subscription =
                    new Subscription(
                            this,
                            member.id(),
                            member.generation(),
                            service,
                            Set.copyOf(fields),
                            id,
                            staleTicks);
            subscriptions.put(id, subscription);
            var current = samples.getOrDefault(new EndpointKey(member.id(), service), Map.of());
            current.entrySet().stream()
                    .filter(entry -> fields.contains(entry.getKey()))
                    .sorted(Comparator.comparingLong(entry -> entry.getValue().sequence))
                    .forEach(entry -> telemetry(subscription, entry.getKey(), entry.getValue()));
            return id;
        }

        public void unsubscribe(UUID id) {
            var subscription = subscriptions.get(id);
            if (subscription != null && subscription.session == this) removeSubscription(id);
        }

        public void ping(RadioAddress target, UUID request) {
            var member = resolve(this, target, null, null);
            var key = new RequestKey(device, Objects.requireNonNull(request));
            if (pings.containsKey(key)) throw new IllegalArgumentException("Duplicate ping");
            if (pings.size() >= MAX_REQUESTS) throw new IllegalStateException("Ping capacity");
            var ping =
                    new Ping(this, member.id(), member.generation(), nanos.getAsLong(), tick + 200);
            pings.put(key, ping);
            var frame =
                    envelope(
                            this,
                            member.id(),
                            "radio",
                            "ping",
                            request,
                            RadioMessage.Type.PING,
                            new byte[0]);
            if (!link.send(
                    device,
                    member.id(),
                    UUID.randomUUID(),
                    RadioMessageCodec.encode(frame),
                    tick,
                    () -> {
                        if (pings.get(key) != ping
                                || !allowed(this, member.id(), member.generation(), null, null))
                            return;
                        var reply =
                                new RadioMessage(
                                        1,
                                        member.id(),
                                        frame.destinationAddress(),
                                        device,
                                        frame.sourceAddress(),
                                        "radio",
                                        "ping",
                                        request,
                                        RadioMessage.Type.PONG,
                                        new byte[0]);
                        link.send(
                                member.id(),
                                device,
                                UUID.randomUUID(),
                                RadioMessageCodec.encode(reply),
                                tick,
                                () -> {
                                    if (pings.get(key) == ping
                                            && allowed(
                                                    this,
                                                    member.id(),
                                                    member.generation(),
                                                    null,
                                                    null)) {
                                        pings.remove(key);
                                        offer(
                                                this,
                                                new Event(
                                                        request,
                                                        State.COMPLETED,
                                                        reply,
                                                        ++sequence,
                                                        Freshness.ONLINE,
                                                        tick,
                                                        Math.max(
                                                                0,
                                                                nanos.getAsLong() - ping.started)));
                                    }
                                });
                    })) {
                pings.remove(key);
                throw new IllegalStateException("Transport unavailable");
            }
        }

        @Override
        public void close() {
            closed = true;
            pings.values().removeIf(value -> value.session == this);
            List.copyOf(subscriptions.values()).stream()
                    .filter(value -> value.session == this)
                    .forEach(value -> removeSubscription(value.id));
            requests.values().stream()
                    .filter(value -> value.session == this)
                    .forEach(Command::disconnect);
            events.clear();
        }
    }

    public final class Command {
        private final Session session;
        private final UUID target;
        private final long generation;
        private final RadioMessage message;
        private final long deadline;
        private State state = State.ACCEPTED;
        private long finishedAt = -1;

        private Command(
                Session session,
                UUID target,
                long generation,
                RadioMessage message,
                long deadline) {
            this.session = session;
            this.target = target;
            this.generation = generation;
            this.message = message;
            this.deadline = deadline;
        }

        public State state() {
            return state;
        }

        public void running() {
            if (active() && state == State.ACCEPTED) {
                state = State.RUNNING;
                emit(new byte[0]);
            }
        }

        public void completed(byte[] result) {
            finish(State.COMPLETED, result);
        }

        public void failed(byte[] result) {
            finish(State.FAILED, result);
        }

        private boolean active() {
            return state == State.ACCEPTED || state == State.RUNNING;
        }

        private void disconnect() {
            finish(State.DISCONNECTED, new byte[0]);
        }

        private void finish(State next, byte[] result) {
            bounded(result);
            if (!active()) return;
            if (tick >= deadline) next = State.TIMED_OUT;
            else if (!allowed(
                    session,
                    target,
                    generation,
                    RadioRegistry.Permission.COMMAND_SEND,
                    message.service())) next = State.DISCONNECTED;
            state = next;
            if (!active()) finishedAt = tick;
            emit(result);
        }

        private void emit(byte[] result) {
            var reply =
                    new RadioMessage(
                            1,
                            target,
                            message.destinationAddress(),
                            session.device,
                            message.sourceAddress(),
                            message.service(),
                            message.channel(),
                            message.requestId(),
                            RadioMessage.Type.RESULT,
                            result);
            var event =
                    new Event(
                            message.requestId(),
                            state,
                            reply,
                            ++sequence,
                            Freshness.ONLINE,
                            tick,
                            -1);
            if (state == State.ACCEPTED || state == State.TIMED_OUT || state == State.DISCONNECTED)
                offer(session, event);
            else
                link.send(
                        target,
                        session.device,
                        UUID.randomUUID(),
                        RadioMessageCodec.encode(reply),
                        tick,
                        () -> {
                            if (allowed(
                                    session,
                                    target,
                                    generation,
                                    RadioRegistry.Permission.COMMAND_SEND,
                                    message.service())) offer(session, event);
                        });
        }
    }

    public void tick(long now) {
        if (now < tick) throw new IllegalArgumentException("Ticks must be monotonic");
        tick = now;
        for (var command : requests.values())
            if (command.active()) {
                if (tick >= command.deadline) command.finish(State.TIMED_OUT, new byte[0]);
                else if (!allowed(
                        command.session,
                        command.target,
                        command.generation,
                        RadioRegistry.Permission.COMMAND_SEND,
                        command.message.service())) command.disconnect();
            }
        requests.values()
                .removeIf(
                        command ->
                                !command.active()
                                        && tick - command.finishedAt > FINISHED_RETENTION_TICKS);
        for (var entry : List.copyOf(pings.entrySet())) {
            var ping = entry.getValue();
            if (tick >= ping.deadline
                    || !allowed(ping.session, ping.target, ping.generation, null, null)) {
                pings.remove(entry.getKey());
                offer(
                        ping.session,
                        new Event(
                                entry.getKey().request,
                                tick >= ping.deadline ? State.TIMED_OUT : State.DISCONNECTED,
                                null,
                                ++sequence,
                                Freshness.OFFLINE,
                                tick,
                                -1));
            }
        }
        // Retain completed request IDs for the lifetime of this broker; reaching capacity applies
        // backpressure.
        for (var subscription : List.copyOf(subscriptions.values())) {
            if (endpoint(subscription.target, subscription.service) == null
                    || !allowed(
                            subscription.session,
                            subscription.target,
                            subscription.generation,
                            RadioRegistry.Permission.TELEMETRY_READ,
                            subscription.service)) {
                offer(
                        subscription.session,
                        new Event(
                                subscription.id,
                                State.DISCONNECTED,
                                null,
                                ++sequence,
                                Freshness.OFFLINE,
                                tick,
                                -1));
                removeSubscription(subscription.id);
            } else {
                for (var entry :
                        samples.getOrDefault(
                                        new EndpointKey(subscription.target, subscription.service),
                                        Map.of())
                                .entrySet()) {
                    if (!subscription.fields.contains(entry.getKey())) continue;
                    var sample = entry.getValue();
                    String key = subscription.id + ":" + entry.getKey();
                    if (tick - sample.tick >= subscription.staleTicks
                            && freshness.put(key, Freshness.STALE) != Freshness.STALE)
                        offer(
                                subscription.session,
                                new Event(
                                        subscription.id,
                                        State.COMPLETED,
                                        new RadioMessage(
                                                1,
                                                subscription.target,
                                                registry.address(subscription.target),
                                                subscription.session.device,
                                                registry.address(subscription.session.device),
                                                subscription.service,
                                                entry.getKey(),
                                                subscription.id,
                                                RadioMessage.Type.TELEMETRY,
                                                sample.bytes),
                                        ++sequence,
                                        Freshness.STALE,
                                        sample.tick,
                                        -1));
                }
            }
        }
    }

    private void telemetry(Subscription subscription, String field, Sample sample) {
        if (endpoint(subscription.target, subscription.service) == null
                || !allowed(
                        subscription.session,
                        subscription.target,
                        subscription.generation,
                        RadioRegistry.Permission.TELEMETRY_READ,
                        subscription.service)) return;
        freshness.put(subscription.id + ":" + field, Freshness.ONLINE);
        var message =
                envelope(
                        subscription.session,
                        subscription.target,
                        subscription.service,
                        field,
                        subscription.id,
                        RadioMessage.Type.TELEMETRY,
                        sample.bytes);
        var outbound =
                new RadioMessage(
                        1,
                        subscription.target,
                        message.destinationAddress(),
                        subscription.session.device,
                        message.sourceAddress(),
                        subscription.service,
                        field,
                        subscription.id,
                        RadioMessage.Type.TELEMETRY,
                        sample.bytes);
        link.send(
                subscription.target,
                subscription.session.device,
                UUID.randomUUID(),
                RadioMessageCodec.encode(outbound),
                tick,
                () -> {
                    if (subscriptions.get(subscription.id) != subscription
                            || endpoint(subscription.target, subscription.service) == null
                            || !allowed(
                                    subscription.session,
                                    subscription.target,
                                    subscription.generation,
                                    RadioRegistry.Permission.TELEMETRY_READ,
                                    subscription.service)) return;
                    long delivered = deliveredSequences.getOrDefault(subscription.id, -1L);
                    if (sample.sequence <= delivered) return;
                    deliveredSequences.put(subscription.id, sample.sequence);
                    offer(
                            subscription.session,
                            new Event(
                                    subscription.id,
                                    State.COMPLETED,
                                    outbound,
                                    sample.sequence,
                                    tick - sample.tick >= subscription.staleTicks
                                            ? Freshness.STALE
                                            : Freshness.ONLINE,
                                    sample.tick,
                                    -1));
                });
    }

    private RadioMessage envelope(
            Session session,
            UUID target,
            String service,
            String channel,
            UUID request,
            RadioMessage.Type type,
            byte[] payload) {
        return new RadioMessage(
                1,
                session.device,
                registry.address(session.device),
                target,
                registry.address(target),
                service,
                channel,
                request,
                type,
                payload);
    }

    private RadioRegistry.Member resolve(
            Session session,
            RadioAddress address,
            RadioRegistry.Permission permission,
            String service) {
        valid(session);
        var target =
                registry.resolve(session.device, session.secret, Objects.requireNonNull(address))
                        .orElseThrow(() -> new SecurityException("Address unavailable"));
        if (!allowed(session, target.id(), target.generation(), permission, service))
            throw new SecurityException("Permission or connectivity denied");
        return target;
    }

    private void valid(Session session) {
        if (session.closed
                || registry.authenticate(session.device, session.secret)
                        .filter(member -> member.generation() == session.generation)
                        .isEmpty()) throw new SecurityException("Session invalid");
    }

    private boolean allowed(
            Session session,
            UUID target,
            long generation,
            RadioRegistry.Permission permission,
            String service) {
        if (session.closed || !link.online(session.device) || !link.online(target)) return false;
        var source = registry.authenticate(session.device, session.secret);
        var destination = registry.member(target);
        return source.isPresent()
                && destination.isPresent()
                && source.get().generation() == session.generation
                && destination.get().generation() == generation
                && source.get().network().equals(destination.get().network())
                && source.get().dimension().equals(destination.get().dimension())
                && (permission == null
                        || registry.authorized(
                                session.device, session.secret, permission, target, service));
    }

    private void offer(Session session, Event event) {
        if (session.closed) return;
        if (session.events.size() >= MAX_EVENTS) {
            session.close();
            return;
        }
        session.events.add(event);
    }

    private Endpoint endpoint(UUID device, String service) {
        var key = new EndpointKey(device, service);
        return registry.member(device)
                        .filter(
                                member ->
                                        Objects.equals(
                                                endpointGenerations.get(key), member.generation()))
                        .isPresent()
                ? endpoints.get(key)
                : null;
    }

    private void removeSubscription(UUID id) {
        subscriptions.remove(id);
        deliveredSequences.remove(id);
        freshness.keySet().removeIf(key -> key.startsWith(id.toString() + ":"));
    }

    private static void bounded(byte[] bytes) {
        if (bytes == null || bytes.length > RadioMessage.MAX_PAYLOAD)
            throw new IllegalArgumentException("Payload limit");
    }
}
