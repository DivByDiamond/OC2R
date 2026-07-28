package li.cil.oc2.common.inet.session.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.SessionBase;
import li.cil.oc2.common.inet.session.SessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SessionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int allSessionCount = 0;

    private final SessionLayer sessionLayer;
    private final NavigableMap<Instant, SessionBase> expirationQueue = new TreeMap<>();
    private final Map<SessionDiscriminator<?>, SessionBase> sessions = new HashMap<>();

    public SessionManager(final SessionLayer sessionLayer) {
        this.sessionLayer = sessionLayer;
    }

    public Map<SessionDiscriminator<?>, SessionBase> getSessions() {
        return sessions;
    }

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
                sessions.remove(session.getDiscriminator());
                --allSessionCount;
                LOGGER.trace("Expired session {}", session.getDiscriminator());
                session.expire();
                sessionLayer.sendSession(session, null);
            } else {
                return;
            }
        }
    }

    public void updateSession(final SessionBase session) {
        final Instant oldKey = session.getLastUpdateTime();
        expirationQueue.remove(oldKey);
        session.update();
        final Instant newLastUpdateTime = session.getLastUpdateTime();
        final SessionBase previous = expirationQueue.put(newLastUpdateTime, session);
        assert previous == null;
    }

    public void closeSession(final SessionBase session) {
        LOGGER.trace("Close session {}", session.getDiscriminator());
        sessions.remove(session.getDiscriminator());
        expirationQueue.remove(session.getLastUpdateTime());
        --allSessionCount;
    }

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