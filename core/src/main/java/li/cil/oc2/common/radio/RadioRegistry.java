package li.cil.oc2.common.radio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/** Server-owned enrollment registry. Exported records contain hashes only, never bearer secrets. */
public final class RadioRegistry {
    public static final int MAX_NETWORKS = 256;
    public static final int MAX_MEMBERS = 4096;
    public static final int MAX_INVITES = 1024;
    public static final int MAX_SCOPES = 64;
    public static final int MAX_RESERVATIONS = 65536;
    public static final long DEFAULT_INVITE_LIFETIME_TICKS = 1200;

    public enum Permission {
        MESSAGE_SEND,
        TELEMETRY_READ,
        COMMAND_SEND,
        NETWORK_ADMIN
    }

    public record Scope(Permission permission, UUID device, String service) {
        public Scope {
            Objects.requireNonNull(permission);
            if (service != null && (service.isEmpty() || service.length() > 128))
                throw new IllegalArgumentException("Invalid service scope");
        }
    }

    /** nextDevice is the legacy allocation floor; all smaller numbers remain reserved. */
    public record Network(UUID id, int prefix, String adminHash, int nextDevice) {
        public Network {
            Objects.requireNonNull(id);
            requireHash(adminHash);
        }
    }

    public record Member(
            UUID id,
            UUID network,
            String dimension,
            int number,
            long generation,
            String credentialHash,
            Set<Scope> scopes,
            boolean revoked) {
        public Member {
            Objects.requireNonNull(id);
            Objects.requireNonNull(network);
            requireDimension(dimension);
            requireHash(credentialHash);
            scopes = Set.copyOf(scopes);
            if (scopes.size() > MAX_SCOPES) throw new IllegalArgumentException("Too many scopes");
        }
    }

    public record Invite(
            UUID id,
            UUID network,
            String dimension,
            UUID device,
            Set<Scope> scopes,
            String secretHash,
            long expiresAtTick) {
        public Invite {
            Objects.requireNonNull(id);
            Objects.requireNonNull(network);
            Objects.requireNonNull(device);
            requireDimension(dimension);
            requireHash(secretHash);
            if (expiresAtTick < 0) throw new IllegalArgumentException("Invalid invitation expiry");
            scopes = Set.copyOf(scopes);
            if (scopes.size() > MAX_SCOPES) throw new IllegalArgumentException("Too many scopes");
        }
    }

    /** A permanent reservation, including addresses retired by revocation or reenrollment. */
    public record Reservation(UUID network, int number) {
        public Reservation {
            Objects.requireNonNull(network);
        }
    }

    public record State(
            int nextPrefix,
            List<Network> networks,
            List<Member> members,
            List<Invite> invites,
            List<Reservation> reservations) {
        public State {
            networks = List.copyOf(networks);
            members = List.copyOf(members);
            invites = List.copyOf(invites);
            reservations = reservations == null ? List.of() : List.copyOf(reservations);
        }

        public State(
                final int nextPrefix,
                final List<Network> networks,
                final List<Member> members,
                final List<Invite> invites) {
            this(nextPrefix, networks, members, invites, List.of());
        }
    }

    public record CreatedNetwork(UUID network, String adminSecret) {}

    public record Invitation(UUID invite, String secret) {}

    public record Enrollment(Member member, String secret) {}

    private final Map<UUID, Network> networks = new HashMap<>();
    private final Map<UUID, Member> members = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final SecureRandom addressRandom;
    private final Set<Reservation> reservations = new HashSet<>();
    private int nextPrefix = 1;

    public RadioRegistry() {
        this(new SecureRandom());
    }

    RadioRegistry(final SecureRandom addressRandom) {
        this.addressRandom = Objects.requireNonNull(addressRandom);
    }

    public CreatedNetwork createNetwork() {
        if (networks.size() >= MAX_NETWORKS)
            throw new IllegalStateException("Network registry capacity");
        final int prefix =
                selectAvailable(1, networks.values().stream().map(Network::prefix).toList());
        final UUID id = UUID.randomUUID();
        final String secret = secret();
        networks.put(id, new Network(id, prefix, hash(secret), 1));
        nextPrefix = Math.max(nextPrefix, prefix + 1);
        return new CreatedNetwork(id, secret);
    }

    public Invitation invite(
            final UUID network,
            final String adminSecret,
            final String dimension,
            final UUID device,
            final Set<Scope> scopes) {
        return invite(network, adminSecret, dimension, device, scopes, 0, DEFAULT_INVITE_LIFETIME_TICKS);
    }

    public Invitation invite(
            final UUID network, final String adminSecret, final String dimension,
            final UUID device, final Set<Scope> scopes, final long now, final long lifetimeTicks) {
        requireAdmin(network, adminSecret);
        if (now < 0 || lifetimeTicks < 1 || lifetimeTicks > 72000)
            throw new IllegalArgumentException("Invalid invitation lifetime");
        expireInvites(now);
        if (invites.size() >= MAX_INVITES) throw new IllegalStateException("Invitation capacity");
        Objects.requireNonNull(dimension);
        Objects.requireNonNull(device);
        final UUID id = UUID.randomUUID();
        final String secret = secret();
        final long expires = now > Long.MAX_VALUE - lifetimeTicks ? Long.MAX_VALUE : now + lifetimeTicks;
        invites.put(id, new Invite(id, network, dimension, device, scopes, hash(secret), expires));
        return new Invitation(id, secret);
    }

    public Enrollment enroll(final UUID invitation, final String invitationSecret) {
        return enroll(invitation, invitationSecret, 0);
    }

    public Enrollment enroll(final UUID invitation, final String invitationSecret, final long now) {
        if (now < 0) throw new IllegalArgumentException("Invalid invitation clock");
        final Invite invite = invites.get(invitation);
        if (invite == null || !matches(invite.secretHash(), invitationSecret))
            throw new SecurityException("Invalid invitation");
        if (invite.expiresAtTick() <= now) throw new SecurityException("Invitation expired");
        final Member previous = members.get(invite.device());
        if (previous == null && members.size() >= MAX_MEMBERS)
            throw new IllegalStateException("Member registry capacity");
        if (previous != null && !previous.revoked())
            throw new SecurityException("Device already enrolled");
        final Network network = networks.get(invite.network());
        if (reservations.size() >= MAX_RESERVATIONS)
            throw new IllegalStateException("Address reservation capacity");
        final int number =
                selectAvailable(
                        network.nextDevice(),
                        reservations.stream()
                                .filter(value -> value.network().equals(network.id()))
                                .map(Reservation::number)
                                .toList());
        final String secret = secret();
        final Member member =
                new Member(
                        invite.device(),
                        network.id(),
                        invite.dimension(),
                        number,
                        previous == null ? 1 : previous.generation() + 1,
                        hash(secret),
                        invite.scopes(),
                        false);
        reservations.add(new Reservation(network.id(), number));
        members.put(member.id(), member);
        invites.remove(invitation);
        return new Enrollment(member, secret);
    }

    public Optional<Member> authenticate(final UUID device, final String secret) {
        final Member member = members.get(device);
        return member != null && !member.revoked() && matches(member.credentialHash(), secret)
                ? Optional.of(member)
                : Optional.empty();
    }

    public boolean authorized(
            final UUID device,
            final String secret,
            final Permission permission,
            final UUID target,
            final String service) {
        return authenticate(device, secret)
                .filter(
                        member ->
                                member.scopes().stream()
                                        .anyMatch(
                                                scope ->
                                                        scope.permission() == permission
                                                                && (scope.device() == null
                                                                        || scope.device()
                                                                                .equals(target))
                                                                && (scope.service() == null
                                                                        || scope.service()
                                                                                .equals(service))))
                .isPresent();
    }

    public int expireInvites(final long now) {
        if (now < 0) throw new IllegalArgumentException("Invalid invitation clock");
        final int before = invites.size();
        invites.values().removeIf(invite -> invite.expiresAtTick() <= now);
        return before - invites.size();
    }

    public void setScopes(final UUID network, final String adminSecret, final UUID device,
                          final Set<Scope> scopes) {
        requireAdmin(network, adminSecret);
        final Member member = member(device).orElseThrow(() -> new SecurityException("Unknown device"));
        if (!member.network().equals(network)) throw new SecurityException("Foreign device");
        if (member.scopes().equals(scopes)) return;
        if (member.generation() == Long.MAX_VALUE) throw new IllegalStateException("Generation exhausted");
        members.put(device, new Member(member.id(), member.network(), member.dimension(), member.number(),
                member.generation() + 1, member.credentialHash(), scopes, false));
    }

    public void revoke(final UUID network, final String adminSecret, final UUID device) {
        requireAdmin(network, adminSecret);
        final Member member = members.get(device);
        if (member == null || !member.network().equals(network))
            throw new SecurityException("Foreign device");
        members.put(
                device,
                new Member(
                        member.id(),
                        network,
                        member.dimension(),
                        member.number(),
                        member.generation(),
                        member.credentialHash(),
                        member.scopes(),
                        true));
    }

    /** Live members of the caller's own network and dimension, ordered by device number. */
    public List<Member> peers(final UUID device, final String secret) {
        final Member caller = authenticate(device, secret).orElseThrow(SecurityException::new);
        return members.values().stream()
                .filter(
                        member ->
                                !member.revoked()
                                        && member.network().equals(caller.network())
                                        && member.dimension().equals(caller.dimension()))
                .sorted(Comparator.comparingInt(Member::number))
                .toList();
    }

    public Optional<Member> member(final UUID device) {
        return Optional.ofNullable(members.get(device)).filter(member -> !member.revoked());
    }

    public RadioAddress address(final UUID device) {
        final Member member =
                member(device).orElseThrow(() -> new IllegalArgumentException("Unknown device"));
        return new RadioAddress(networks.get(member.network()).prefix(), member.number());
    }

    public Optional<Member> resolve(
            final UUID authenticatedDevice, final String secret, final RadioAddress address) {
        final Optional<Member> caller = authenticate(authenticatedDevice, secret);
        if (caller.isEmpty() || networks.get(caller.get().network()).prefix() != address.network())
            return Optional.empty();
        return members.values().stream()
                .filter(
                        member ->
                                !member.revoked()
                                        && member.network().equals(caller.get().network())
                                        && member.dimension().equals(caller.get().dimension())
                                        && member.number() == address.device())
                .findFirst();
    }

    public State exportState() {
        return new State(
                nextPrefix,
                List.copyOf(networks.values()),
                List.copyOf(members.values()),
                List.copyOf(invites.values()),
                List.copyOf(reservations));
    }

    public static RadioRegistry importState(final State state) {
        return importState(state, new SecureRandom());
    }

    static RadioRegistry importState(final State state, final SecureRandom addressRandom) {
        if (state.networks().size() > MAX_NETWORKS
                || state.members().size() > MAX_MEMBERS
                || state.invites().size() > MAX_INVITES
                || state.reservations().size() > MAX_RESERVATIONS)
            throw new IllegalArgumentException("Registry capacity exceeded");
        final RadioRegistry result = new RadioRegistry(addressRandom);
        final Set<Integer> prefixes = new HashSet<>();
        for (final Network network : state.networks()) {
            if (network.prefix() < 1
                    || network.prefix() > RadioAddress.MAX
                    || network.nextDevice() < 1
                    || network.nextDevice() > RadioAddress.MAX + 1
                    || !prefixes.add(network.prefix())
                    || result.networks.putIfAbsent(network.id(), network) != null)
                throw new IllegalArgumentException("Invalid network record");
        }
        final Set<String> addresses = new HashSet<>();
        for (final Member member : state.members()) {
            final Network network = result.networks.get(member.network());
            if (network == null
                    || member.number() < 1
                    || member.number() > RadioAddress.MAX
                    || member.generation() < 1
                    || !addresses.add(member.network() + ":" + member.number())
                    || result.members.putIfAbsent(member.id(), member) != null)
                throw new IllegalArgumentException("Invalid member record");
        }
        for (final Reservation reservation : state.reservations()) {
            if (!result.networks.containsKey(reservation.network())
                    || reservation.number() < 1
                    || reservation.number() > RadioAddress.MAX
                    || !result.reservations.add(reservation))
                throw new IllegalArgumentException("Invalid address reservation");
        }
        for (final Member member : state.members()) {
            if (member.number() >= result.networks.get(member.network()).nextDevice())
                result.reservations.add(new Reservation(member.network(), member.number()));
        }
        if (result.reservations.size() > MAX_RESERVATIONS)
            throw new IllegalArgumentException("Address reservation capacity exceeded");
        for (final Invite invite : state.invites()) {
            if (!result.networks.containsKey(invite.network())
                    || result.invites.putIfAbsent(invite.id(), invite) != null)
                throw new IllegalArgumentException("Invalid invitation record");
        }
        if (state.nextPrefix() < 1
                || state.nextPrefix() > RadioAddress.MAX + 1
                || prefixes.stream().anyMatch(prefix -> prefix >= state.nextPrefix()))
            throw new IllegalArgumentException("Invalid address allocator");
        result.nextPrefix = state.nextPrefix();
        return result;
    }

    /** Selects a uniform rank among free addresses, then skips reserved intervals. */
    private int selectAvailable(final int floor, final Collection<Integer> used) {
        final List<Integer> sorted =
                used.stream().filter(number -> number >= floor).distinct().sorted().toList();
        final int available = RadioAddress.MAX - floor + 1 - sorted.size();
        if (available <= 0) throw new IllegalStateException("Address exhaustion");
        int candidate = floor + addressRandom.nextInt(available);
        for (final int number : sorted) {
            if (number > candidate) break;
            candidate++;
        }
        return candidate;
    }

    private void requireAdmin(final UUID network, final String secret) {
        final Network value = networks.get(network);
        if (value == null || !matches(value.adminHash(), secret))
            throw new SecurityException("Invalid network administrator");
    }

    private static void requireDimension(final String dimension) {
        if (dimension == null || dimension.isEmpty() || dimension.length() > 256)
            throw new IllegalArgumentException("Invalid dimension");
    }

    private static void requireHash(final String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid credential hash");
    }

    private String secret() {
        final byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(final String secret) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean matches(final String expected, final String actual) {
        return actual != null
                && actual.length() == 43
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.US_ASCII),
                        hash(actual).getBytes(StandardCharsets.US_ASCII));
    }
}
