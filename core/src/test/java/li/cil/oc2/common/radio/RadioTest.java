package li.cil.oc2.common.radio;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class RadioTest {
    private static final String DIMENSION = "overworld";
    private final RadioRegistry registry = new RadioRegistry();
    private final RadioRegistry.CreatedNetwork network = registry.createNetwork();
    private final RadioTopology topology = new RadioTopology();
    private RadioRegistry.Enrollment add(final double x, final double y, final double z, final boolean online) {
        final UUID id = UUID.randomUUID();
        final var invite = registry.invite(network.network(), network.adminSecret(), DIMENSION, id, Set.of());
        final var enrollment = registry.enroll(invite.invite(), invite.secret());
        topology.upsert(new RadioTopology.Node(id, network.network(), DIMENSION, x, y, z, 45, online));
        return enrollment;
    }
    @Test void canonicalAddressesRejectReservedAndAmbiguousInput() {
        assertEquals("7K3F:02Q9", RadioAddress.parse("7k3f:02q9").toString());
        for (final String bad : List.of("0000:0001", "0001:0000", "IIII:0001", "OOOO:0001", "0001-0002", " 001:0002"))
            assertThrows(IllegalArgumentException.class, () -> RadioAddress.parse(bad));
    }
    @Test void nonAsciiLookalikesCannotNormalizeIntoValidAddresses() {
        assertThrows(IllegalArgumentException.class, () -> RadioAddress.parse("ſ001:0001"));
    }
    @Test void circularRangeIgnoresHeightAndUnloadedRelays() {
        final UUID a = add(0, -200000, 0, true).member().id();
        final UUID relay = add(45, 200000, 0, false).member().id();
        final UUID b = add(90, 0, 0, true).member().id();
        assertEquals(List.of(a, relay, b), topology.route(a, b));
        final UUID corner = add(45, 0, 45, true).member().id();
        assertEquals(3, topology.route(a, corner).size());
        topology.remove(relay);
        assertTrue(topology.route(a, b).isEmpty());
        assertTrue(topology.route(a, corner).isEmpty());
        topology.upsert(new RadioTopology.Node(b, network.network(), DIMENSION, 0, 0, 0, 45, false));
        assertTrue(topology.route(a, b).isEmpty());
    }
    @Test void scopesRevocationAndPersistence() {
        final UUID id = UUID.randomUUID();
        final UUID target = UUID.randomUUID();
        assertThrows(SecurityException.class, () -> registry.invite(network.network(), "wrong", DIMENSION, id, Set.of()));
        final var invite = registry.invite(network.network(), network.adminSecret(), DIMENSION, id,
            Set.of(new RadioRegistry.Scope(RadioRegistry.Permission.COMMAND_SEND, target, "motor")));
        final var member = registry.enroll(invite.invite(), invite.secret());
        assertThrows(SecurityException.class, () -> registry.enroll(invite.invite(), invite.secret()));
        assertTrue(registry.authorized(id, member.secret(), RadioRegistry.Permission.COMMAND_SEND, target, "motor"));
        assertFalse(registry.authorized(id, member.secret(), RadioRegistry.Permission.COMMAND_SEND, target, "disk"));
        final RadioRegistry restored = RadioRegistry.importState(registry.exportState());
        assertEquals(registry.address(id), restored.address(id));
        assertTrue(restored.authenticate(id, member.secret()).isPresent());
        restored.revoke(network.network(), network.adminSecret(), id);
        assertTrue(restored.authenticate(id, member.secret()).isEmpty());
        final var next = restored.invite(network.network(), network.adminSecret(), DIMENSION, id, Set.of());
        final var reenrolled = restored.enroll(next.invite(), next.secret());
        assertNotEquals(member.member().number(), reenrolled.member().number());
        assertEquals(2, reenrolled.member().generation());
        assertTrue(restored.authenticate(id, member.secret()).isEmpty());
    }
    @Test void schedulerDelayTtlDedupAndRevocation() {
        final UUID a = add(0, 0, 0, true).member().id();
        final UUID relay = add(45, 0, 0, false).member().id();
        final UUID b = add(90, 0, 0, true).member().id();
        final RadioScheduler scheduler = new RadioScheduler(registry, topology, RadioScheduler.Config.defaults());
        final UUID packet = UUID.randomUUID();
        assertEquals(RadioScheduler.Result.TTL_EXCEEDED, scheduler.submit(packet, a, b, new byte[]{1}, 1, 0));
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(packet, a, b, new byte[]{1}, 4, 0));
        assertEquals(RadioScheduler.Result.DUPLICATE, scheduler.submit(packet, a, b, new byte[]{1}, 4, 0));
        final List<Integer> received = new ArrayList<>();
        scheduler.tick(1, (source, target, frame, ttl) -> received.add(ttl));
        assertTrue(received.isEmpty());
        scheduler.tick(2, (source, target, frame, ttl) -> received.add(ttl));
        assertEquals(List.of(2), received);
        scheduler.submit(UUID.randomUUID(), a, b, new byte[]{1}, 4, 2);
        registry.revoke(network.network(), network.adminSecret(), relay);
        scheduler.tick(4, (source, target, frame, ttl) -> fail("Revoked relay delivered"));
    }
    @Test void linkBandwidthSerializesFramesPerSender() {
        final UUID a = add(0, 0, 0, true).member().id();
        final UUID b = add(10, 0, 0, true).member().id();
        // 100 bytes per tick, one hop: a 250-byte frame occupies 3 ticks.
        final RadioScheduler scheduler = new RadioScheduler(registry, topology,
                new RadioScheduler.Config(1, 8, 2048, 64, 64, 64, 64, 50, 100));
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(UUID.randomUUID(), a, b, new byte[250], 4, 0));
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(UUID.randomUUID(), a, b, new byte[100], 4, 0));
        final List<Long> arrivals = new ArrayList<>();
        for (long tick = 1; tick <= 8; tick++) {
            final long now = tick;
            scheduler.tick(tick, (source, target, frame, ttl) -> arrivals.add(now));
        }
        // First frame: last transmit tick 2, plus one hop = tick 3. Second starts at 3: arrives at 4.
        assertEquals(List.of(3L, 4L), arrivals);
    }
    @Test void deliveryDelayIsCappedRegardlessOfHops() {
        final UUID a = add(0, 0, 0, true).member().id();
        for (int i = 1; i < 10; i++) add(i * 40, 0, 0, false);
        final UUID b = add(400, 0, 0, true).member().id();
        final RadioScheduler scheduler = new RadioScheduler(registry, topology,
                new RadioScheduler.Config(1, 64, 2048, 64, 64, 64, 64, 50, 1024, 2));
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(UUID.randomUUID(), a, b, new byte[10], 64, 0));
        final List<Long> arrivals = new ArrayList<>();
        for (long tick = 1; tick <= 6; tick++) {
            final long now = tick;
            scheduler.tick(tick, (source, target, frame, ttl) -> arrivals.add(now));
        }
        assertEquals(List.of(2L), arrivals);
    }
    @Test void backlogBeyondLifetimeIsRefused() {
        final UUID a = add(0, 0, 0, true).member().id();
        final UUID b = add(10, 0, 0, true).member().id();
        final RadioScheduler scheduler = new RadioScheduler(registry, topology,
                new RadioScheduler.Config(1, 8, 2048, 64, 64, 64, 64, 4, 100));
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(UUID.randomUUID(), a, b, new byte[300], 4, 0));
        assertEquals(RadioScheduler.Result.QUEUE_FULL, scheduler.submit(UUID.randomUUID(), a, b, new byte[300], 4, 0));
    }
    @Test void boundsLifetimeAndIsolation() {
        final UUID a = add(0, 0, 0, true).member().id();
        final UUID b = add(45, 0, 0, true).member().id();
        final RadioScheduler scheduler = new RadioScheduler(registry, topology, new RadioScheduler.Config(3, 8, 8, 1, 1, 1, 2, 2));
        assertEquals(RadioScheduler.Result.INVALID_FRAME, scheduler.submit(UUID.randomUUID(), a, b, new byte[9], 4, 0));
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(UUID.randomUUID(), a, b, new byte[]{1}, 4, 0));
        assertEquals(RadioScheduler.Result.QUEUE_FULL, scheduler.submit(UUID.randomUUID(), a, b, new byte[]{1}, 4, 0));
        scheduler.tick(3, (source, target, frame, ttl) -> fail("Expired frame delivered"));
        final var foreign = registry.createNetwork();
        final var invitation = registry.invite(foreign.network(), foreign.adminSecret(), DIMENSION, UUID.randomUUID(), Set.of());
        final var member = registry.enroll(invitation.invite(), invitation.secret());
        assertEquals(RadioScheduler.Result.UNAUTHORIZED, scheduler.submit(UUID.randomUUID(), a, member.member().id(), new byte[]{1}, 4, 3));
        assertTrue(registry.resolve(member.member().id(), member.secret(), registry.address(a)).isEmpty());
    }
    @Test void importedRegistryRejectsCollisionsAndBrokenAllocators() {
        add(0, 0, 0, true);
        final var state = registry.exportState();
        final var original = state.networks().getFirst();
        final var collision = new RadioRegistry.Network(UUID.randomUUID(), original.prefix(), original.adminHash(), 1);
        assertThrows(IllegalArgumentException.class, () -> RadioRegistry.importState(new RadioRegistry.State(
            state.nextPrefix(), List.of(original, collision), state.members(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> RadioRegistry.importState(new RadioRegistry.State(
            1, state.networks(), state.members(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> new RadioRegistry.Scope(RadioRegistry.Permission.COMMAND_SEND, null, ""));
    }
    @Test void unloadedRecipientAndReenrolledRelayCancelPendingFrames() {
        final UUID a = add(0, 0, 0, true).member().id();
        final UUID relay = add(45, 0, 0, false).member().id();
        final UUID b = add(90, 0, 0, true).member().id();
        final RadioScheduler scheduler = new RadioScheduler(registry, topology, RadioScheduler.Config.defaults());
        scheduler.submit(UUID.randomUUID(), a, b, new byte[]{1}, 4, 0);
        registry.revoke(network.network(), network.adminSecret(), relay);
        final var invitation = registry.invite(network.network(), network.adminSecret(), DIMENSION, relay, Set.of());
        registry.enroll(invitation.invite(), invitation.secret());
        scheduler.tick(2, (source, target, frame, ttl) -> fail("Stale relay generation delivered"));
        scheduler.submit(UUID.randomUUID(), a, b, new byte[]{1}, 4, 2);
        topology.upsert(new RadioTopology.Node(b, network.network(), DIMENSION, 90, 0, 0, 45, false));
        scheduler.tick(4, (source, target, frame, ttl) -> fail("Offline recipient delivered"));
        assertEquals(RadioScheduler.Result.NO_ROUTE, scheduler.submit(UUID.randomUUID(), a, b, new byte[]{1}, 4, 4));
    }
    @Test void warmedSourceRoutesInvalidateOnChangedRelayAndRemoval() {
        final UUID a = add(0, 0, 0, true).member().id();
        final UUID relay = add(45, 0, 0, false).member().id();
        final UUID b = add(90, 0, 0, true).member().id();
        final var saved = topology.node(relay).orElseThrow();
        assertEquals(List.of(a, relay, b), topology.route(a, b));
        topology.upsert(saved);
        assertEquals(List.of(a, relay, b), topology.route(a, b));
        topology.upsert(new RadioTopology.Node(relay, network.network(), DIMENSION, 45, 0, 46, 45, false));
        assertTrue(topology.route(a, b).isEmpty());
        topology.upsert(saved);
        assertEquals(List.of(a, relay, b), topology.route(a, b));
        topology.remove(relay);
        assertTrue(topology.route(a, b).isEmpty());
        assertTrue(topology.node(relay).isEmpty());
    }
}
