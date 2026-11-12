package li.cil.oc2.common.inet.session.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.SessionBase;
import li.cil.oc2.common.inet.session.SessionDiscriminator;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionBase;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionDiscriminator;
import li.cil.oc2.common.inet.session.echo.EchoSessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Per-card session registry for one transport layer. Sessions are indexed by their
 * discriminator for lookup, and by last-update time in an ordered expiration queue:
 * sessions idle for longer than the configured lifetime are expired and closed.
 * Creation is capped both per card and globally across all cards.
 */
public final class SessionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int allSessionCount = 0;

    // Coarse shared clock: both per-packet scans below reuse one cached instant instead
    // of each fetching their own, refreshed at most once per interval. Expiration
    // thresholds are seconds-scale, so microsecond staleness is irrelevant; everything
    // here runs on the single "Internet" thread (docs/roadmap/done.md §38 Sh8).
    private static final long CLOCK_RESOLUTION_NANOS = 200_000;
    private long clockNanos;
    private Instant clock = Instant.now();

    private Instant now() {
        final long nanos = System.nanoTime();
        if (nanos - clockNanos >= CLOCK_RESOLUTION_NANOS) {
            clockNanos = nanos;
            clock = Instant.now();
        }
        return clock;
    }

    private final SessionLayer sessionLayer;
    /**
     * Sessions ordered by last-update time. The head of the map is the oldest entry,
     * so expiration and retransmission scans only need to walk from the front until
     * the first entry younger than the respective cutoff.
     */
    private final NavigableMap<Instant, SessionBase> expirationQueue = new TreeMap<>();
    private final Map<SessionDiscriminator<?>, SessionBase> sessions = new ConcurrentHashMap<>();

    public SessionManager(final SessionLayer sessionLayer) {
        this.sessionLayer = sessionLayer;
    }

    public Map<SessionDiscriminator<?>, SessionBase> getSessions() {
        return sessions;
    }

    /** Number of sessions tracked for expiration; exposed for tests. */
    int getQueueSize() {
        return expirationQueue.size();
    }

    /**
     * How long a session may sit idle before it is expired. A UDP exchange (a DNS lookup takes a new source port
     * every time) lives briefly, an established TCP connection very long, and anything still opening or closing
     * gets the default.
     */
    private static long lifetimeMs(final SessionBase session) {
        if (session instanceof DatagramSessionBase) {
            return Config.datagramSessionLifetimeMs;
        }
        if (session instanceof final StreamSessionImpl stream
                && stream.getState() == Session.States.ESTABLISHED && !stream.isClosed()) {
            return Config.tcpEstablishedLifetimeMs;
        }
        return Config.defaultSessionLifetimeMs;
    }

    /**
     * Expires every session that has been idle for longer than its own lifetime, see {@link #lifetimeMs}.
     * Expired sessions are removed from the registry, notified via {@code expire()}, and closed towards the
     * peer. The queue is ordered by last update, so the walk stops at the first entry that is younger than the
     * shortest lifetime in use; older entries with a longer lifetime are skipped.
     */
    public void processSessionExpirationQueue() {
        if (expirationQueue.isEmpty()) {
            return;
        }
        final Instant current = now();
        final long shortest = Math.min(Config.defaultSessionLifetimeMs, Config.datagramSessionLifetimeMs);
        final Instant youngestToCheck = current.minus(shortest, ChronoUnit.MILLIS);
        final Iterator<Map.Entry<Instant, SessionBase>> iterator =
                expirationQueue.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Instant, SessionBase> entry = iterator.next();
            if (entry.getKey().compareTo(youngestToCheck) >= 0) {
                return;
            }
            final SessionBase session = entry.getValue();
            if (entry.getKey().compareTo(current.minus(lifetimeMs(session), ChronoUnit.MILLIS)) < 0) {
                iterator.remove();
                expire(session);
            }
        }
    }

    /** Drops a session that is already out of the expiration queue and tells its layer. */
    private void expire(final SessionBase session) {
        removeRegistered(session);
        --allSessionCount;
        LOGGER.trace("Expired session {}", session.getDiscriminator());
        session.expire();
        sessionLayer.sendSession(session, null);
    }

    /** Makes room for a new UDP exchange by expiring the oldest one, like a NAT table that is full. */
    private void evictOldestDatagramSession() {
        final Iterator<Map.Entry<Instant, SessionBase>> iterator =
                expirationQueue.entrySet().iterator();
        while (iterator.hasNext()) {
            final SessionBase session = iterator.next().getValue();
            if (session instanceof DatagramSessionBase) {
                iterator.remove();
                expire(session);
                return;
            }
        }
    }

    /**
     * Refreshes a session's position in the expiration queue after activity, so its
     * lifetime restarts from now.
     */
    public void updateSession(final SessionBase session) {
        expirationQueue.remove(session.getLastUpdateTime());
        session.update();
        enqueue(session);
    }

    /**
     * Inserts an updated session under a unique expiration-queue key.
     *
     * <p>{@code Instant.now()} has finite resolution and can hand the same timestamp to
     * two different sessions. A plain put would silently evict the earlier session from
     * this queue while leaving it registered in {@code sessions}: never expired again,
     * its slot never released. Enough such leaks exhaust the session limits and all
     * networking dies (observed as "network gets slower until it stops" with chatty
     * protocols like DHCP).
     */
    void enqueue(final SessionBase session) {
        Instant key = session.getLastUpdateTime();
        while (expirationQueue.containsKey(key)) {
            key = key.plusNanos(1);
        }
        session.setLastUpdateTime(key);
        final SessionBase previous = expirationQueue.put(key, session);
        assert previous == null;
    }

    public void closeSession(final SessionBase session) {
        LOGGER.trace("Close session {}", session.getDiscriminator());
        removeRegistered(session);
        expirationQueue.remove(session.getLastUpdateTime());
        --allSessionCount;
    }

    private void removeRegistered(final SessionBase session) {
        // Discriminator may be absent on synthetic/test sessions.
        final SessionDiscriminator<?> discriminator = session.getDiscriminator();
        if (discriminator != null) {
            sessions.remove(discriminator);
        }
    }

    /**
     * Returns the session matching the discriminator, creating it via the factory if
     * absent. Creation fails (returns {@code null}) when either the per-card or the
     * global session limit has been reached.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <S extends SessionBase, D extends SessionDiscriminator<S>> S getOrCreateSession(
            final D discriminator, final Function<D, S> factory) {
        final S session = (S) sessions.get(discriminator);
        if (session != null) {
            return session;
        }
        final boolean datagram = isDatagram(discriminator);
        if (datagram) {
            while (count(true) >= Config.datagramSessionsNumberPerCardLimit) {
                evictOldestDatagramSession();
            }
        } else if (count(false) >= Config.defaultSessionsNumberPerCardLimit) {
            LOGGER.warn("Session count per card limit has reached");
            return null;
        }
        if (allSessionCount >= Config.defaultSessionsNumberLimit) {
            LOGGER.warn("Session count limit has reached");
            return null;
        }
        ++allSessionCount;
        LOGGER.trace("New session: {}", discriminator);
        final S newSession = factory.apply(discriminator);
        sessions.put(discriminator, newSession);
        updateSession(newSession);
        return newSession;
    }

    private static boolean isDatagram(final SessionDiscriminator<?> discriminator) {
        return discriminator instanceof DatagramSessionDiscriminator
                || discriminator instanceof EchoSessionDiscriminator;
    }

    /** Number of registered sessions of one kind: UDP and ping exchanges, or connections. */
    private int count(final boolean datagram) {
        int result = 0;
        for (final SessionDiscriminator<?> key : sessions.keySet()) {
            if (isDatagram(key) == datagram) {
                ++result;
            }
        }
        return result;
    }

    /**
     * Scans the expiration queue for the oldest stream session that has unacknowledged
     * data older than the TCP retransmission timeout, for the caller to retransmit.
     */
    @Nullable
    StreamSessionImpl getNextStreamForRetransmission() {
        if (expirationQueue.isEmpty()) {
            return null;
        }
        final Instant retransmissionTime =
                now().minus(Config.tcpRetransmissionTimeoutMs, ChronoUnit.MILLIS);
        for (final Map.Entry<Instant, SessionBase> entry : expirationQueue.entrySet()) {
            if (entry.getKey().compareTo(retransmissionTime) >= 0) {
                break;
            }
            final SessionBase session = entry.getValue();
            if (session instanceof StreamSessionImpl stream && stream.isNeedsAcknowledgment()) {
                return stream;
            }
        }
        return null;
    }
}