package li.cil.oc2.common.inet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.internet.InternetAdapter;
import li.cil.oc2.common.inet.internet.InternetConnection;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import li.cil.oc2.platform.event.CommonEvents;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The whole stack as a guest sees it, against servers on this machine: ARP for the gateway, UDP exchanges,
 * a TCP download and a ping. Frames travel through the production queues and are moved once per simulated
 * server tick, like on a server. Loopback is taken out of the denied hosts for the test.
 */
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidUsingHardCodedIP"})
class InternetStackEndToEndTest {
    private static final int GUEST = 0x0A00020F; // 10.0.2.15
    private static final int GATEWAY = 0x0A000202; // 10.0.2.2
    private static final int LOOPBACK = 0x7F000001;
    private static final byte[] GUEST_MAC = {2, 0, 0, 0, 0x0A, 0x01};
    private static final int ETHERNET = 14;
    private static final int IP_START = ETHERNET + 20;
    private static final int DOWNLOAD_SIZE = 300_000;

    private static boolean previousEnabled;
    private static int previousGlobalLimit;
    private static List<String> previousDenied;

    private final ConcurrentLinkedQueue<byte[]> toStack = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<byte[]> fromStack = new ConcurrentLinkedQueue<>();
    private byte[] gatewayMac;
    private InternetConnection connection;
    private DatagramSocket udpServer;
    private ServerSocket tcpServer;
    private final byte[] download = new byte[DOWNLOAD_SIZE];
    private final ConcurrentLinkedQueue<Exception> serverEvents = new ConcurrentLinkedQueue<>();

    @BeforeAll
    static void startManager() {
        previousEnabled = Config.internetCardEnabled;
        previousDenied = Config.deniedHosts;
        previousGlobalLimit = Config.defaultSessionsNumberLimit;
        // The total over all cards is a static counter that tests of other classes may have left raised.
        Config.defaultSessionsNumberLimit = Integer.MAX_VALUE;
        Config.internetCardEnabled = true;
        Config.deniedHosts = List.of("0.0.0.0/8", "10.0.0.0/8", "192.168.0.0/16");
        Config.allowedHosts = List.of();
        InternetManagerImpl.initialize();
    }

    @AfterAll
    static void restoreConfig() {
        Config.internetCardEnabled = previousEnabled;
        Config.deniedHosts = previousDenied;
        Config.defaultSessionsNumberLimit = previousGlobalLimit;
    }

    @BeforeEach
    void connect() throws Exception {
        for (int i = 0; i < download.length; i++) download[i] = (byte) (i * 31 + (i >>> 8));
        udpServer = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        tcpServer = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
        startUdpServer();
        startTcpServer();
        final InternetAdapter adapter = new InternetAdapter() {
            @Override
            @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull") // the adapter contract is "no frame: null"
            public @Nullable byte[] receiveEthernetFrame() {
                return toStack.poll();
            }

            @Override
            public void sendEthernetFrame(final byte[] frame) {
                fromStack.add(frame.clone());
            }
        };
        connection = InternetManagerImpl.getInstance().orElseThrow().connect(adapter, null);
        resolveGateway();
    }

    @AfterEach
    void close() throws Exception {
        udpServer.close();
        tcpServer.close();
        // Stopping tears the stack down on the next ticks and releases its sessions from the shared counter.
        connection.stop();
        for (int i = 0; i < 10; i++) tick();
    }

    private void startUdpServer() {
        final Thread thread = new Thread(() -> {
            final byte[] buffer = new byte[2048];
            while (!udpServer.isClosed()) {
                final DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                try {
                    udpServer.receive(request);
                    final byte[] answer = new byte[request.getLength() + 4];
                    System.arraycopy("ANS:".getBytes(StandardCharsets.US_ASCII), 0, answer, 0, 4);
                    System.arraycopy(request.getData(), 0, answer, 4, request.getLength());
                    udpServer.send(new DatagramPacket(answer, answer.length, request.getSocketAddress()));
                } catch (final IOException closedWithTheTest) {
                    serverEvents.add(closedWithTheTest);
                }
            }
        }, "e2e-udp");
        thread.setDaemon(true);
        thread.start();
    }

    private void startTcpServer() {
        final Thread thread = new Thread(() -> {
            try (Socket client = tcpServer.accept()) {
                final InputStream in = client.getInputStream();
                final StringBuilder request = new StringBuilder();
                while (!request.toString().endsWith("\r\n\r\n")) {
                    final int value = in.read();
                    if (value < 0) {
                        break;
                    }
                    request.append((char) value);
                }
                final OutputStream out = client.getOutputStream();
                out.write(download);
                out.flush();
            } catch (final IOException closedWithTheTest) {
                serverEvents.add(closedWithTheTest);
            }
        }, "e2e-tcp");
        thread.setDaemon(true);
        thread.start();
    }

    private void tick() throws InterruptedException {
        CommonEvents.SERVER_TICK_START.fire(listener -> listener.accept(null));
        Thread.sleep(8);
    }

    @Nullable
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull") // "no matching frame" is null for the caller to assert
    private byte[] awaitFrame(final Predicate<byte[]> wanted, final int ticks) throws InterruptedException {
        for (int i = 0; i < ticks; i++) {
            tick();
            for (byte[] frame = fromStack.poll(); frame != null; frame = fromStack.poll()) {
                if (wanted.test(frame)) {
                    return frame;
                }
            }
        }
        return null;
    }

    private void resolveGateway() throws InterruptedException {
        final ByteBuffer arp = ByteBuffer.allocate(42);
        arp.put(new byte[] {-1, -1, -1, -1, -1, -1}).put(GUEST_MAC).putShort((short) 0x0806);
        arp.putShort((short) 1).putShort((short) 0x0800).put((byte) 6).put((byte) 4).putShort((short) 1);
        arp.put(GUEST_MAC).putInt(GUEST).put(new byte[6]).putInt(GATEWAY);
        toStack.add(arp.array());
        final byte[] reply = awaitFrame(f -> f.length >= 42 && (f[12] & 0xFF) == 0x08 && (f[13] & 0xFF) == 0x06, 400);
        assertNotNull(reply, "no ARP reply for the gateway");
        gatewayMac = new byte[6];
        System.arraycopy(reply, 6, gatewayMac, 0, 6);
    }

    private byte[] ip(final int protocol, final byte[] payload) {
        final ByteBuffer frame = ByteBuffer.allocate(ETHERNET + 20 + payload.length);
        frame.put(gatewayMac).put(GUEST_MAC).putShort((short) 0x0800);
        frame.put((byte) 0x45).put((byte) 0).putShort((short) (20 + payload.length));
        frame.putShort((short) 1).putShort((short) 0x4000).put((byte) 64).put((byte) protocol).putShort((short) 0);
        frame.putInt(GUEST).putInt(LOOPBACK).put(payload);
        return frame.array();
    }

    private static boolean isIp(final byte[] frame, final int protocol) {
        return frame.length >= IP_START && (frame[12] & 0xFF) == 0x08 && (frame[13] & 0xFF) == 0x00
                && (frame[23] & 0xFF) == protocol;
    }

    private void sendUdp(final int sourcePort, final int destinationPort, final byte[] data) {
        final ByteBuffer udp = ByteBuffer.allocate(8 + data.length);
        udp.putShort((short) sourcePort).putShort((short) destinationPort).putShort((short) udp.capacity())
                .putShort((short) 0).put(data);
        toStack.add(ip(17, udp.array()));
    }

    @Test
    void theFirstUdpDatagramOfAnExchangeIsAnswered() throws Exception {
        final byte[] query = "who-is-there".getBytes(StandardCharsets.US_ASCII);
        sendUdp(40001, udpServer.getLocalPort(), query);
        final byte[] answer = awaitFrame(f -> isIp(f, 17), 400);
        assertNotNull(answer, "the first datagram of a new source port never got an answer");
        final int length = ((answer[IP_START + 4] & 0xFF) << 8 | (answer[IP_START + 5] & 0xFF)) - 8;
        final byte[] payload = new byte[length];
        System.arraycopy(answer, IP_START + 8, payload, 0, length);
        assertEquals("ANS:who-is-there", new String(payload, StandardCharsets.US_ASCII));
        assertEquals(40001, (answer[IP_START + 2] & 0xFF) << 8 | (answer[IP_START + 3] & 0xFF), "it goes back to the source port");
    }

    @Test
    void manyLookupsWithFreshSourcePortsAllGetAnswers() throws Exception {
        for (int port = 41000; port < 41100; port++) {
            sendUdp(port, udpServer.getLocalPort(), ("q" + port).getBytes(StandardCharsets.US_ASCII));
            final int expected = port;
            final byte[] answer = awaitFrame(f -> isIp(f, 17) && ((f[IP_START + 2] & 0xFF) << 8 | (f[IP_START + 3] & 0xFF)) == expected, 300);
            assertNotNull(answer, "lookup from port " + port + " was never answered");
        }
    }

    @Test
    void aTcpDownloadArrivesCompleteAndInOrder() throws Exception {
        final int port = 42000;
        final int[] sequence = {0x1000};
        sendTcp(port, sequence[0], 0, 0x02, new byte[0]);
        sequence[0]++;
        final byte[] synAck = awaitFrame(f -> isIp(f, 6) && (f[IP_START + 13] & 0x12) == 0x12, 400);
        assertNotNull(synAck, "no SYN-ACK from the server on this machine");
        int ack = ByteBuffer.wrap(synAck, IP_START + 4, 4).getInt() + 1;
        sendTcp(port, sequence[0], ack, 0x10, new byte[0]);
        final byte[] request = "GET / HTTP/1.1\r\nHost: test\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        sendTcp(port, sequence[0], ack, 0x18, request);
        sequence[0] += request.length;

        final byte[] received = new byte[DOWNLOAD_SIZE];
        int size = 0;
        boolean finished = false;
        for (int i = 0; i < 4000 && !finished; i++) {
            tick();
            for (byte[] frame = fromStack.poll(); frame != null; frame = fromStack.poll()) {
                if (!isIp(frame, 6)) continue;
                final int offset = (frame[IP_START + 12] & 0xF0) >> 2;
                final int flags = frame[IP_START + 13] & 0xFF;
                final int length = frame.length - IP_START - offset;
                assertTrue((flags & 0x04) == 0, "the stack reset the connection after " + size + " bytes");
                if (length > 0 && ByteBuffer.wrap(frame, IP_START + 4, 4).getInt() == ack) {
                    System.arraycopy(frame, IP_START + offset, received, size, length);
                    size += length;
                    ack += length;
                    sendTcp(port, sequence[0], ack, 0x10, new byte[0]);
                }
                if ((flags & 0x01) != 0) {
                    ack++;
                    sendTcp(port, sequence[0], ack, 0x11, new byte[0]);
                    finished = true;
                }
            }
        }
        assertTrue(finished, "no FIN after " + size + " of " + DOWNLOAD_SIZE + " bytes");
        assertEquals(DOWNLOAD_SIZE, size);
        assertArrayEquals(download, received);
    }

    @Test
    void aPingIsAnsweredWithTheDataOfTheRequest() throws Exception {
        final byte[] data = new byte[56];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i + 9);
        final ByteBuffer icmp = ByteBuffer.allocate(8 + data.length);
        icmp.put((byte) 8).put((byte) 0).putShort((short) 0).putShort((short) 0x4242).putShort((short) 1).put(data);
        toStack.add(ip(1, icmp.array()));
        final byte[] pong = awaitFrame(f -> isIp(f, 1) && (f[IP_START] & 0xFF) == 0, 400);
        assertNotNull(pong, "no echo reply from loopback");
        final byte[] echoed = new byte[data.length];
        System.arraycopy(pong, IP_START + 8, echoed, 0, echoed.length);
        assertArrayEquals(data, echoed);
    }

    private void sendTcp(final int sourcePort, final int sequence, final int ack, final int flags, final byte[] data) {
        final ByteBuffer tcp = ByteBuffer.allocate(20 + data.length);
        tcp.putShort((short) sourcePort).putShort((short) tcpServer.getLocalPort()).putInt(sequence).putInt(ack);
        tcp.put((byte) 0x50).put((byte) flags).putShort((short) 60000).putShort((short) 0).putShort((short) 0).put(data);
        toStack.add(ip(6, tcp.array()));
    }
}
