package li.cil.oc2.common.vxlan;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.config.Config;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Manages VXLAN tunnels between the game server and an external tunnel endpoint.
 * The manager owns a single UDP socket bound to {@code bindHost:bindPort}; all
 * tunnels share it and send their traffic to {@code remoteHost:remotePort}.
 *
 * <p>Each datagram carries an eight-byte VXLAN header (flag byte with the
 * VNI-present bit set, reserved bytes, then a 24-bit VNI) followed by the inner
 * Ethernet frame. Inbound frames are demultiplexed by VNI to the
 * {@link TunnelInterface} registered under that VTI and appended to its packet
 * queue; the queue is owned by the registering block entity, which drains it on
 * the server thread. Frames for unknown VNIs are dropped.
 *
 * <p>Outbound frames written to a {@link TunnelInterface} get the same header
 * prepended and are sent as a single datagram to the remote endpoint.
 */
public class TunnelManager {

    private final ReentrantLock lock = new ReentrantLock();

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<Integer, TunnelInterface> tunnels = new ConcurrentHashMap<>();
    private DatagramSocket socket;
    private static TunnelManager managerInstance;
    private final InetAddress remoteHost;
    private final short remotePort;
    private final InetAddress bindHost;
    private final short bindPort;

    public TunnelManager(
            InetAddress bindHost, short bindPort, InetAddress remoteHost, short remotePort)
            throws SocketException {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
    }

    public static void initialize() {
        LOGGER.info("Initializing outernet tunnel manager");

        try {
            managerInstance =
                    new TunnelManager(
                            InetAddress.getByName(Config.bindHost), (short) Config.bindPort,
                            InetAddress.getByName(Config.remoteHost), (short) Config.remotePort);
        } catch (SocketException | UnknownHostException e) {
            LOGGER.error("Failed to bind to configured address: " + e.getMessage());
            LOGGER.error(e);
        }

        if (Config.enable) {
            Thread bgThread =
                    new Thread(
                            () -> {
                                try {
                                    managerInstance.listen();
                                } catch (IOException e) {
                                    LOGGER.error(e);
                                }
                            });
            bgThread.setName("VXLAN Background Thread");
            bgThread.start();
        }
    }

    /**
     * Receive loop for the background socket thread. Reads datagrams, validates and
     * strips the eight-byte VXLAN header, and enqueues the remaining Ethernet frame
     * into the packet queue of the tunnel interface registered for the extracted VNI.
     */
    public void listen() throws IOException {
        LOGGER.printf(Level.INFO, "Binding %s:%s\n", bindHost, bindPort);

        if (Config.enable) {
            socket = new DatagramSocket(bindPort, bindHost);
        } else {
            return;
        }
        LOGGER.printf(
                Level.INFO,
                "Bind successful: connected=%s bound=%s\n",
                socket.isConnected(),
                socket.isBound());

        byte[] buffer = new byte[65535];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        // The loop lives as long as the server process; there is no graceful shutdown.
        //noinspection InfiniteLoopStatement
        while (true) {
            socket.receive(packet);

            // A VXLAN header is eight bytes; anything shorter cannot carry a frame.
            if (packet.getLength() < 8) {
                continue;
            }

            // The flag byte must have bit 0x08 set ("VNI present"); the VNI itself is
            // stored big-endian in bytes 4..6 of the header.
            byte flags = packet.getData()[0];
            int vni =
                    (packet.getData()[6] & 0xFF)
                            | ((packet.getData()[5] & 0xFF) << 8)
                            | ((packet.getData()[4] & 0xFF) << 16);

            if ((flags & 0x08) != 0x08) {
                continue;
            }

            LOGGER.debug("recv on vti " + vni);

            TunnelInterface iface = tunnels.get(vni);

            if (iface != null) {
                // NOPMD - the buffer length depends on the received packet.
                byte[] inner = new byte[packet.getLength() - 8]; // NOPMD allocation depends on loop iteration / per-item state
                System.arraycopy(packet.getData(), 8, inner, 0, packet.getLength() - 8);

                // Queues are bounded blocking queues owned by the registering block
                // entities, so offers are already thread-safe; the lock merely
                // serializes producers on this socket thread.
                lock.lock();
                try {

                    iface.packetQueue.offer(inner);
                
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public static TunnelManager instance() {
        return managerInstance;
    }

    /**
     * Sends an Ethernet frame through the tunnel, prepending an eight-byte VXLAN
     * header that carries the given VTI as the VNI.
     */
    public void sendToOuternet(int vti, byte[] payload) {
        if (socket != null) {

            byte[] buffer = new byte[payload.length + 8];

            System.arraycopy(payload, 0, buffer, 8, payload.length);

            buffer[0] = 0x08;
            buffer[4] = (byte) ((vti >> 16) & 0xff);
            buffer[5] = (byte) ((vti >> 8) & 0xff);
            buffer[6] = (byte) (vti & 0xff);

            DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length, this.remoteHost, this.remotePort);

            try {
                socket.send(packet);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        } else {
            LOGGER.error("No socket in TunnelManager\n");
        }
    }

    /**
     * Registers a tunnel interface for the given VTI. Frames received with this VNI
     * are appended to {@code packetQueue}, which the owner drains on the server thread.
     */
    public NetworkInterface registerVti(int vti, Queue<byte[]> packetQueue) {
        TunnelInterface tuniface = new TunnelInterface(vti, packetQueue);
        tunnels.put(vti, tuniface);
        return tuniface;
    }

    public void unregisterVti(int vti) {
        tunnels.remove(vti);
    }

    /**
     * One end of a VXLAN tunnel as seen from the local network bus. Frames written
     * to it are forwarded to the remote tunnel endpoint; reading always yields no
     * frame, because inbound frames are delivered through the external packet queue.
     */
    public class TunnelInterface implements NetworkInterface {
        final Queue<byte[]> packetQueue;
        private final int vti;

        public TunnelInterface(int vti, Queue<byte[]> packetQueue) {
            this.vti = vti;
            this.packetQueue = packetQueue;
        }

        private static final byte[] NO_FRAME = new byte[0];

        @Override
        public byte[] readEthernetFrame() {
            return NO_FRAME;
        }

        @Override
        public void writeEthernetFrame(
                final @NotNull NetworkInterface source,
                final byte @NotNull [] frame,
                final int timeToLive) {
            TunnelManager.this.sendToOuternet(vti, frame);
        }
    }
}