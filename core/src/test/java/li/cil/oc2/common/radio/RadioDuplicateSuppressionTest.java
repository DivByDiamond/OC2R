package li.cil.oc2.common.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RadioDuplicateSuppressionTest {
    @Test
    void filterRemembersWithinTheLifetimeAndForgetsAfterwards() {
        final var filter = new RadioDuplicateFilter<String>(8, 10);
        assertTrue(filter.firstSeen("a", 0));
        assertFalse(filter.firstSeen("a", 9));
        assertFalse(filter.firstSeen("a", 9), "a duplicate does not extend the first sighting");
        assertTrue(filter.firstSeen("a", 10));
    }

    @Test
    void filterStateIsBoundedAndForgetsTheOldestFirst() {
        final var filter = new RadioDuplicateFilter<Integer>(3, 1000);
        for (int i = 0; i < 100; i++) assertTrue(filter.firstSeen(i, i));
        assertEquals(3, filter.size());
        assertFalse(filter.firstSeen(99, 100));
        assertTrue(filter.firstSeen(0, 100), "an evicted key is new again");
        assertThrows(IllegalArgumentException.class, () -> new RadioDuplicateFilter<>(0, 1));
    }

    @Test
    void loopedFramesAreDroppedButDeliberateResendsPass() {
        final var filter = new RadioFrameFilter();
        final UUID network = UUID.randomUUID();
        final byte[] frame = {1, 2, 3};
        assertTrue(filter.admit(network, frame, 16, 0));
        assertFalse(filter.admit(network, frame, 15, 1), "the same bytes back with a lower TTL are a loop");
        assertFalse(filter.admit(network, frame.clone(), 3, 2));
        assertTrue(filter.admit(network, frame, 16, 3), "a resend with a full TTL is not a loop");
        assertTrue(filter.admit(UUID.randomUUID(), frame, 14, 3), "another network is independent");
        assertTrue(filter.admit(network, new byte[] {1, 2, 4}, 14, 3));
        assertTrue(filter.admit(network, frame, 15, 3 + RadioFrameFilter.WINDOW_TICKS + 1), "the entry expired");
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Distinct frames are the point.
    void frameFilterStaysBounded() {
        final var filter = new RadioFrameFilter();
        final UUID network = UUID.randomUUID();
        for (int i = 0; i < RadioFrameFilter.CAPACITY + 50; i++)
            filter.admit(network, new byte[] {(byte) i, (byte) (i >> 8), (byte) (i >> 16)}, 16, 0);
        assertEquals(RadioFrameFilter.CAPACITY, filter.size());
    }

    @Test
    void aRingOfTowersCannotMultiplyTrafficBeyondOneFloodPerTtlStep() {
        // Three towers wired as a ring: each delivery re-enters transmit with one hop less TTL.
        final var filter = new RadioFrameFilter();
        final UUID network = UUID.randomUUID();
        final byte[] frame = {9, 9, 9};
        int ttl = 16;
        int floods = 0;
        for (int hop = 0; hop < 12; hop++) {
            if (filter.admit(network, frame, ttl, hop / 3)) floods++;
            ttl--;
        }
        assertEquals(1, floods);
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // One node per iteration.
    void schedulerDropsADuplicatePacketAndDeliversOnceWithItsOrigin() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var topology = new RadioTopology();
        final List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final UUID id = UUID.randomUUID();
            final var invite = registry.invite(network.network(), network.adminSecret(), "overworld", id, Set.of());
            registry.enroll(invite.invite(), invite.secret());
            topology.upsert(new RadioTopology.Node(id, network.network(), "overworld", i * 10, 0, 0, 45, true));
            ids.add(id);
        }
        final var scheduler = new RadioScheduler(registry, topology, RadioScheduler.Config.defaults());
        final UUID packet = UUID.randomUUID();
        final UUID origin = UUID.randomUUID();
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(packet, ids.get(0), origin, ids.get(1), new byte[] {1}, 8, 0));
        assertEquals(RadioScheduler.Result.DUPLICATE, scheduler.submit(packet, ids.get(0), origin, ids.get(1), new byte[] {1}, 8, 0));
        final List<UUID> origins = new ArrayList<>();
        scheduler.tick(5, (source, from, target, frame, ttl) -> origins.add(from));
        assertEquals(List.of(origin), origins);
        // Without an explicit origin the source is the origin, as before.
        final UUID second = UUID.randomUUID();
        assertEquals(RadioScheduler.Result.ACCEPTED, scheduler.submit(second, ids.get(0), ids.get(1), new byte[] {2}, 8, 6));
        scheduler.tick(10, (source, from, target, frame, ttl) -> origins.add(from));
        assertEquals(List.of(origin, ids.get(0)), origins);
    }
}
