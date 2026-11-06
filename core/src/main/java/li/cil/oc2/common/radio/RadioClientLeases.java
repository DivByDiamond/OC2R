package li.cil.oc2.common.radio;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Registry-level rules for a wireless client whose identity travels inside an item (a tablet).
 * The server, never the item data, decides whether the identity is in use.
 *
 * <ul>
 *   <li>One identity has at most one active holder. A second {@link #activate} while the first is
 *       active is refused, so a clone of the item data cannot run beside the original.
 *   <li>Every activation rotates the credential and returns the new secret, which the holder must
 *       store in the item. A copy made earlier carries the previous secret, which is a prior
 *       generation and never authenticates again, so duplicated item data does not create a second
 *       authenticated identity even after the original was suspended.
 *   <li>{@link #suspend} (drop, death, logout, dimension change, transfer to another holder) ends
 *       the activity and keeps identity, address and permissions. The next holder activates with the
 *       secret stored in the item. Activity is not saved, so a restart suspends every client.
 *   <li>An identity only activates in the dimension it was enrolled in. Another dimension is refused
 *       without side effects; moving to another network or dimension needs an explicit re-enrollment.
 * </ul>
 */
public final class RadioClientLeases {
    /** Why a holder stopped using the identity. Every reason keeps the identity and its credential. */
    public enum Reason {
        DROPPED,
        DIED,
        LOGGED_OUT,
        DIMENSION_CHANGED,
        TRANSFERRED,
        DESTROYED
    }

    /** The rotated credential; its string form hides the secret. */
    public record Lease(RadioRegistry.Member member, String secret) {
        @Override
        public String toString() {
            return "Lease[" + member + "]";
        }
    }

    private final RadioRegistry registry;
    private final Set<UUID> holders = new HashSet<>();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The leases use the live registry of a level.")
    public RadioClientLeases(final RadioRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    /**
     * Takes the identity for one holder and rotates its credential. Throws SecurityException when the
     * credential is wrong, outdated or revoked, when the identity is already active or when it was
     * enrolled in another dimension. The caller stores {@link Lease#secret()} and marks the registry
     * data dirty.
     */
    public Lease activate(final UUID device, final String secret, final String dimension) {
        final var member = registry.authenticate(device, secret)
                .orElseThrow(() -> new SecurityException("Authentication failed"));
        if (!member.dimension().equals(dimension)) throw new SecurityException("Enrolled in another dimension");
        if (holders.contains(device)) throw new SecurityException("Identity already active");
        final var rotated = registry.rotateCredential(device, secret);
        holders.add(device);
        return new Lease(rotated.member(), rotated.secret());
    }

    /** Ends the activity of an identity. Unknown, revoked and already suspended identities are ignored. */
    public void suspend(final UUID device, final Reason reason) {
        Objects.requireNonNull(reason);
        holders.remove(device);
    }

    /** Whether the identity is held right now. A revoked or removed identity is never active. */
    public boolean active(final UUID device) {
        if (registry.member(device).isEmpty()) holders.remove(device);
        return holders.contains(device);
    }
}
