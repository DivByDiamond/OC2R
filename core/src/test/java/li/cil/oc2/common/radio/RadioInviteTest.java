package li.cil.oc2.common.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RadioInviteTest {
    @Test
    void invitationsExpireExactlyAtTheirDeadlineAndSurviveRegistryReload() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var valid = registry.invite(network.network(), network.adminSecret(), "overworld",
                UUID.randomUUID(), Set.of(), 100, 20);
        final var expired = registry.invite(network.network(), network.adminSecret(), "overworld",
                UUID.randomUUID(), Set.of(), 100, 20);
        final var restored = RadioRegistry.importState(registry.exportState());
        assertNotNull(restored.enroll(valid.invite(), valid.secret(), 119));
        assertThrows(SecurityException.class, () -> restored.enroll(expired.invite(), expired.secret(), 120));
        assertEquals(1, restored.expireInvites(120));
        assertEquals(0, restored.expireInvites(121));
    }

    @Test
    void legacyInvitationsWithoutExpiryCannotBecomePermanentCredentials() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final var invitation = registry.invite(network.network(), network.adminSecret(), "overworld",
                UUID.randomUUID(), Set.of());
        final var saved = registry.exportState();
        final var previous = saved.invites().getFirst();
        final var legacy = new RadioRegistry.Invite(previous.id(), previous.network(),
                previous.dimension(), previous.device(), previous.scopes(), previous.secretHash(), 0);
        final var restored = RadioRegistry.importState(new RadioRegistry.State(saved.nextPrefix(),
                saved.networks(), saved.members(), List.of(legacy), saved.reservations()));
        assertThrows(SecurityException.class, () -> restored.enroll(invitation.invite(), invitation.secret(), 0));
        assertEquals(1, restored.expireInvites(0));
    }

    @Test
    void scopeChangesInvalidateSessionGenerationWithoutChangingAddressOrCredential() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = registry.invite(network.network(), network.adminSecret(), "overworld", device, Set.of());
        final var member = registry.enroll(invite.invite(), invite.secret());
        final var address = registry.address(device);
        final var scope = new RadioRegistry.Scope(RadioRegistry.Permission.MESSAGE_SEND, null, "status");
        assertThrows(SecurityException.class, () -> registry.setScopes(network.network(), "wrong", device, Set.of(scope)));
        registry.setScopes(network.network(), network.adminSecret(), device, Set.of(scope));
        assertEquals(address, registry.address(device));
        assertEquals(2, registry.authenticate(device, member.secret()).orElseThrow().generation());
        assertTrue(registry.authorized(device, member.secret(), RadioRegistry.Permission.MESSAGE_SEND, null, "status"));
        assertFalse(registry.authorized(device, member.secret(), RadioRegistry.Permission.COMMAND_SEND, null, "status"));
        registry.setScopes(network.network(), network.adminSecret(), device, Set.of(scope));
        assertEquals(2, registry.member(device).orElseThrow().generation());
    }

    @Test
    void expiredInvitationCapacityIsReclaimedBeforeIssuingNewInvites() {
        final var registry = new RadioRegistry();
        final var network = registry.createNetwork();
        for (int i = 0; i < RadioRegistry.MAX_INVITES; i++) {
            registry.invite(network.network(), network.adminSecret(), "overworld", UUID.randomUUID(), Set.of(), 0, 1);
        }
        assertNotNull(registry.invite(network.network(), network.adminSecret(), "overworld",
                UUID.randomUUID(), Set.of(), 1, 20));
        assertEquals(1, registry.exportState().invites().size());
        assertThrows(IllegalArgumentException.class, () -> registry.invite(network.network(), network.adminSecret(),
                "overworld", UUID.randomUUID(), Set.of(), 1, 0));
    }
}
