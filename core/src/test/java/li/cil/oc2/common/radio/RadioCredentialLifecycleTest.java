package li.cil.oc2.common.radio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RadioCredentialLifecycleTest {
    private static final String DIMENSION = "overworld";
    private final RadioRegistry registry = new RadioRegistry();
    private final RadioRegistry.CreatedNetwork network = registry.createNetwork();

    private RadioRegistry.Enrollment enroll(final UUID device) {
        final var invite = registry.invite(network.network(), network.adminSecret(), DIMENSION, device, Set.of());
        return registry.enroll(invite.invite(), invite.secret());
    }

    @Test
    void credentialsAreRandomUrlSafeAndUnique() {
        final Set<String> secrets = new HashSet<>();
        for (int i = 0; i < 64; i++) {
            final String secret = enroll(UUID.randomUUID()).secret();
            assertEquals(43, secret.length(), "256 bits as unpadded base64url");
            assertTrue(secret.matches("[A-Za-z0-9_-]{43}"));
            assertTrue(secrets.add(secret), "no credential repeats");
        }
    }

    @Test
    void rotationReplacesOnlyTheRotatedCredential() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final var first = enroll(a);
        final var other = enroll(b);
        final var rotated = registry.rotateCredential(a, first.secret());
        assertNotEquals(first.secret(), rotated.secret());
        assertTrue(registry.authenticate(a, first.secret()).isEmpty(), "old key is dead at once");
        assertTrue(registry.authenticate(a, rotated.secret()).isPresent());
        assertEquals(first.member().number(), rotated.member().number(), "address survives rotation");
        assertEquals(first.member().generation() + 1, rotated.member().generation());
        assertTrue(registry.authenticate(b, other.secret()).isPresent(), "other clients keep their key");
        assertEquals(other.member().generation(), registry.member(b).orElseThrow().generation());
        assertThrows(SecurityException.class, () -> registry.rotateCredential(a, first.secret()), "old key cannot rotate");
        assertThrows(SecurityException.class, () -> registry.rotateCredential(UUID.randomUUID(), rotated.secret()));
    }

    @Test
    void rotatedCredentialsSurviveSaveAndReload() {
        final UUID a = UUID.randomUUID();
        final var rotated = registry.rotateCredential(a, enroll(a).secret());
        final var restored = RadioRegistry.importState(registry.exportState());
        assertTrue(restored.authenticate(a, rotated.secret(), rotated.member().generation()).isPresent());
    }

    @Test
    void individualRevocationLeavesOtherClientsAndReenrollmentGetsANewGeneration() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final var first = enroll(a);
        final var other = enroll(b);
        registry.revoke(network.network(), network.adminSecret(), a);
        assertTrue(registry.authenticate(a, first.secret()).isEmpty());
        assertTrue(registry.authenticate(b, other.secret()).isPresent());
        final var again = enroll(a);
        assertEquals(first.member().generation() + 1, again.member().generation());
        assertNotEquals(first.member().number(), again.member().number(), "revoked address is not reused");
        assertTrue(registry.authenticate(a, first.secret()).isEmpty(), "prior generation key stays dead");
        assertTrue(registry.authenticate(a, again.secret(), again.member().generation()).isPresent());
        assertTrue(registry.authenticate(a, again.secret(), first.member().generation()).isEmpty(),
                "a credential is bound to its enrollment generation");
        assertEquals(other.member().generation(), registry.member(b).orElseThrow().generation());
    }

    @Test
    void secretsNeverAppearInStringForms() {
        final UUID device = UUID.randomUUID();
        final var invitation = registry.invite(network.network(), network.adminSecret(), DIMENSION, device, Set.of());
        final var enrollment = registry.enroll(invitation.invite(), invitation.secret());
        final var rotated = registry.rotateCredential(device, enrollment.secret());
        final String[] secrets = {network.adminSecret(), invitation.secret(), enrollment.secret(), rotated.secret()};
        for (final String text : new String[] {
                network.toString(), invitation.toString(), enrollment.toString(), rotated.toString(),
                enrollment.member().toString(), registry.exportState().networks().getFirst().toString()}) {
            for (final String secret : secrets) assertFalse(text.contains(secret), text);
            assertFalse(text.matches(".*[0-9a-f]{64}.*"), "no credential hash in " + text);
        }
    }
}
