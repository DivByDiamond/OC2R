package li.cil.oc2.common.tablet.radio;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.radio.OraEndpoint;
import li.cil.oc2.common.blockentity.radio.RadioNetworkData;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.radio.RadioClientLeases;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The radio client of one tablet. Its endpoint id is the tablet id; its secret lives only in the
 * item and is rotated through {@link RadioClientLeases} on every activation, so a copy of the item
 * cannot run beside the original and an older copy never authenticates again. The identity is held
 * while a player holds the tablet in a level where it was enrolled and is suspended otherwise.
 */
// Error Prone 2.50.0's text-block style checker crashes on an empty source-token list here.
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName", "PMD.CompareObjectsWithEquals", "StringConcatToTextBlock"})
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "The radio exposes its live endpoint to the guest device adapter by design.")
// Physical stack, player and level identity distinguish originals from byte-identical copies.
public final class TabletRadio {
    private static final int RETRY_TICKS = 20;
    private static final String DEFAULT_SERVICE = "tablet";

    private final UUID id;
    private final OraEndpoint endpoint;
    private final Set<String> services = new LinkedHashSet<>();
    @Nullable private TabletHolder holder;
    private ServerLevel level;
    @Nullable private RadioNetworkData leaseData;
    private boolean active;
    private long retryAt;
    private String denied = "";

    public TabletRadio(final UUID id, final ServerLevel fallbackLevel) {
        this.id = id;
        this.level = fallbackLevel;
        endpoint = new OraEndpoint(() -> level, () -> id, this::x, this::z, () -> active);
        endpoint.identity(id, "");
    }

    /** The live endpoint; every call on it is authorized by the broker for the tablet's session. */
    public OraEndpoint endpoint() {
        return endpoint;
    }

    /** Checks the live item against server-owned enrollment; UUID alone grants no access. */
    public boolean accepts(final TabletHolder candidate) {
        if (!candidate.player().isAlive() || candidate.player().isRemoved()) return false;
        if (holder != null && holder.stack() != candidate.stack() && holderStillPresent()) return false;
        if (li.cil.oc2.common.item.tablet.TabletItem.getId(candidate.stack()).filter(id::equals).isEmpty()) {
            return false;
        }
        final String secret = candidate.stack().getOrDefault(DataComponents.TABLET_RADIO_SECRET.get(), "");
        boolean known = false;
        for (final ServerLevel dimension : candidate.level().getServer().getAllLevels()) {
            final var registry = RadioNetworkData.get(dimension).registry();
            known |= registry.knowsIdentity(id);
            if (registry.recognizesItemCredential(id, secret)) return true;
        }
        return !known && secret.isEmpty() && !active;
    }

    /** Whether the active physical item is still in either hand of its live owner. */
    public boolean holderStillPresent() {
        return holder != null && holder.player().isAlive() && !holder.player().isRemoved()
                && (holder.player().getMainHandItem() == holder.stack()
                        || holder.player().getOffhandItem() == holder.stack());
    }

    public boolean active() {
        return active;
    }

    /** Why the last activation was refused, empty if it was not. Never contains a secret. */
    public String denied() {
        return denied;
    }

    /**
     * Called once per server tick with whoever holds the tablet, or null. Suspends the identity when
     * the holder or the dimension changed and takes it again when the item carries a valid secret.
     */
    public void update(@Nullable final TabletHolder next, final long now) {
        if (next == null) {
            if (holder != null) release(reasonFor(holder.player()));
            holder = null;
            return;
        }
        if (!accepts(next)) {
            release(RadioClientLeases.Reason.TRANSFERRED);
            holder = null;
            denied = "Authentication failed";
            return;
        }
        if (holder != null && (holder.player() != next.player() || holder.stack() != next.stack())) {
            release(RadioClientLeases.Reason.TRANSFERRED);
        } else if (active && next.level() != level) {
            release(RadioClientLeases.Reason.DIMENSION_CHANGED);
        }
        holder = next;
        if (!active) {
            level = next.level();
            if (now >= retryAt) activateFromItem(now);
        } else {
            try {
                endpoint.tick();
            } catch (final SecurityException refused) {
                release(RadioClientLeases.Reason.TRANSFERRED);
                denied = "Authentication failed";
                retryAt = now + RETRY_TICKS;
            }
        }
    }

    /** Suspends the identity, keeping the secret in the item; also used when the machine is released. */
    public void release(final RadioClientLeases.Reason reason) {
        if (!active) return;
        active = false;
        if (leaseData != null) leaseData.leases().suspend(id, reason);
        leaseData = null;
        endpoint.identity(id, "");
    }

    /** Enrolls with an invitation the network owner approved, then takes the identity for this holder. */
    public void join(final String invite, final String secret) {
        if (holder == null) throw new IllegalStateException("Tablet is not held");
        final String previous = holder.stack().getOrDefault(DataComponents.TABLET_RADIO_SECRET.get(), "");
        release(RadioClientLeases.Reason.TRANSFERRED);
        level = holder.level();
        endpoint.joinRadioNetwork(invite, secret);
        try {
            claim(endpoint.credential());
        } catch (final SecurityException refused) {
            endpoint.identity(id, "");
            throw refused;
        }
        if (!previous.isEmpty()) {
            for (final ServerLevel dimension : level.getServer().getAllLevels()) {
                if (dimension == level) continue;
                final RadioNetworkData old = RadioNetworkData.get(dimension);
                if (old.registry().recognizesItemCredential(id, previous)) {
                    old.registry().retireItemCredential(id, previous);
                    old.setDirty();
                }
            }
        }
        register(DEFAULT_SERVICE);
    }

    /** Registers a service on the endpoint and remembers it for the next activation. */
    public void register(final String service) {
        endpoint.register(service);
        services.add(service);
    }

    public Collection<String> services() {
        return Set.copyOf(services);
    }

    public void restoreServices(final Collection<String> saved) {
        services.clear();
        services.addAll(saved);
    }

    /** The endpoint status plus whether the tablet holds its identity at the moment. */
    @SuppressWarnings("PMD.UseConcurrentHashMap") // The status map is local to this call.
    public Map<String, Object> status() {
        final Map<String, Object> result = new java.util.LinkedHashMap<>(endpoint.status());
        result.put("active", active);
        return Map.copyOf(result);
    }

    private void activateFromItem(final long now) {
        final String secret = holder.stack().getOrDefault(DataComponents.TABLET_RADIO_SECRET.get(), "");
        if (secret.isEmpty()) return;
        try {
            claim(secret);
            for (final String service : services) endpoint.register(service);
        } catch (final SecurityException refused) {
            denied = String.valueOf(refused.getMessage());
            retryAt = now + RETRY_TICKS;
        }
    }

    /** Takes the lease with the given secret and stores the rotated one in the item. */
    private void claim(final String secret) {
        final RadioNetworkData data = RadioNetworkData.get(level);
        final RadioClientLeases.Lease lease = data.leases().activate(id, secret, endpoint.dimension());
        leaseData = data;
        active = true;
        denied = "";
        endpoint.identity(id, lease.secret());
        holder.stack().set(DataComponents.TABLET_RADIO_SECRET.get(), lease.secret());
        data.setDirty();
    }

    private static RadioClientLeases.Reason reasonFor(final ServerPlayer player) {
        if (player.isRemoved()) return RadioClientLeases.Reason.LOGGED_OUT;
        return player.isAlive() ? RadioClientLeases.Reason.DROPPED : RadioClientLeases.Reason.DIED;
    }

    private double x() {
        return holder == null ? 0 : holder.player().getX();
    }

    private double z() {
        return holder == null ? 0 : holder.player().getZ();
    }
}
