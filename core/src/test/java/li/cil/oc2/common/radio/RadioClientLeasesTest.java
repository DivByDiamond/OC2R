package li.cil.oc2.common.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RadioClientLeasesTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private final RadioRegistry registry = new RadioRegistry();
    private final RadioRegistry.CreatedNetwork network = registry.createNetwork();
    private final RadioClientLeases leases = new RadioClientLeases(registry);
    private final UUID item = UUID.randomUUID();
    private final RadioRegistry.Enrollment enrollment = enroll(item);

    private RadioRegistry.Enrollment enroll(final UUID device) {
        final var invite = registry.invite(network.network(), network.adminSecret(), OVERWORLD, device, Set.of());
        return registry.enroll(invite.invite(), invite.secret());
    }

    @Test
    void activationRotatesTheStoredSecret() {
        final var lease = leases.activate(item, enrollment.secret(), OVERWORLD);
        assertTrue(leases.active(item));
        assertNotEquals(enrollment.secret(), lease.secret());
        assertTrue(registry.authenticate(item, lease.secret()).isPresent());
        assertTrue(registry.authenticate(item, enrollment.secret()).isEmpty());
        assertEquals(enrollment.member().number(), lease.member().number(), "address is stable");
    }

    @Test
    void aCloneCannotRunBesideTheOriginal() {
        // The cloned item data carries the same identity and the same secret.
        final var original = leases.activate(item, enrollment.secret(), OVERWORLD);
        assertThrows(SecurityException.class, () -> leases.activate(item, enrollment.secret(), OVERWORLD));
        assertThrows(SecurityException.class, () -> leases.activate(item, original.secret(), OVERWORLD),
                "even a current secret does not take a held identity");
        assertTrue(leases.active(item));
    }

    @Test
    void aCopyMadeBeforeActivationHoldsAPriorGenerationAndNeverAuthenticatesAgain() {
        final var original = leases.activate(item, enrollment.secret(), OVERWORLD);
        leases.suspend(item, RadioClientLeases.Reason.LOGGED_OUT);
        assertThrows(SecurityException.class, () -> leases.activate(item, enrollment.secret(), OVERWORLD),
                "the stale copy is not a second identity after the original let go");
        final var next = leases.activate(item, original.secret(), OVERWORLD);
        assertEquals(enrollment.member().generation() + 2, next.member().generation());
    }

    @Test
    void everyReasonSuspendsButKeepsIdentityAndPermissions() {
        var secret = enrollment.secret();
        for (final var reason : RadioClientLeases.Reason.values()) {
            final var lease = leases.activate(item, secret, OVERWORLD);
            leases.suspend(item, reason);
            assertFalse(leases.active(item), reason.name());
            assertEquals(enrollment.member().number(), registry.member(item).orElseThrow().number());
            secret = lease.secret();
        }
        assertTrue(registry.authenticate(item, secret).isPresent());
    }

    @Test
    void anotherDimensionIsRefusedWithoutRotation() {
        assertThrows(SecurityException.class, () -> leases.activate(item, enrollment.secret(), "minecraft:the_nether"));
        assertFalse(leases.active(item));
        assertTrue(registry.authenticate(item, enrollment.secret()).isPresent(), "nothing was rotated");
        assertDoesNotThrow(() -> leases.activate(item, enrollment.secret(), OVERWORLD));
    }

    @Test
    void revokedAndWrongCredentialsNeverActivate() {
        assertThrows(SecurityException.class, () -> leases.activate(item, "x".repeat(43), OVERWORLD));
        final var lease = leases.activate(item, enrollment.secret(), OVERWORLD);
        registry.revoke(network.network(), network.adminSecret(), item);
        assertFalse(leases.active(item), "a revoked identity is not active");
        assertThrows(SecurityException.class, () -> leases.activate(item, lease.secret(), OVERWORLD));
        assertFalse(lease.toString().contains(lease.secret()));
    }

    @Test
    void recognizingARevokedItemDoesNotAuthorizeNetworkAccessOrAnOldCopy() {
        final var lease = leases.activate(item, enrollment.secret(), OVERWORLD);
        registry.revoke(network.network(), network.adminSecret(), item);
        assertTrue(registry.knowsIdentity(item));
        assertTrue(registry.recognizesItemCredential(item, lease.secret()));
        assertFalse(registry.recognizesItemCredential(item, enrollment.secret()));
        assertTrue(registry.authenticate(item, lease.secret()).isEmpty());
        assertThrows(SecurityException.class, () -> leases.activate(item, lease.secret(), OVERWORLD));
    }

    @Test
    void enrollmentElsewhereRetiresOldCopiesEvenWhenThePreviousMembershipWasRevoked() {
        final var original = leases.activate(item, enrollment.secret(), OVERWORLD);
        leases.suspend(item, RadioClientLeases.Reason.DIMENSION_CHANGED);
        registry.revoke(network.network(), network.adminSecret(), item);
        final var other = new RadioRegistry();
        final var otherNetwork = other.createNetwork();
        final var invitation = other.invite(otherNetwork.network(), otherNetwork.adminSecret(),
                "minecraft:the_nether", item, Set.of());
        final var newEnrollment = other.enroll(invitation.invite(), invitation.secret());
        final var newLeases = new RadioClientLeases(other);
        final var current = newLeases.activate(item, newEnrollment.secret(), "minecraft:the_nether");
        registry.retireItemCredential(item, original.secret());
        newLeases.suspend(item, RadioClientLeases.Reason.DROPPED);
        assertFalse(registry.recognizesItemCredential(item, original.secret()), "old dimension clone is stale");
        assertTrue(registry.authenticate(item, original.secret()).isEmpty());
        assertTrue(other.recognizesItemCredential(item, current.secret()), "current tablet keeps local access");
        assertDoesNotThrow(() -> newLeases.activate(item, current.secret(), "minecraft:the_nether"));
        assertThrows(SecurityException.class, () -> registry.retireItemCredential(item, original.secret()));
    }

    @Test
    void otherClientsAreNotAffectedByALease() {
        final UUID other = UUID.randomUUID();
        final var otherEnrollment = enroll(other);
        leases.activate(item, enrollment.secret(), OVERWORLD);
        leases.suspend(item, RadioClientLeases.Reason.DIED);
        assertTrue(registry.authenticate(other, otherEnrollment.secret(), otherEnrollment.member().generation()).isPresent());
        assertFalse(leases.active(other));
    }
}
