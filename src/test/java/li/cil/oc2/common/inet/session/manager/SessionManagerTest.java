package li.cil.oc2.common.inet.session.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.SessionBase;
import li.cil.oc2.common.inet.session.echo.EchoSessionDiscriminator;
import li.cil.oc2.common.inet.session.echo.EchoSessionImpl;
import li.cil.oc2.common.inet.session.stream.StreamSessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "The tests set the process-wide configuration and put it back afterwards.")
final class SessionManagerTest {
    private static final int SRC = 0x0A000201;
    private static final int DST = 0x01010101;
    private static final short PORT = 7;

    private SessionLayer sessionLayer;
    private SessionManager manager;
    private int savedLifetime;
    private int savedDatagramLifetime;
    private int savedPerCardLimit;
    private int savedGlobalLimit;

    @BeforeEach
    void setUp() {
        sessionLayer = mock(SessionLayer.class);
        manager = new SessionManager(sessionLayer);
        savedLifetime = Config.defaultSessionLifetimeMs;
        savedDatagramLifetime = Config.datagramSessionLifetimeMs;
        savedPerCardLimit = Config.defaultSessionsNumberPerCardLimit;
        // The total of all cards is a static counter that other tests leave behind; these tests are about one card.
        savedGlobalLimit = Config.defaultSessionsNumberLimit;
        Config.defaultSessionsNumberLimit = Integer.MAX_VALUE;
    }

    @AfterEach
    void tearDown() {
        // The total over all cards is a static counter; give back what this test took.
        new java.util.ArrayList<>(manager.getSessions().values()).forEach(manager::closeSession);
        Config.defaultSessionLifetimeMs = savedLifetime;
        Config.datagramSessionLifetimeMs = savedDatagramLifetime;
        Config.defaultSessionsNumberPerCardLimit = savedPerCardLimit;
        Config.defaultSessionsNumberLimit = savedGlobalLimit;
    }

    private static EchoSessionDiscriminator discriminator(final int identity) {
        return new EchoSessionDiscriminator(SRC, DST, (short) identity);
    }

    private EchoSessionImpl createSession(final int identity) {
        return manager.getOrCreateSession(
                discriminator(identity), it -> new EchoSessionImpl(DST, PORT, it));
    }

    @Test
    void getOrCreateSessionReusesSessionForEqualDiscriminator() {
        final EchoSessionImpl first = createSession(1);
        final EchoSessionImpl second = createSession(1);
        assertSame(first, second);
        assertEquals(1, manager.getSessions().size());
    }

    @Test
    void getOrCreateSessionCreatesSeparateSessionsPerIdentity() {
        final EchoSessionImpl first = createSession(1);
        final EchoSessionImpl second = createSession(2);
        assertNotEquals(first, second);
        assertEquals(2, manager.getSessions().size());
    }

    @Test
    void closeSessionRemovesItFromRegistry() {
        final EchoSessionImpl session = createSession(1);
        manager.closeSession(session);
        assertEquals(0, manager.getSessions().size());
        final EchoSessionImpl recreated = createSession(1);
        assertNotEquals(session, recreated);
    }

    private StreamSessionImpl createStream(final int sourcePort) {
        return manager.getOrCreateSession(
                new StreamSessionDiscriminator(SRC, (short) sourcePort, DST, PORT),
                it -> new StreamSessionImpl(DST, PORT, it));
    }

    @Test
    void perCardLimitRejectsNewConnections() {
        Config.defaultSessionsNumberPerCardLimit = 1;
        assertSame(createStream(40000), createStream(40000));
        assertNull(createStream(40001));
        assertEquals(1, manager.getSessions().size());
    }

    @Test
    void expiredSessionsAreRemovedAndNotified() {
        Config.datagramSessionLifetimeMs = -1000; // an echo exchange expires immediately
        final EchoSessionImpl session = createSession(1);
        manager.processSessionExpirationQueue();
        assertEquals(0, manager.getSessions().size());
        final ArgumentCaptor<SessionBase> captor = ArgumentCaptor.forClass(SessionBase.class);
        verify(sessionLayer).sendSession(captor.capture(), isNull());
        assertEquals(session.getDiscriminator(), captor.getValue().getDiscriminator());
    }

    @Test
    void expirationQueueProcessingWithoutSessionsIsNoOp() {
        manager.processSessionExpirationQueue();
        verify(sessionLayer, never()).sendSession(any(), any());
    }
}
