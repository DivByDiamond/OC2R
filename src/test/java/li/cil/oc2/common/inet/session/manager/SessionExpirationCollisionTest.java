package li.cil.oc2.common.inet.session.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.common.inet.session.SessionBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Regression tests for expiration-queue bookkeeping: two sessions updated at the identical
 * {@code Instant} must not orphan each other (a plain {@code TreeMap.put} silently evicted
 * the earlier entry while it stayed registered, leaking session counts until networking
 * died entirely — observed with chatty protocols like DHCP).
 */
final class SessionExpirationCollisionTest {
    private SessionManager manager;

    /** Minimal concrete session: the base class is abstract but carries all bookkeeping. */
    private static final class FakeSession extends SessionBase {
        FakeSession(final short port) {
            super(0x01010101, port);
        }

        @Nullable
        @Override
        public InetSocketAddress getDestination() {
            return null;
        }

        @Override
        public void close() {}

        @Override
        public void expire() {
            close();
        }

        @Override
        public States getState() {
            return States.ESTABLISHED;
        }

        @Nullable
        @Override
        public li.cil.oc2.common.inet.session.SessionDiscriminator<?> getDiscriminator() {
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        manager = new SessionManager(Mockito.mock(SessionLayer.class));
    }

    @Test
    void sameTimestampUpdatesKeepBothSessionsExpirable() {
        final FakeSession first = new FakeSession((short) 80);
        final FakeSession second = new FakeSession((short) 443);

        manager.updateSession(first);
        // Simulate Instant.now() returning the same value for both updates: enqueue
        // the second session directly under the first one's timestamp.
        second.setLastUpdateTime(first.getLastUpdateTime());
        manager.enqueue(second);

        assertEquals(
                2,
                manager.getQueueSize(),
                "both sessions must remain individually expirable");
    }

    @Test
    void repeatedUpdatesDoNotAccumulateQueueEntries() {
        final FakeSession session = new FakeSession((short) 80);
        manager.updateSession(session);
        manager.updateSession(session);
        manager.updateSession(session);

        assertEquals(1, manager.getQueueSize());
    }

    @Test
    void closedSessionLeavesTheQueue() {
        final FakeSession session = new FakeSession((short) 80);
        manager.updateSession(session);
        assertEquals(1, manager.getQueueSize());

        manager.closeSession(session);

        assertEquals(0, manager.getQueueSize());
    }
}
