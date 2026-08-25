package li.cil.oc2.common.inet.session.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.SessionBase;
import li.cil.oc2.common.inet.session.SessionDiscriminator;
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
     * Expires every session whose last update is older than the configured lifetime.
     * Expired sessions are removed from the registry, notified via {@code expire()},
     * and closed towards the peer. Because keys are ordered by time, this stops at
     * the first entry that is still young enough.
     */
    public void processSessionExpirationQueue() {
        if (expirationQueue.isEmpty()) {
            return;
        }
        final Instant expireTime =
                Instant.now().minus(Config.defaultSessionLifetimeMs, ChronoUnit.MILLIS);
        final Iterator<Instant> iterator = expirationQueue.navigableKeySet().iterator();
        while (iterator.hasNext()) {
            final Instant time = iterator.next();
            if (time.compareTo(expireTime) < 0) {
                final SessionBase session = expirationQueue.get(time);
                iterator.remove();
                removeRegistered(session);
                --allSessionCount;
                LOGGER.trace("Expired session {}", session.getDiscriminator());
                session.expire();
                sessionLayer.sendSession(session, null);
            } else {
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
        if (sessions.size() >= Config.defaultSessionsNumberPerCardLimit) {
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
                Instant.now().minus(Config.tcpRetransmissionTimeoutMs, ChronoUnit.MILLIS);
        for (final Instant time : expirationQueue.navigableKeySet()) {
            if (time.compareTo(retransmissionTime) < 0) {
                final SessionBase session = expirationQueue.get(time);
                if (session instanceof StreamSessionImpl stream && stream.isNeedsAcknowledgment()) {
                    return stream;
                }
            } else {
                break;
            }
        }
        return null;
    }
}