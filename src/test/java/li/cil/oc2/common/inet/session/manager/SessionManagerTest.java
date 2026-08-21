package li.cil.oc2.common.inet.session.manager;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class SessionManagerTest {
    private static final int SRC = 0x0A000201;
    private static final int DST = 0x01010101;
    private static final short PORT = 7;

    private SessionLayer sessionLayer;
    private SessionManager manager;
    private int savedLifetime;
    private int savedPerCardLimit;

    @BeforeEach
    void setUp() {
        sessionLayer = mock(SessionLayer.class);
        manager = new SessionManager(sessionLayer);
        savedLifetime = Config.defaultSessionLifetimeMs;
        savedPerCardLimit = Config.defaultSessionsNumberPerCardLimit;
    }

    @AfterEach
    void tearDown() {
        Config.defaultSessionLifetimeMs = savedLifetime;
        Config.defaultSessionsNumberPerCardLimit = savedPerCardLimit;
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

    @Test
    void perCardLimitRejectsNewSessions() {
        Config.defaultSessionsNumberPerCardLimit = 1;
        assertSame(createSession(1), createSession(1));
        assertNull(createSession(2));
        assertEquals(1, manager.getSessions().size());
    }

    @Test
    void expiredSessionsAreRemovedAndNotified() {
        Config.defaultSessionLifetimeMs = -1000; // everything expires immediately
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
