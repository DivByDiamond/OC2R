package li.cil.oc2.common.radio.service;

import li.cil.oc2.common.radio.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class RadioServiceBrokerTest {
    final RadioRegistry registry = new RadioRegistry();
    final RadioRegistry.CreatedNetwork network = registry.createNetwork();
    final UUID source = UUID.randomUUID(), target = UUID.randomUUID();
    final AtomicLong nanos = new AtomicLong();
    final Queue<Runnable> deliveries = new ArrayDeque<>();
    final Set<UUID> offline = new HashSet<>();
    final RadioServiceBroker broker = new RadioServiceBroker(registry, new RadioServiceBroker.Link() {
        public boolean online(UUID id) { return !offline.contains(id); }
        public boolean send(UUID from, UUID to, UUID packet, byte[] bytes, long tick, Runnable delivery) {
            deliveries.add(delivery); return online(from) && online(to);
        }
        public RadioServiceBroker.Estimate estimate(UUID from, UUID to) { return new RadioServiceBroker.Estimate(2, 2); }
        public String security() { return "TEST_PLAINTEXT"; }
    }, nanos::get);
    RadioRegistry.Enrollment enroll(UUID id, RadioRegistry.Permission... permissions) {
        var scopes = new HashSet<RadioRegistry.Scope>();
        for (var permission : permissions) scopes.add(new RadioRegistry.Scope(permission, target, "status"));
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
        var first = session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 10);
        first.failed(new byte[0]);
        for (int i = 1; i < RadioServiceBroker.MAX_REQUESTS_PER_DEVICE; i++)
        { session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 1200); session.poll(); }
        assertThrows(IllegalStateException.class, () -> session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 1200));
        broker.tick(RadioServiceBroker.FINISHED_RETENTION_TICKS + 5);
        assertDoesNotThrow(() -> session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], RadioServiceBroker.FINISHED_RETENTION_TICKS + 100));
    }
    @Test void scopesAndEnvelopeCannotBeForged() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        assertThrows(SecurityException.class, () -> session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 10));
        assertThrows(SecurityException.class, () -> session.message(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new RadioMessage(2, source, registry.address(source), target, registry.address(target), "status", "move", UUID.randomUUID(), RadioMessage.Type.COMMAND, new byte[0]));
    }
    @Test void commandsRequireActualCompletionAndDeduplicate() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND); var count = new AtomicLong();
        final RadioServiceBroker.Command[] pending = new RadioServiceBroker.Command[1];
        broker.register(target, "status", new RadioServiceBroker.Endpoint() {
            public void command(RadioMessage message, RadioServiceBroker.Command command) { count.incrementAndGet(); pending[0] = command; command.running(); }
        });
        var id = UUID.randomUUID(); var command = session.command(registry.address(target), "status", "move", id, new byte[]{1}, 10);
        assertEquals(RadioServiceBroker.State.ACCEPTED, command.state()); deliveries.remove().run();
        assertEquals(RadioServiceBroker.State.RUNNING, command.state());
        assertSame(command, session.command(registry.address(target), "status", "move", id, new byte[]{1}, 10));
        pending[0].completed(new byte[]{2}); assertEquals(1, count.get());
        assertEquals(RadioServiceBroker.State.COMPLETED, command.state());
        while (!deliveries.isEmpty()) deliveries.remove().run();
        assertEquals(3, session.poll().size());
    }
    @Test void deadlineAndDisconnectPreventLateCompletion() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND);
        var command = session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 3);
        broker.tick(3); deliveries.remove().run(); command.completed(new byte[0]);
        assertEquals(RadioServiceBroker.State.TIMED_OUT, command.state());
        var next = session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 10);
        offline.add(target); broker.tick(4); assertEquals(RadioServiceBroker.State.DISCONNECTED, next.state());
    }
    @Test void telemetrySnapshotsRevokeAndUnsubscribe() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        broker.register(target, "status", new RadioServiceBroker.Endpoint() {});
        broker.publish(target, "status", "energy", new byte[]{1});
        var subscription = session.subscribe(registry.address(target), "status", Set.of("energy"), 3);
        deliveries.remove().run(); assertArrayEquals(new byte[]{1}, session.poll().getFirst().message().payload());
        broker.publish(target, "status", "energy", new byte[]{2}); session.unsubscribe(subscription); deliveries.remove().run(); assertTrue(session.poll().isEmpty());
        session.subscribe(registry.address(target), "status", Set.of("energy"), 3);
        registry.revoke(network.network(), network.adminSecret(), target); deliveries.remove().run(); broker.tick(1);
        assertEquals(RadioServiceBroker.Freshness.OFFLINE, session.poll().getFirst().freshness());
    }
    @Test void pingMeasuresBothScheduledLegs() {
        var session = session(); var request = UUID.randomUUID(); session.ping(registry.address(target), request);
        nanos.set(100_000_000); deliveries.remove().run(); assertTrue(session.poll().isEmpty());
        nanos.set(350_000_000); deliveries.remove().run(); assertEquals(350_000_000, session.poll().getFirst().roundTripNanos());
    }
    @Test void codecBoundsAndRoundTrip() {
        session(); var message = new RadioMessage(1, source, registry.address(source), target, registry.address(target), "status", "energy", UUID.randomUUID(), RadioMessage.Type.MESSAGE, new byte[]{3});
        assertArrayEquals(message.payload(), RadioMessageCodec.decode(RadioMessageCodec.encode(message)).payload());
        assertThrows(IllegalArgumentException.class, () -> RadioMessageCodec.decode(new byte[RadioMessageCodec.MAX_FRAME + 1]));
    }
    @Test void staleSamplesAndDirectoryAreScoped() {
        var session = session(RadioRegistry.Permission.TELEMETRY_READ);
        broker.register(target, "status", new RadioServiceBroker.Endpoint() {});
        broker.register(target, "private", new RadioServiceBroker.Endpoint() {});
        assertEquals(List.of("status"), session.services(registry.address(target)));
        broker.publish(target, "status", "energy", new byte[]{1});
        session.subscribe(registry.address(target), "status", Set.of("energy"), 2);
        deliveries.remove().run(); session.poll(); broker.tick(2);
        var stale = session.poll().getFirst();
        assertEquals(RadioServiceBroker.Freshness.STALE, stale.freshness());
        assertEquals("energy", stale.message().channel());
        broker.tick(3); assertTrue(session.poll().isEmpty());
    }
    @Test void queuedCommandsRecheckPermissionAndPayloadBounds() {
        var session = session(RadioRegistry.Permission.COMMAND_SEND);
        var calls = new AtomicLong();
        broker.register(target, "status", new RadioServiceBroker.Endpoint() {
            public void command(RadioMessage message, RadioServiceBroker.Command command) { calls.incrementAndGet(); }
        });
        assertThrows(IllegalArgumentException.class, () -> session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[RadioMessage.MAX_PAYLOAD + 1], 10));
        var command = session.command(registry.address(target), "status", "move", UUID.randomUUID(), new byte[0], 10);
        registry.setScopes(network.network(), network.adminSecret(), source, Set.of());
        deliveries.remove().run(); assertEquals(0, calls.get());
        assertEquals(RadioServiceBroker.State.DISCONNECTED, command.state());
    }

}
