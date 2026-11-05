package li.cil.oc2.common.radio;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RadioAllocationTest {
    private static final class Slots extends SecureRandom {
        private final ArrayDeque<Integer> values = new ArrayDeque<>();
        private final ArrayDeque<Integer> bounds = new ArrayDeque<>();

        Slots(final Integer... slots) {
            super();
            values.addAll(List.of(slots));
        }

        @Override
        public int nextInt(final int bound) {
            bounds.add(bound);
            final int value = values.remove();
            assertTrue(value >= 0 && value < bound);
            return value;
        }
    }

    private static RadioRegistry.Enrollment enroll(
            final RadioRegistry registry, final RadioRegistry.CreatedNetwork network, final UUID id) {
        final var invitation = registry.invite(
            network.network(), network.adminSecret(), "overworld", id, Set.of());
        return registry.enroll(invitation.invite(), invitation.secret());
    }

    @Test void randomRanksSkipOccupiedPrefixesAndNumbers() {
        final Slots random = new Slots(41, 41, 99, 99, 0, RadioAddress.MAX - 4);
        final var registry = new RadioRegistry(random);
        final var first = registry.createNetwork();
        final var second = registry.createNetwork();
        assertEquals(42, registry.exportState().networks().stream()
            .filter(value -> value.id().equals(first.network())).findFirst().orElseThrow().prefix());
        assertEquals(43, registry.exportState().networks().stream()
            .filter(value -> value.id().equals(second.network())).findFirst().orElseThrow().prefix());
        assertEquals(100, enroll(registry, first, UUID.randomUUID()).member().number());
        assertEquals(101, enroll(registry, first, UUID.randomUUID()).member().number());
        assertEquals(1, enroll(registry, first, UUID.randomUUID()).member().number());
        assertEquals(RadioAddress.MAX, enroll(registry, first, UUID.randomUUID()).member().number());
        assertEquals(List.of(RadioAddress.MAX, RadioAddress.MAX - 1, RadioAddress.MAX,
            RadioAddress.MAX - 1, RadioAddress.MAX - 2, RadioAddress.MAX - 3),
            List.copyOf(random.bounds));
    }

    @Test void reenrollmentReservationsSurviveRestarts() {
        final var registry = new RadioRegistry(new Slots(17, 99, 99));
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var original = enroll(registry, network, device);
        registry.revoke(network.network(), network.adminSecret(), device);
        final var replacement = enroll(registry, network, device);
        assertEquals(100, original.member().number());
        assertEquals(101, replacement.member().number());
        final var state = registry.exportState();
        final var restored = RadioRegistry.importState(state, new Slots(99));
        assertEquals(registry.address(device), restored.address(device));
        assertTrue(restored.authenticate(device, replacement.secret()).isPresent());
        restored.revoke(network.network(), network.adminSecret(), device);
        assertEquals(102, enroll(restored, network, device).member().number());
        assertEquals(3, restored.exportState().reservations().size());
        assertEquals(3, RadioRegistry.importState(restored.exportState()).exportState().reservations().size());
    }

    @Test void legacyFloorProtectsErasedNumbersAndMissingReservations() {
        final var registry = new RadioRegistry(new Slots(0, 0));
        final var network = registry.createNetwork();
        final var member = enroll(registry, network, UUID.randomUUID());
        final var saved = registry.exportState();
        final var legacyNetwork = new RadioRegistry.Network(network.network(), 1,
            saved.networks().getFirst().adminHash(), 500);
        final var legacy = new RadioRegistry.State(2, List.of(legacyNetwork),
            List.of(member.member()), List.of());
        final var missingReservations = new RadioRegistry.State(legacy.nextPrefix(), legacy.networks(),
            legacy.members(), legacy.invites(), null);
        final var restored = RadioRegistry.importState(missingReservations, new Slots(0, 0));
        assertEquals(registry.address(member.member().id()), restored.address(member.member().id()));
        assertTrue(restored.authenticate(member.member().id(), member.secret()).isPresent());
        assertEquals(500, enroll(restored, network, UUID.randomUUID()).member().number());
        assertEquals(501, enroll(restored, network, UUID.randomUUID()).member().number());
        assertEquals(500, restored.exportState().networks().getFirst().nextDevice());
    }

    @Test void exhaustedLegacyDeviceFloorDoesNotExhaustRandomPrefixes() {
        final var registry = new RadioRegistry(new Slots(0));
        final var network = registry.createNetwork();
        final var saved = registry.exportState().networks().getFirst();
        final var state = new RadioRegistry.State(RadioAddress.MAX + 1,
            List.of(new RadioRegistry.Network(saved.id(), RadioAddress.MAX,
                saved.adminHash(), RadioAddress.MAX + 1)), List.of(), List.of());
        final var restored = RadioRegistry.importState(state, new Slots(0));
        assertNotNull(restored.createNetwork());
        assertThrows(IllegalStateException.class,
            () -> enroll(restored, network, UUID.randomUUID()));
    }

    @Test void reservationCapacityFailsWithoutConsumingInvitation() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var saved = registry.exportState();
        final var reservations = java.util.stream.IntStream.rangeClosed(1, RadioRegistry.MAX_RESERVATIONS)
            .mapToObj(number -> new RadioRegistry.Reservation(network.network(), number)).toList();
        final var restored = RadioRegistry.importState(new RadioRegistry.State(saved.nextPrefix(),
            saved.networks(), List.of(), List.of(), reservations));
        final var invitation = restored.invite(network.network(), network.adminSecret(),
            "overworld", UUID.randomUUID(), Set.of());
        assertThrows(IllegalStateException.class,
            () -> restored.enroll(invitation.invite(), invitation.secret()));
        assertEquals(1, restored.exportState().invites().size());
        assertTrue(restored.exportState().members().isEmpty());
    }

    @Test void malformedReservationsAndAddressBoundsAreRejected() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var saved = registry.exportState();
        for (final var reservation : List.of(
                new RadioRegistry.Reservation(UUID.randomUUID(), 1),
                new RadioRegistry.Reservation(network.network(), 0),
                new RadioRegistry.Reservation(network.network(), RadioAddress.MAX + 1))) {
            assertThrows(IllegalArgumentException.class, () -> RadioRegistry.importState(
                new RadioRegistry.State(saved.nextPrefix(), saved.networks(), List.of(), List.of(), // NOPMD - per case
                    List.of(reservation))));
        }
        final var reservation = new RadioRegistry.Reservation(network.network(), 5);
        assertThrows(IllegalArgumentException.class, () -> RadioRegistry.importState(
            new RadioRegistry.State(saved.nextPrefix(), saved.networks(), List.of(), List.of(),
                List.of(reservation, reservation))));
        assertThrows(IllegalArgumentException.class, () -> RadioRegistry.importState(
            new RadioRegistry.State(saved.nextPrefix(), saved.networks(), List.of(), List.of(),
                java.util.Collections.nCopies(RadioRegistry.MAX_RESERVATIONS + 1, reservation))));
    }
}
