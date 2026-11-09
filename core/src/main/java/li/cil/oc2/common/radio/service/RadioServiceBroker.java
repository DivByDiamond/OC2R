package li.cil.oc2.common.radio.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.*;
import java.util.function.LongSupplier;
import li.cil.oc2.common.radio.RadioAddress;
import li.cil.oc2.common.radio.RadioDuplicateFilter;
import li.cil.oc2.common.radio.RadioRegistry;

/** Server-thread application broker. Link implementations must schedule both directions. */
// Server thread only; sessions, pings and subscriptions are matched by identity on purpose.
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.CompareObjectsWithEquals"})
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

    /**
     * One outcome delivered to a session. roundTripNanos is a measured monotonic-clock round trip of a
     * ping and is -1 for everything else. route is the hop count and nominal one-way estimate that
     * applied when a ping completed; it is null for other events and is never a measurement.
     */
    public record Event(
            UUID requestId,
            State state,
            RadioMessage message,
            long sequence,
            Freshness freshness,
            long sampleTick,
            long roundTripNanos,
            Estimate route) {
        public Event(UUID requestId, State state, RadioMessage message, long sequence,
                     Freshness freshness, long sampleTick, long roundTripNanos) {
            this(requestId, state, message, sequence, freshness, sampleTick, roundTripNanos, null);
        }
    }

    /** Milliseconds one server tick lasts at the nominal 20 TPS; real ticks are longer when the server lags. */
    public static final long NOMINAL_TICK_MILLIS = 50;

    /**
     * Route estimate, not a measurement: radio legs (hops), the scheduled one-way delivery delay in
     * ticks and that delay in milliseconds at the nominal tick rate. hops is -1 when there is no route.
     */
    public record Estimate(int hops, long nominalTicks, long nominalMillis) {
        public Estimate(int hops, long nominalTicks) {
            this(hops, nominalTicks, nominalTicks < 0 ? -1 : nominalTicks * NOMINAL_TICK_MILLIS);
        }
    }

    /**
     * Age and state of one telemetry field of a subscription. ageTicks counts server ticks since the
     * sample was published and ageMillis converts it at the nominal tick rate. state is OFFLINE when
     * the publisher or the link is gone, STALE once the age reaches the subscription's stale limit,
     * otherwise ONLINE.
     */
    public record FieldStatus(
            String field, Freshness state, long sampleTick, long ageTicks, long ageMillis, long sequence) {}

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
    private final Map<EndpointKey, Long> endpointRegistrations = new HashMap<>();
    private long registrationSequence;
    private final Map<EndpointKey, Endpoint> endpoints = new HashMap<>();
    private final Map<EndpointKey, Map<String, Sample>> samples = new HashMap<>();
    private final Map<RequestKey, Command> requests = new LinkedHashMap<>();
    private final Map<RequestKey, Ping> pings = new HashMap<>();
    private final Map<String, Freshness> freshness = new HashMap<>();
    private final Map<UUID, Long> deliveredSequences = new HashMap<>();
    private final RadioDuplicateFilter<RequestKey> seenMessages =
            new RadioDuplicateFilter<>(MAX_REQUESTS, FINISHED_RETENTION_TICKS);
    private final Map<UUID, Subscription> subscriptions = new LinkedHashMap<>();
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // tick(long) advances this clock.
    private long tick;
    private long sequence;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The broker works on the live registry.")
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
        final long generation = registry.member(device).orElseThrow().generation();
        // Identity of the live handler distinguishes reconnects from repeated registration.
        if (endpoints.get(key) != endpoint) { // NOPMD
            endpointRegistrations.put(key, ++registrationSequence);
        }
        endpoints.put(key, Objects.requireNonNull(endpoint));
        endpointGenerations.put(key, generation);
    }

    public void unregister(UUID device, String service) {
        var key = new EndpointKey(device, service);
        endpoints.remove(key);
        endpointGenerations.remove(key);
        endpointRegistrations.remove(key);
        samples.remove(key);
        requests.values().stream()
                .filter(command -> command.target.equals(device) && command.message.service().equals(service))
                .forEach(Command::disconnect);
        List.copyOf(subscriptions.values()).stream()
                .filter(subscription -> subscription.target.equals(device) && subscription.service.equals(service))
                .forEach(subscription -> removeSubscription(subscription.id));
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
        @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // Backs device().
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
            // A retry with the same request ID, for example after a reconnect, is delivered only once.
            var messageKey = new RequestKey(device, request);
            final var destination = new EndpointKey(member.id(), service);
            final Long registration = endpointRegistrations.get(destination);
            if (seenMessages.contains(messageKey, tick)) return;
            if (!link.send(
                    device,
                    member.id(),
                    UUID.randomUUID(),
                    RadioMessageCodec.encode(envelope),
                    tick,
                    () -> {
                        if (!allowed(
                                this,
                                member.id(),
                                member.generation(),
                                RadioRegistry.Permission.MESSAGE_SEND,
                                service)) return;
                        if (!Objects.equals(registration, endpointRegistrations.get(destination))) return;
                        var endpoint = endpoint(member.id(), service);
                        if (endpoint != null) endpoint.message(envelope);
                    })) throw new IllegalStateException("Transport unavailable");
            seenMessages.firstSeen(messageKey, tick);
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
                if (previous.session != this) previous.adopt(this);
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

        /**
         * Age and online/stale/offline state of every field the subscription has a sample for, ordered
         * by field name. A field with no sample yet is absent, not reported as live.
         */
        public List<FieldStatus> telemetryStatus(UUID id) {
            valid(this);
            var subscription = subscriptions.get(id);
            if (subscription == null || subscription.session != this)
                throw new IllegalArgumentException("Unknown subscription");
            boolean reachable =
                    endpoint(subscription.target, subscription.service) != null
                            && allowed(
                                    this,
                                    subscription.target,
                                    subscription.generation,
                                    RadioRegistry.Permission.TELEMETRY_READ,
                                    subscription.service);
            return samples.getOrDefault(new EndpointKey(subscription.target, subscription.service), Map.of())
                    .entrySet()
                    .stream()
                    .filter(entry -> subscription.fields.contains(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(
                            entry -> {
                                long age = Math.max(0, tick - entry.getValue().tick);
                                return new FieldStatus(
                                        entry.getKey(),
                                        !reachable
                                                ? Freshness.OFFLINE
                                                : age >= subscription.staleTicks
                                                        ? Freshness.STALE
                                                        : Freshness.ONLINE,
                                        entry.getValue().tick,
                                        age,
                                        age * NOMINAL_TICK_MILLIS,
                                        entry.getValue().sequence);
                            })
                    .toList();
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
                                                                nanos.getAsLong() - ping.started),
                                                        link.estimate(device, member.id())));
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
        private Session session;
        private byte[] lastResult = new byte[0];
        private final UUID target;
        private final long generation;
        private final RadioMessage message;
        private final long deadline;
        @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // Backs state().
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

        /**
         * A retry with a known request ID after a reconnect: later results go to the new session and
         * the state reached so far is reported again. The command is never executed a second time.
         */
        private void adopt(Session next) {
            session = next;
            offer(
                    next,
                    new Event(
                            message.requestId(),
                            state,
                            new RadioMessage(
                                    1,
                                    target,
                                    message.destinationAddress(),
                                    next.device,
                                    message.sourceAddress(),
                                    message.service(),
                                    message.channel(),
                                    message.requestId(),
                                    RadioMessage.Type.RESULT,
                                    lastResult),
                            ++sequence,
                            Freshness.ONLINE,
                            tick,
                            -1));
        }

        private void disconnect() {
            finish(State.DISCONNECTED, new byte[0]);
        }

        private void finish(State next, byte[] result) {
            bounded(result);
            if (!active()) return;
            State outcome = next;
            if (tick >= deadline) outcome = State.TIMED_OUT;
            else if (!allowed(
                    session,
                    target,
                    generation,
                    RadioRegistry.Permission.COMMAND_SEND,
                    message.service())) outcome = State.DISCONNECTED;
            state = outcome;
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
            lastResult = result.clone();
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

    // One pass over every pending request kind; events are fresh immutable records per entry.
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.AvoidInstantiatingObjectsInLoops"})
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
