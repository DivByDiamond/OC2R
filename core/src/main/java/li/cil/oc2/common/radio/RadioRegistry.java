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
    /** Pending approval requests allowed per network at one time. */
    public static final int MAX_PENDING_PER_NETWORK = 16;
    /** Fixed lifetime of a pending approval request (five minutes at 20 TPS). */
    public static final long PENDING_LIFETIME_TICKS = 6000;

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
            long expiresAtTick,
            boolean requiresApproval) {
        public Invite(final UUID id, final UUID network, final String dimension, final UUID device,
                      final Set<Scope> scopes, final String secretHash, final long expiresAtTick) {
            this(id, network, dimension, device, scopes, secretHash, expiresAtTick, false);
        }

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

    /**
     * A device that presented a valid approval-required invitation. It holds no membership and no
     * permission; credentialHash is the hash of the claim secret that becomes the device credential
     * only if an administrator approves.
     */
    public record EnrollmentRequest(
            UUID id,
            UUID network,
            String dimension,
            UUID device,
            Set<Scope> requestedScopes,
            String credentialHash,
            long expiresAtTick) {
        public EnrollmentRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(network);
            Objects.requireNonNull(device);
            requireDimension(dimension);
            requireHash(credentialHash);
            if (expiresAtTick < 0) throw new IllegalArgumentException("Invalid request expiry");
            requestedScopes = Set.copyOf(requestedScopes);
            if (requestedScopes.size() > MAX_SCOPES) throw new IllegalArgumentException("Too many scopes");
        }
    }

    public record PendingTicket(UUID request, String secret) {}

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
            List<Reservation> reservations,
            List<EnrollmentRequest> pending) {
        public State {
            networks = List.copyOf(networks);
            members = List.copyOf(members);
            invites = List.copyOf(invites);
            reservations = reservations == null ? List.of() : List.copyOf(reservations);
            pending = pending == null ? List.of() : List.copyOf(pending);
        }

        public State(
                final int nextPrefix,
                final List<Network> networks,
                final List<Member> members,
                final List<Invite> invites,
                final List<Reservation> reservations) {
            this(nextPrefix, networks, members, invites, reservations, List.of());
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
    private final Map<UUID, EnrollmentRequest> pending = new HashMap<>();
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
        return invite(network, adminSecret, dimension, device, scopes, now, lifetimeTicks, false);
    }

    /** With requiresApproval the invite can only open a pending request, never admit directly. */
    public Invitation invite(
            final UUID network, final String adminSecret, final String dimension,
            final UUID device, final Set<Scope> scopes, final long now, final long lifetimeTicks,
            final boolean requiresApproval) {
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
        invites.put(id, new Invite(id, network, dimension, device, scopes, hash(secret), expires,
                requiresApproval));
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
        if (invite.requiresApproval()) throw new SecurityException("Invitation requires approval");
        final String secret = secret();
        final Member member = admit(invite.device(), invite.network(), invite.dimension(),
                invite.scopes(), hash(secret));
        invites.remove(invitation);
        return new Enrollment(member, secret);
    }

    /** Shared admission: capacity, duplicate and address checks, then a fresh random address. */
    private Member admit(final UUID device, final UUID networkId, final String dimension,
                         final Set<Scope> scopes, final String credentialHash) {
        final Member previous = members.get(device);
        if (previous == null && members.size() >= MAX_MEMBERS)
            throw new IllegalStateException("Member registry capacity");
        if (previous != null && !previous.revoked())
            throw new SecurityException("Device already enrolled");
        final Network network = networks.get(networkId);
        if (reservations.size() >= MAX_RESERVATIONS)
            throw new IllegalStateException("Address reservation capacity");
        final int number = selectAvailable(network.nextDevice(), reservations.stream()
                .filter(value -> value.network().equals(network.id()))
                .map(Reservation::number)
                .toList());
        final Member member = new Member(device, network.id(), dimension, number,
                previous == null ? 1 : previous.generation() + 1, credentialHash, scopes, false);
        reservations.add(new Reservation(network.id(), number));
        members.put(member.id(), member);
        return member;
    }

    /**
     * Turns a valid approval-required invitation into a pending request. The invitation is consumed,
     * so one invitation yields at most one request. Returns a claim secret that becomes the device
     * credential on approval; it grants nothing before that.
     */
    public PendingTicket requestEnrollment(final UUID invitation, final String invitationSecret, final long now) {
        if (now < 0) throw new IllegalArgumentException("Invalid invitation clock");
        final Invite invite = invites.get(invitation);
        if (invite == null || !matches(invite.secretHash(), invitationSecret))
            throw new SecurityException("Invalid invitation");
        if (invite.expiresAtTick() <= now) throw new SecurityException("Invitation expired");
        if (!invite.requiresApproval()) throw new SecurityException("Invitation does not use approval");
        expireEnrollmentRequests(now);
        final Member previous = members.get(invite.device());
        if (previous != null && !previous.revoked()) throw new SecurityException("Device already enrolled");
        if (pending.values().stream().anyMatch(value -> value.device().equals(invite.device())))
            throw new IllegalStateException("Enrollment already pending");
        if (pending.values().stream().filter(value -> value.network().equals(invite.network())).count()
                >= MAX_PENDING_PER_NETWORK)
            throw new IllegalStateException("Pending enrollment capacity");
        final String secret = secret();
        final UUID id = UUID.randomUUID();
        final long expires = now > Long.MAX_VALUE - PENDING_LIFETIME_TICKS
                ? Long.MAX_VALUE : now + PENDING_LIFETIME_TICKS;
        pending.put(id, new EnrollmentRequest(id, invite.network(), invite.dimension(), invite.device(),
                invite.scopes(), hash(secret), expires));
        invites.remove(invitation);
        return new PendingTicket(id, secret);
    }

    /** Live pending requests of one network, oldest expiry first. Requires the network admin secret. */
    public List<EnrollmentRequest> pendingEnrollments(final UUID network, final String adminSecret, final long now) {
        requireAdmin(network, adminSecret);
        expireEnrollmentRequests(now);
        return pending.values().stream()
                .filter(value -> value.network().equals(network))
                .sorted(Comparator.comparingLong(EnrollmentRequest::expiresAtTick)
                        .thenComparing(EnrollmentRequest::id))
                .toList();
    }

    /** Admits the requester with exactly the approved scopes, which must be a subset of those requested. */
    public Member approveEnrollment(final UUID network, final String adminSecret, final UUID request,
                                    final Set<Scope> approved, final long now) {
        requireAdmin(network, adminSecret);
        final EnrollmentRequest value = livePending(network, request, now);
        if (!value.requestedScopes().containsAll(approved))
            throw new SecurityException("Approved scopes exceed requested scopes");
        final Member member = admit(value.device(), network, value.dimension(), approved, value.credentialHash());
        pending.remove(request);
        return member;
    }

    public void denyEnrollment(final UUID network, final String adminSecret, final UUID request, final long now) {
        requireAdmin(network, adminSecret);
        livePending(network, request, now);
        pending.remove(request);
    }

    public int expireEnrollmentRequests(final long now) {
        if (now < 0) throw new IllegalArgumentException("Invalid request clock");
        final int before = pending.size();
        pending.values().removeIf(value -> value.expiresAtTick() <= now);
        return before - pending.size();
    }

    private EnrollmentRequest livePending(final UUID network, final UUID request, final long now) {
        expireEnrollmentRequests(now);
        final EnrollmentRequest value = pending.get(request);
        if (value == null || !value.network().equals(network))
            throw new SecurityException("Unknown enrollment request");
        return value;
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
                List.copyOf(reservations),
                List.copyOf(pending.values()));
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
        final Map<UUID, Integer> perNetwork = new HashMap<>();
        final Set<UUID> pendingDevices = new HashSet<>();
        for (final EnrollmentRequest request : state.pending()) {
            if (!result.networks.containsKey(request.network())
                    || !pendingDevices.add(request.device())
                    || perNetwork.merge(request.network(), 1, Integer::sum) > MAX_PENDING_PER_NETWORK
                    || result.pending.putIfAbsent(request.id(), request) != null)
                throw new IllegalArgumentException("Invalid enrollment request record");
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
