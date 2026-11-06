package li.cil.oc2.common.radio.service;

import li.cil.oc2.common.radio.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

final class RadioServiceBrokerTest {
    private static final String STATUS = "status";
    private static final String ENERGY = "energy";
    final RadioRegistry registry = new RadioRegistry();
    final RadioRegistry.CreatedNetwork network = registry.createNetwork();
    final UUID source = UUID.randomUUID();
    final UUID target = UUID.randomUUID();
    final AtomicLong nanos = new AtomicLong();
    final Queue<Runnable> deliveries = new ArrayDeque<>();
    final Set<UUID> offline = new HashSet<>();
    final RadioServiceBroker broker = new RadioServiceBroker(registry, new RadioServiceBroker.Link() {
        @Override
        public boolean online(UUID id) { return !offline.contains(id); }
        @Override
        public boolean send(UUID from, UUID to, UUID packet, byte[] bytes, long tick, Runnable delivery) {
            deliveries.add(delivery); return online(from) && online(to);
        }
        @Override
        public RadioServiceBroker.Estimate estimate(UUID from, UUID to) { return new RadioServiceBroker.Estimate(2, 2); }
        @Override
        public String security() { return "TEST_PLAINTEXT"; }
    }, nanos::get);
    RadioRegistry.Enrollment enroll(UUID id, RadioRegistry.Permission... permissions) {
        var scopes = new HashSet<RadioRegistry.Scope>();
        for (var permission : permissions) scopes.add(new RadioRegistry.Scope(permission, target, STATUS));
        var invite = registry.invite(network.network(), network.adminSecret(), "overworld", id, scopes);
        return registry.enroll(invite.invite(), invite.secret());
    }
    RadioServiceBroker.Session session(RadioRegistry.Permission... permissions) {
        var enrollment = enroll(source, permissions); enroll(target);
        return broker.connect(source, enrollment.secret());
    }
    @Test void peersListOwnNetworkWithReachability() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        var foreign = registry.createNetwork();
        var invite = registry.invite(foreign.network(), foreign.adminSecret(), "overworld", UUID.randomUUID(), Set.of());
        registry.enroll(invite.invite(), invite.secret());
        offline.add(target);
        var peers = session.peers();
        assertEquals(2, peers.size());
        assertEquals(1, peers.stream().filter(RadioServiceBroker.Peer::self).count());
        assertEquals(1, peers.stream().filter(peer -> !peer.online()).count());
    }
    @Test void finishedCommandsExpireAndOneDeviceCannotFillTheBroker() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND);
        var first = session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 10);
        first.failed(new byte[0]);
        for (int i = 1; i < RadioServiceBroker.MAX_REQUESTS_PER_DEVICE; i++)
        { session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 1200); session.poll(); } // NOPMD - fills the broker
        assertThrows(IllegalStateException.class, () -> session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 1200));
        broker.tick(RadioServiceBroker.FINISHED_RETENTION_TICKS + 5);
        assertDoesNotThrow(() -> session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], RadioServiceBroker.FINISHED_RETENTION_TICKS + 100));
    }
    @Test void scopesAndEnvelopeCannotBeForged() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        assertThrows(SecurityException.class, () -> session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 10));
        assertThrows(SecurityException.class, () -> session.message(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new RadioMessage(2, source, registry.address(source), target, registry.address(target), STATUS, "move", UUID.randomUUID(), RadioMessage.Type.COMMAND, new byte[0]));
    }
    @Test void commandsRequireActualCompletionAndDeduplicate() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND); var count = new AtomicLong();
        final RadioServiceBroker.Command[] pending = new RadioServiceBroker.Command[1];
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {
            @Override
            public void command(RadioMessage message, RadioServiceBroker.Command command) { count.incrementAndGet(); pending[0] = command; command.running(); }
        });
        var id = UUID.randomUUID(); var command = session.command(registry.address(target), STATUS, "move", id, new byte[]{1}, 10);
        assertEquals(RadioServiceBroker.State.ACCEPTED, command.state()); deliveries.remove().run();
        assertEquals(RadioServiceBroker.State.RUNNING, command.state());
        assertSame(command, session.command(registry.address(target), STATUS, "move", id, new byte[]{1}, 10));
        pending[0].completed(new byte[]{2}); assertEquals(1, count.get());
        assertEquals(RadioServiceBroker.State.COMPLETED, command.state());
        while (!deliveries.isEmpty()) deliveries.remove().run();
        assertEquals(3, session.poll().size());
    }
    @Test void deadlineAndDisconnectPreventLateCompletion() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND);
        var command = session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 3);
        broker.tick(3); deliveries.remove().run(); command.completed(new byte[0]);
        assertEquals(RadioServiceBroker.State.TIMED_OUT, command.state());
        var next = session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 10);
        offline.add(target); broker.tick(4); assertEquals(RadioServiceBroker.State.DISCONNECTED, next.state());
    }
    @Test void telemetrySnapshotsRevokeAndUnsubscribe() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {});
        broker.publish(target, STATUS, ENERGY, new byte[]{1});
        var subscription = session.subscribe(registry.address(target), STATUS, Set.of(ENERGY), 3);
        deliveries.remove().run(); assertArrayEquals(new byte[]{1}, session.poll().getFirst().message().payload());
        broker.publish(target, STATUS, ENERGY, new byte[]{2}); session.unsubscribe(subscription); deliveries.remove().run(); assertTrue(session.poll().isEmpty());
        session.subscribe(registry.address(target), STATUS, Set.of(ENERGY), 3);
        registry.revoke(network.network(), network.adminSecret(), target); deliveries.remove().run(); broker.tick(1);
        assertEquals(RadioServiceBroker.Freshness.OFFLINE, session.poll().getFirst().freshness());
    }
    @Test void pingMeasuresBothScheduledLegs() {
        var session = session(); var request = UUID.randomUUID(); session.ping(registry.address(target), request);
        nanos.set(100_000_000); deliveries.remove().run(); assertTrue(session.poll().isEmpty());
        nanos.set(350_000_000); deliveries.remove().run(); assertEquals(350_000_000, session.poll().getFirst().roundTripNanos());
    }
    @Test void codecBoundsAndRoundTrip() {
        session(); var message = new RadioMessage(1, source, registry.address(source), target, registry.address(target), STATUS, ENERGY, UUID.randomUUID(), RadioMessage.Type.MESSAGE, new byte[]{3});
        assertArrayEquals(message.payload(), RadioMessageCodec.decode(RadioMessageCodec.encode(message)).payload());
        assertThrows(IllegalArgumentException.class, () -> RadioMessageCodec.decode(new byte[RadioMessageCodec.MAX_FRAME + 1]));
    }
    @Test void staleSamplesAndDirectoryAreScoped() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {});
        broker.register(target, "private", new RadioServiceBroker.Endpoint() {});
        assertEquals(List.of(STATUS), session.services(registry.address(target)));
        broker.publish(target, STATUS, ENERGY, new byte[]{1});
        session.subscribe(registry.address(target), STATUS, Set.of(ENERGY), 2);
        deliveries.remove().run(); session.poll(); broker.tick(2);
        var stale = session.poll().getFirst();
        assertEquals(RadioServiceBroker.Freshness.STALE, stale.freshness());
        assertEquals(ENERGY, stale.message().channel());
        broker.tick(3); assertTrue(session.poll().isEmpty());
    }
    @Test void queuedCommandsRecheckPermissionAndPayloadBounds() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND);
        var calls = new AtomicLong();
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {
            @Override
            public void command(RadioMessage message, RadioServiceBroker.Command command) { calls.incrementAndGet(); }
        });
        assertThrows(IllegalArgumentException.class, () -> session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[RadioMessage.MAX_PAYLOAD + 1], 10));
        var command = session.command(registry.address(target), STATUS, "move", UUID.randomUUID(), new byte[0], 10);
        registry.setScopes(network.network(), network.adminSecret(), source, Set.of());
        deliveries.remove().run(); assertEquals(0, calls.get());
        assertEquals(RadioServiceBroker.State.DISCONNECTED, command.state());
    }

    @Test void commandRetryAfterReconnectKeepsOneExecutionAndReportsTheKnownState() {
        var enrollment = enroll(source, RadioRegistry.Permission.COMMAND_SEND); enroll(target);
        var first = broker.connect(source, enrollment.secret()); var count = new AtomicLong();
        final RadioServiceBroker.Command[] pending = new RadioServiceBroker.Command[1];
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {
            @Override
            public void command(RadioMessage message, RadioServiceBroker.Command command) { count.incrementAndGet(); pending[0] = command; command.running(); }
        });
        var id = UUID.randomUUID(); var command = first.command(registry.address(target), STATUS, "move", id, new byte[]{1}, 100);
        while (!deliveries.isEmpty()) deliveries.remove().run();
        var second = broker.connect(source, enrollment.secret());
        assertSame(command, second.command(registry.address(target), STATUS, "move", id, new byte[]{1}, 100));
        assertTrue(deliveries.isEmpty(), "a retry sends nothing new");
        assertEquals(RadioServiceBroker.State.RUNNING, second.poll().getFirst().state());
        pending[0].completed(new byte[]{7}); while (!deliveries.isEmpty()) deliveries.remove().run();
        assertEquals(RadioServiceBroker.State.COMPLETED, second.poll().getLast().state());
        assertEquals(1, count.get());
        var third = broker.connect(source, enrollment.secret());
        assertSame(command, third.command(registry.address(target), STATUS, "move", id, new byte[]{1}, 100));
        assertArrayEquals(new byte[]{7}, third.poll().getFirst().message().payload());
        assertEquals(1, count.get(), "the finished command is not executed again");
        assertThrows(IllegalArgumentException.class, () -> second.command(registry.address(target), STATUS, "move", id, new byte[]{2}, 100));
    }
    @Test void aClosedSessionsCommandIsNotReplayedByTheRetry() {
        var enrollment = enroll(source, RadioRegistry.Permission.COMMAND_SEND); enroll(target); var count = new AtomicLong();
        var first = broker.connect(source, enrollment.secret());
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {
            @Override
            public void command(RadioMessage message, RadioServiceBroker.Command command) { count.incrementAndGet(); }
        });
        var id = UUID.randomUUID(); first.command(registry.address(target), STATUS, "move", id, new byte[0], 100);
        first.close(); var second = broker.connect(source, enrollment.secret());
        var again = second.command(registry.address(target), STATUS, "move", id, new byte[0], 100);
        assertEquals(RadioServiceBroker.State.DISCONNECTED, again.state());
        while (!deliveries.isEmpty()) deliveries.remove().run();
        assertEquals(0, count.get(), "a command disconnected before delivery never executes");
        assertEquals(RadioServiceBroker.State.DISCONNECTED, second.poll().getFirst().state());
    }
    @Test void aRetriedMessageIsDeliveredOnce() {
        var session = session(RadioRegistry.Permission.MESSAGE_SEND); var count = new AtomicLong();
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {
            @Override
            public void message(RadioMessage message) { count.incrementAndGet(); }
        });
        var id = UUID.randomUUID();
        session.message(registry.address(target), STATUS, "note", id, new byte[]{1});
        session.message(registry.address(target), STATUS, "note", id, new byte[]{1});
        session.message(registry.address(target), STATUS, "note", UUID.randomUUID(), new byte[]{1});
        while (!deliveries.isEmpty()) deliveries.remove().run();
        assertEquals(2, count.get());
        broker.tick(RadioServiceBroker.FINISHED_RETENTION_TICKS + 1);
        session.message(registry.address(target), STATUS, "note", id, new byte[]{1});
        while (!deliveries.isEmpty()) deliveries.remove().run();
        assertEquals(3, count.get(), "the request ID is forgotten after the retention window");
    }
    @Test void pingReportsRttHopsAndNominalDelayAsSeparateValues() {
        var session = session(); var request = UUID.randomUUID(); session.ping(registry.address(target), request);
        nanos.set(10); deliveries.remove().run(); nanos.set(7_000_010); deliveries.remove().run();
        var result = session.poll().getFirst();
        assertEquals(7_000_010, result.roundTripNanos(), "measured with the monotonic clock supplier");
        assertEquals(2, result.route().hops());
        assertEquals(2, result.route().nominalTicks());
        assertEquals(2 * RadioServiceBroker.NOMINAL_TICK_MILLIS, result.route().nominalMillis());
        assertNotEquals(result.roundTripNanos() / 1_000_000, result.route().nominalMillis(), "estimate and measurement differ");
        assertEquals(new RadioServiceBroker.Estimate(-1, -1).nominalMillis(), -1);
    }
    @Test void telemetryAgeAndOnlineStaleOfflineStateAreQueryable() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        broker.register(target, STATUS, new RadioServiceBroker.Endpoint() {});
        var subscription = session.subscribe(registry.address(target), STATUS, Set.of(ENERGY, "heat"), 10);
        assertTrue(session.telemetryStatus(subscription).isEmpty(), "no sample is not reported as live");
        broker.tick(5); broker.publish(target, STATUS, ENERGY, new byte[]{1});
        broker.tick(8); broker.publish(target, STATUS, "heat", new byte[]{1});
        broker.tick(14);
        var status = session.telemetryStatus(subscription);
        assertEquals(List.of("energy", "heat"), status.stream().map(RadioServiceBroker.FieldStatus::field).toList());
        assertEquals(RadioServiceBroker.Freshness.ONLINE, status.get(0).state());
        assertEquals(9, status.get(0).ageTicks()); assertEquals(450, status.get(0).ageMillis());
        broker.tick(15);
        assertEquals(RadioServiceBroker.Freshness.STALE, session.telemetryStatus(subscription).get(0).state());
        assertEquals(RadioServiceBroker.Freshness.ONLINE, session.telemetryStatus(subscription).get(1).state());
        offline.add(target);
        assertTrue(session.telemetryStatus(subscription).stream().allMatch(field -> field.state() == RadioServiceBroker.Freshness.OFFLINE));
        assertThrows(IllegalArgumentException.class, () -> session.telemetryStatus(UUID.randomUUID()));
    }

}
