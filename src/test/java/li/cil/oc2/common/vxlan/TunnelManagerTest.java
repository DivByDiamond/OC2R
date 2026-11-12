package li.cil.oc2.common.vxlan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import li.cil.oc2.common.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The VXLAN hub against a plain UDP peer, which is all a Linux host with {@code ip link add vxlan0 type vxlan}
 * is from the mod's point of view. Ports come from the ephemeral range on purpose: it lies above 32767.
 */
@SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "The tests switch the tunnel on in the process-wide configuration and put it back afterwards.")
class TunnelManagerTest {
    private static final int VNI = 0x123456;
    private static final byte[] FRAME = new byte[64];
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    private DatagramSocket peer;
    private DatagramSocket stranger;
    private TunnelManager manager;
    private Thread loop;
    private final BlockingQueue<byte[]> inbound = new ArrayBlockingQueue<>(8);
    private final List<Exception> events = new ArrayList<>();
    private boolean previousEnable;
    private int bindPort;

    @BeforeEach
    void start() throws Exception {
        previousEnable = Config.enable;
        Config.enable = true;
        for (int i = 0; i < FRAME.length; i++) FRAME[i] = (byte) (i * 7 + 1);
        // Ports above 32767 are the ones a 16 bit signed type cannot hold; the configuration allows up to 65535.
        peer = socketAbove(40000);
        stranger = socketAbove(peer.getLocalPort() + 1);
        try (DatagramSocket probe = socketAbove(stranger.getLocalPort() + 1)) {
            bindPort = probe.getLocalPort();
        }
        assertTrue(bindPort > Short.MAX_VALUE && peer.getLocalPort() > Short.MAX_VALUE);
        manager = new TunnelManager(LOOPBACK, bindPort, LOOPBACK, peer.getLocalPort());
        loop = new Thread(() -> {
            try {
                manager.listen();
            } catch (final Exception closedByShutdown) {
                events.add(closedByShutdown);
            }
        }, "tunnel-test");
        loop.setDaemon(true);
        loop.start();
        manager.registerVti(VNI, inbound);
        for (int i = 0; i < 100 && !bound(); i++) Thread.sleep(20);
        peer.setSoTimeout(1000);
    }

    @AfterEach
    void stop() {
        manager.shutdown();
        peer.close();
        stranger.close();
        Config.enable = previousEnable;
    }

    @Test
    void aFrameFromThePeerReachesTheHubWithTheMatchingVni() throws Exception {
        peer.send(new DatagramPacket(datagram(VNI, FRAME), 8 + FRAME.length, LOOPBACK, bindPort));
        assertArrayEquals(FRAME, inbound.poll(2, TimeUnit.SECONDS));
    }

    @Test
    void aFrameForAnotherNetworkIsIgnored() throws Exception {
        peer.send(new DatagramPacket(datagram(VNI + 1, FRAME), 8 + FRAME.length, LOOPBACK, bindPort));
        assertNull(inbound.poll(300, TimeUnit.MILLISECONDS));
    }

    @Test
    void aFrameFromAnotherHostOrPortIsDroppedByTheConnectedSocket() throws Exception {
        stranger.send(new DatagramPacket(datagram(VNI, FRAME), 8 + FRAME.length, LOOPBACK, bindPort));
        assertNull(inbound.poll(300, TimeUnit.MILLISECONDS));
    }

    @Test
    void aFrameFromTheHubReachesThePeerWithAStandardVxlanHeader() throws Exception {
        manager.sendToOuternet(VNI, FRAME);
        final byte[] buffer = new byte[2048];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        peer.receive(packet);
        assertEquals(8 + FRAME.length, packet.getLength());
        assertEquals(0x08, buffer[0], "the I flag marks a valid VNI");
        assertEquals(VNI, (buffer[4] & 0xFF) << 16 | (buffer[5] & 0xFF) << 8 | buffer[6] & 0xFF);
        assertEquals(bindPort, packet.getPort(), "the peer must answer to the bind port");
        final byte[] payload = new byte[FRAME.length];
        System.arraycopy(buffer, 8, payload, 0, payload.length);
        assertArrayEquals(FRAME, payload);
        assertNotNull(packet.getAddress());
    }

    private boolean bound() {
        // sendToOuternet drops with a warning until the channel exists; a probe frame tells when it does.
        manager.sendToOuternet(VNI, new byte[0]);
        try {
            final DatagramPacket probe = new DatagramPacket(new byte[64], 64);
            peer.setSoTimeout(20);
            peer.receive(probe);
            return true;
        } catch (final java.io.IOException noAnswerYet) {
            events.add(noAnswerYet);
            return false;
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // one socket per candidate port
    private static DatagramSocket socketAbove(final int minimum) throws java.io.IOException {
        final List<Exception> taken = new ArrayList<>();
        for (int port = minimum; port < 65536; port++) {
            try {
                return new DatagramSocket(new InetSocketAddress(LOOPBACK, port));
            } catch (final java.net.SocketException inUse) {
                taken.add(inUse);
            }
        }
        throw new java.io.IOException("no free UDP port above " + minimum + " (" + taken.size() + " tried)");
    }

    private static byte[] datagram(final int vni, final byte[] frame) {
        final byte[] bytes = new byte[8 + frame.length];
        bytes[0] = 0x08;
        bytes[4] = (byte) (vni >> 16);
        bytes[5] = (byte) (vni >> 8);
        bytes[6] = (byte) vni;
        System.arraycopy(frame, 0, bytes, 8, frame.length);
        return bytes;
    }
}
