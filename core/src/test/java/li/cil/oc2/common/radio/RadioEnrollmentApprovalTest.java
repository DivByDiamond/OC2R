package li.cil.oc2.common.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RadioEnrollmentApprovalTest {
    private static final RadioRegistry.Scope SEND =
            new RadioRegistry.Scope(RadioRegistry.Permission.MESSAGE_SEND, null, null);
    private static final RadioRegistry.Scope COMMAND =
            new RadioRegistry.Scope(RadioRegistry.Permission.COMMAND_SEND, null, null);

    private static RadioRegistry.Invitation approvalInvite(
            final RadioRegistry registry, final RadioRegistry.CreatedNetwork network, final UUID device,
            final Set<RadioRegistry.Scope> scopes) {
        return registry.invite(network.network(), network.adminSecret(), "overworld", device, scopes, 0, 100, true);
    }

    @Test
    void requestGrantsNothingUntilApprovedAndApprovalAdmitsOnlyApprovedScopes() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = approvalInvite(registry, network, device, Set.of(SEND, COMMAND));
        final var ticket = registry.requestEnrollment(invite.invite(), invite.secret(), 10);
        assertTrue(registry.member(device).isEmpty());
        assertTrue(registry.authenticate(device, ticket.secret()).isEmpty());
        final var listed = registry.pendingEnrollments(network.network(), network.adminSecret(), 10);
        assertEquals(1, listed.size());
        assertEquals(device, listed.getFirst().device());
        assertEquals(Set.of(SEND, COMMAND), listed.getFirst().requestedScopes());

        final var member = registry.approveEnrollment(network.network(), network.adminSecret(),
                ticket.request(), Set.of(SEND), 11);
        assertEquals(Set.of(SEND), member.scopes());
        assertTrue(registry.authorized(device, ticket.secret(), RadioRegistry.Permission.MESSAGE_SEND, null, null));
        assertFalse(registry.authorized(device, ticket.secret(), RadioRegistry.Permission.COMMAND_SEND, null, null));
        assertTrue(registry.pendingEnrollments(network.network(), network.adminSecret(), 11).isEmpty());
    }

    @Test
    void approvalCannotWidenRequestedScopes() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var invite = approvalInvite(registry, network, UUID.randomUUID(), Set.of(SEND));
        final var ticket = registry.requestEnrollment(invite.invite(), invite.secret(), 0);
        assertThrows(SecurityException.class, () -> registry.approveEnrollment(network.network(),
                network.adminSecret(), ticket.request(), Set.of(SEND, COMMAND), 1));
        assertEquals(1, registry.pendingEnrollments(network.network(), network.adminSecret(), 1).size());
    }

    @Test
    void approvalRequiredInviteCannotEnrollDirectlyAndDirectInviteCannotRequest() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var gated = approvalInvite(registry, network, UUID.randomUUID(), Set.of());
        assertThrows(SecurityException.class, () -> registry.enroll(gated.invite(), gated.secret(), 0));
        final var direct = registry.invite(network.network(), network.adminSecret(), "overworld",
                UUID.randomUUID(), Set.of(), 0, 100);
        assertThrows(SecurityException.class, () -> registry.requestEnrollment(direct.invite(), direct.secret(), 0));
        assertNotNull(registry.enroll(direct.invite(), direct.secret(), 0));
    }

    @Test
    void wrongSecretsAreRejected() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var invite = approvalInvite(registry, network, UUID.randomUUID(), Set.of(SEND));
        assertThrows(SecurityException.class, () -> registry.requestEnrollment(invite.invite(), "x", 0));
        final var ticket = registry.requestEnrollment(invite.invite(), invite.secret(), 0);
        assertThrows(SecurityException.class,
                () -> registry.pendingEnrollments(network.network(), "wrong", 0));
        assertThrows(SecurityException.class, () -> registry.approveEnrollment(network.network(), "wrong",
                ticket.request(), Set.of(SEND), 0));
        assertThrows(SecurityException.class,
                () -> registry.denyEnrollment(network.network(), "wrong", ticket.request(), 0));
        // Another network's admin cannot see or approve it.
        final var other = registry.createNetwork();
        assertTrue(registry.pendingEnrollments(other.network(), other.adminSecret(), 0).isEmpty());
        assertThrows(SecurityException.class, () -> registry.approveEnrollment(other.network(),
                other.adminSecret(), ticket.request(), Set.of(), 0));
    }

    @Test
    void doubleApproveAndApproveAfterDenyFailCleanly() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var a = approvalInvite(registry, network, UUID.randomUUID(), Set.of(SEND));
        final var b = approvalInvite(registry, network, UUID.randomUUID(), Set.of(SEND));
        final var first = registry.requestEnrollment(a.invite(), a.secret(), 0);
        final var second = registry.requestEnrollment(b.invite(), b.secret(), 0);
        registry.approveEnrollment(network.network(), network.adminSecret(), first.request(), Set.of(SEND), 1);
        assertThrows(SecurityException.class, () -> registry.approveEnrollment(network.network(),
                network.adminSecret(), first.request(), Set.of(SEND), 2));
        registry.denyEnrollment(network.network(), network.adminSecret(), second.request(), 1);
        assertThrows(SecurityException.class, () -> registry.approveEnrollment(network.network(),
                network.adminSecret(), second.request(), Set.of(SEND), 2));
        assertThrows(SecurityException.class,
                () -> registry.denyEnrollment(network.network(), network.adminSecret(), second.request(), 2));
        assertTrue(registry.authenticate(UUID.randomUUID(), second.secret()).isEmpty());
    }

    @Test
    void deniedRequestNeverAdmitsAndInviteCannotBeReplayed() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = approvalInvite(registry, network, device, Set.of(SEND));
        final var ticket = registry.requestEnrollment(invite.invite(), invite.secret(), 0);
        registry.denyEnrollment(network.network(), network.adminSecret(), ticket.request(), 1);
        assertTrue(registry.member(device).isEmpty());
        assertTrue(registry.authenticate(device, ticket.secret()).isEmpty());
        assertThrows(SecurityException.class, () -> registry.requestEnrollment(invite.invite(), invite.secret(), 2));
        assertTrue(registry.pendingEnrollments(network.network(), network.adminSecret(), 2).isEmpty());
    }

    @Test
    void requestsExpireExactlyAtTheirDeadlineAndCannotBeApproved() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = registry.invite(network.network(), network.adminSecret(), "overworld", device,
                Set.of(SEND), 0, 100, true);
        final var ticket = registry.requestEnrollment(invite.invite(), invite.secret(), 50);
        final long deadline = 50 + RadioRegistry.PENDING_LIFETIME_TICKS;
        assertEquals(1, registry.pendingEnrollments(network.network(), network.adminSecret(), deadline - 1).size());
        assertThrows(SecurityException.class, () -> registry.approveEnrollment(network.network(),
                network.adminSecret(), ticket.request(), Set.of(SEND), deadline));
        assertTrue(registry.member(device).isEmpty());
        assertEquals(0, registry.expireEnrollmentRequests(deadline));
        assertTrue(registry.pendingEnrollments(network.network(), network.adminSecret(), deadline).isEmpty());
    }

    @Test
    void expiredInvitationCannotOpenARequest() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var invite = approvalInvite(registry, network, UUID.randomUUID(), Set.of());
        assertThrows(SecurityException.class, () -> registry.requestEnrollment(invite.invite(), invite.secret(), 100));
    }

    @Test
    void pendingRequestsAreBoundedPerNetworkAndPerDevice() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        for (int i = 0; i < RadioRegistry.MAX_PENDING_PER_NETWORK; i++) {
            final var invite = approvalInvite(registry, network, UUID.randomUUID(), Set.of());
            registry.requestEnrollment(invite.invite(), invite.secret(), 0);
        }
        final var overflow = approvalInvite(registry, network, UUID.randomUUID(), Set.of());
        assertThrows(IllegalStateException.class,
                () -> registry.requestEnrollment(overflow.invite(), overflow.secret(), 0));
        // A different network is unaffected and a rejected request does not consume the invite.
        final var other = registry.createNetwork();
        final var elsewhere = approvalInvite(registry, other, UUID.randomUUID(), Set.of());
        assertNotNull(registry.requestEnrollment(elsewhere.invite(), elsewhere.secret(), 0));
        // Capacity is reclaimed after expiry.
        final long later = RadioRegistry.PENDING_LIFETIME_TICKS;
        final var fresh = registry.invite(network.network(), network.adminSecret(), "overworld",
                UUID.randomUUID(), Set.of(), later, 100, true);
        assertNotNull(registry.requestEnrollment(fresh.invite(), fresh.secret(), later));
    }

    @Test
    void sameDeviceCannotHoldTwoPendingRequests() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var one = approvalInvite(registry, network, device, Set.of());
        final var two = approvalInvite(registry, network, device, Set.of());
        registry.requestEnrollment(one.invite(), one.secret(), 0);
        assertThrows(IllegalStateException.class, () -> registry.requestEnrollment(two.invite(), two.secret(), 0));
        assertEquals(1, registry.pendingEnrollments(network.network(), network.adminSecret(), 0).size());
    }

    @Test
    void pendingRequestsSurviveRegistryReloadWithoutSecrets() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = approvalInvite(registry, network, device, Set.of(SEND));
        final var ticket = registry.requestEnrollment(invite.invite(), invite.secret(), 0);
        final var state = registry.exportState();
        assertFalse(state.pending().toString().contains(ticket.secret()));
        final var restored = RadioRegistry.importState(state);
        final var member = restored.approveEnrollment(network.network(), network.adminSecret(),
                ticket.request(), Set.of(SEND), 1);
        assertEquals(device, member.id());
        assertTrue(restored.authenticate(device, ticket.secret()).isPresent());
    }
}
