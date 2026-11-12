package li.cil.oc2.common.inet.session.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionDiscriminator;
import li.cil.oc2.common.inet.session.datagram.DatagramSessionImpl;
import li.cil.oc2.common.inet.session.stream.StreamSessionDiscriminator;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What kept the internet card from working for real use: a DNS lookup takes a new source port every time and
 * used to occupy one of ten slots for a minute, and a quiet SSH connection was dropped after that same minute.
 */
@SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "The tests set the process-wide configuration and put it back afterwards.")
final class SessionLifetimeTest {
    private static final int SRC = 0x0A000201;
    private static final int DST = 0x01010101;
    private static final long LONG = 3_600_000;

    private SessionLayer sessionLayer;
    private SessionManager manager;
    private int savedDefault;
    private int savedTcp;
    private int savedDatagram;
    private int savedPerCard;
    private int savedGlobalLimit;
    private int savedDatagramPerCard;

    @BeforeEach
    void setUp() {
        sessionLayer = mock(SessionLayer.class);
        manager = new SessionManager(sessionLayer);
        savedDefault = Config.defaultSessionLifetimeMs;
        savedTcp = Config.tcpEstablishedLifetimeMs;
        savedDatagram = Config.datagramSessionLifetimeMs;
        savedPerCard = Config.defaultSessionsNumberPerCardLimit;
        // The total of all cards is a static counter that other tests leave behind; these tests are about one card.
        savedGlobalLimit = Config.defaultSessionsNumberLimit;
        Config.defaultSessionsNumberLimit = Integer.MAX_VALUE;
        savedDatagramPerCard = Config.datagramSessionsNumberPerCardLimit;
        Config.defaultSessionLifetimeMs = (int) LONG;
        Config.tcpEstablishedLifetimeMs = (int) LONG;
        Config.datagramSessionLifetimeMs = (int) LONG;
    }

    @AfterEach
    void tearDown() {
        // The total over all cards is a static counter; give back what this test took.
        new java.util.ArrayList<>(manager.getSessions().values()).forEach(manager::closeSession);
        Config.defaultSessionLifetimeMs = savedDefault;
        Config.tcpEstablishedLifetimeMs = savedTcp;
        Config.datagramSessionLifetimeMs = savedDatagram;
        Config.defaultSessionsNumberPerCardLimit = savedPerCard;
        Config.defaultSessionsNumberLimit = savedGlobalLimit;
        Config.datagramSessionsNumberPerCardLimit = savedDatagramPerCard;
    }

    private StreamSessionImpl stream(final int sourcePort) {
        return manager.getOrCreateSession(
                new StreamSessionDiscriminator(SRC, (short) sourcePort, DST, (short) 22),
                it -> new StreamSessionImpl(DST, (short) 22, it));
    }

    private DatagramSessionImpl datagram(final int sourcePort) {
        return manager.getOrCreateSession(
                new DatagramSessionDiscriminator(SRC, (short) sourcePort, DST, (short) 53),
                it -> new DatagramSessionImpl(DST, (short) 53, it));
    }

    @Test
    void anEstablishedConnectionOutlivesTheDefaultLifetime() {
        Config.defaultSessionLifetimeMs = -1000; // anything on the default lifetime expires at once
        final StreamSessionImpl session = stream(40000);
        session.state = TcpStates.ESTABLISHED;
        manager.processSessionExpirationQueue();
        assertSame(session, stream(40000));
        verify(sessionLayer, never()).sendSession(session, null);

        Config.tcpEstablishedLifetimeMs = -1000;
        manager.processSessionExpirationQueue();
        assertEquals(0, manager.getSessions().size());
        verify(sessionLayer).sendSession(session, null);
    }

    @Test
    void aConnectionThatIsStillOpeningUsesTheDefaultLifetime() {
        Config.defaultSessionLifetimeMs = -1000;
        final StreamSessionImpl session = stream(40000);
        manager.processSessionExpirationQueue();
        assertEquals(0, manager.getSessions().size());
        verify(sessionLayer).sendSession(session, null);
    }

    @Test
    void aUdpExchangeExpiresOnItsOwnShortLifetimeWhileConnectionsStay() {
        Config.datagramSessionLifetimeMs = -1000;
        final StreamSessionImpl kept = stream(40000);
        final DatagramSessionImpl gone = datagram(50000);
        manager.processSessionExpirationQueue();
        assertEquals(1, manager.getSessions().size());
        assertSame(kept, stream(40000));
        verify(sessionLayer).sendSession(gone, null);
    }

    @Test
    void anOldConnectionBehindAYoungerUdpExchangeIsStillChecked() {
        // The queue is ordered by last update; a long-lived entry at its head must not hide an expired one behind it.
        final StreamSessionImpl old = stream(40000);
        old.state = TcpStates.ESTABLISHED;
        final DatagramSessionImpl lookup = datagram(50000);
        Config.datagramSessionLifetimeMs = -1000;
        manager.processSessionExpirationQueue();
        verify(sessionLayer).sendSession(lookup, null);
        verify(sessionLayer, never()).sendSession(old, null);
    }

    @Test
    void udpExchangesDoNotUseUpTheConnectionLimit() {
        Config.defaultSessionsNumberPerCardLimit = 2;
        assertNotNull(stream(40000));
        for (int port = 50000; port < 50040; port++) {
            assertNotNull(datagram(port), "lookup " + port + " was refused");
        }
        assertNotNull(stream(40001));
        assertNull(stream(40002), "the third connection is over the limit");
    }

    @Test
    void connectionsDoNotUseUpTheUdpLimit() {
        Config.datagramSessionsNumberPerCardLimit = 2;
        for (int port = 40000; port < 40020; port++) {
            assertNotNull(stream(port), "connection " + port + " was refused");
        }
        assertNotNull(datagram(50000));
        assertNotNull(datagram(50001));
    }

    @Test
    void aFullUdpTableDropsTheOldestExchangeInsteadOfRefusingTheNewOne() {
        Config.datagramSessionsNumberPerCardLimit = 2;
        final DatagramSessionImpl first = datagram(50000);
        final DatagramSessionImpl second = datagram(50001);
        final DatagramSessionImpl third = datagram(50002);
        assertNotNull(third);
        assertEquals(2, manager.getSessions().size());
        verify(sessionLayer).sendSession(first, null);
        verify(sessionLayer, never()).sendSession(second, null);
        assertSame(second, datagram(50001));
        assertSame(third, datagram(50002));
    }
}
