package li.cil.oc2.common.vxlan;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.config.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages VXLAN tunnels between the game server and an external tunnel endpoint.
 * The manager owns a single non-blocking UDP channel bound to {@code bindHost:bindPort};
 * all tunnels share it and send their traffic to {@code remoteHost:remotePort}.
 *
 * <p>Each datagram carries an eight-byte VXLAN header (flag byte with the
 * VNI-present bit set, reserved bytes, then a 24-bit VNI) followed by the inner
 * Ethernet frame. Inbound frames are demultiplexed by VNI to the
 * {@link TunnelInterface} registered under that VTI and appended to its packet
 * queue; the queue is owned by the registering block entity, which drains it on
 * the server thread. Frames for unknown VNIs are dropped.
 *
 * <p>Outbound frames written to a {@link TunnelInterface} get the same header
 * prepended and are sent as a single datagram to the remote endpoint. Sends are
 * non-blocking: if the OS socket buffer is full the datagram is dropped, which
 * UDP receivers must tolerate anyway &mdash; this keeps frame floods on the server
 * tick thread from ever blocking it (todo.md §38 Ш3).
 */
@EventBusSubscriber(modid = API.MOD_ID)
public class TunnelManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int HEADER_SIZE = 8;
    private static final int MAX_DATAGRAM_SIZE = 65535;

    private final Map<Integer, TunnelInterface> tunnels = new ConcurrentHashMap<>();

    @Nullable private volatile DatagramChannel channel;
    @Nullable private Selector selector;
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
            LOGGER.error("Failed to bind to configured address", e);
        }

        if (Config.enable && managerInstance != null) {
            Thread bgThread =
                    new Thread(
                            () -> {
                                try {
                                    managerInstance.listen();
                                } catch (final ClosedSelectorException e) {
                                    LOGGER.info("VXLAN receive loop stopped");
                                } catch (IOException e) {
                                    LOGGER.error("VXLAN receive loop failed", e);
                                }
                            });
            bgThread.setName("VXLAN Background Thread");
            bgThread.setDaemon(true);
            bgThread.start();
        }
    }

    /**
     * Receive loop for the background socket thread. Blocks on a selector with a timeout,
     * reads datagrams, validates and strips the eight-byte VXLAN header, and enqueues the
     * remaining Ethernet frame into the packet queue of the tunnel interface registered for
     * the extracted VNI. Exiting the loop closes the channel and selector.
     */
    public void listen() throws IOException {
        LOGGER.printf(Level.INFO, "Binding %s:%s\n", bindHost, bindPort);

        if (!Config.enable) {
            return;
        }

        final DatagramChannel datagramChannel = DatagramChannel.open();
        // Non-blocking sends from the server thread never stall a tick (Ш3); receives
        // block on the selector below instead of the channel itself.
        datagramChannel.configureBlocking(false);
        datagramChannel.bind(new InetSocketAddress(bindHost, bindPort));
        final Selector datagramSelector = Selector.open();
        datagramChannel.register(datagramSelector, SelectionKey.OP_READ);
        channel = datagramChannel;
        selector = datagramSelector;

        LOGGER.printf(
                Level.INFO,
                "Bind successful: local=%s blocking=%s\n",
                datagramChannel.getLocalAddress(),
                datagramChannel.isBlocking());

        final ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_DATAGRAM_SIZE);

        while (true) {
            // Timeout doubles as a shutdown checkpoint: closing the selector makes
            // select() throw ClosedSelectorException and ends this thread.
            if (datagramSelector.select(1000) == 0) {
                continue;
            }
            datagramSelector.selectedKeys().clear();

            // Drain everything currently available before going back to sleep.
            while (true) {
                buffer.clear();
                final InetSocketAddress sender =
                        (InetSocketAddress) datagramChannel.receive(buffer);
                if (sender == null) {
                    break;
                }
                buffer.flip();
                handleDatagram(buffer);
            }
        }
    }

    /** Validates and dispatches one received VXLAN datagram. */
    private void handleDatagram(final ByteBuffer datagram) {
        if (datagram.remaining() < HEADER_SIZE) {
            return;
        }

        // The flag byte must have bit 0x08 set ("VNI present"); the VNI itself is
        // stored big-endian in bytes 4..6 of the header.
        final byte flags = datagram.get(0);
        if ((flags & 0x08) != 0x08) {
            return;
        }
        final int vni =
                ((datagram.get(6) & 0xFF))
                        | ((datagram.get(5) & 0xFF) << 8)
                        | ((datagram.get(4) & 0xFF) << 16);

        final TunnelInterface iface = tunnels.get(vni);
        if (iface == null) {
            return;
        }

        LOGGER.debug("recv on vti {}", vni);

        final byte[] inner = new byte[datagram.remaining() - HEADER_SIZE]; // NOPMD allocation depends on loop iteration / per-item state
        datagram.position(HEADER_SIZE);
        datagram.get(inner);

        // ArrayBlockingQueue.offer is already thread-safe; no external lock needed (Ш5).
        if (!iface.packetQueue.offer(inner)) {
            iface.droppedFrames.incrementAndGet();
        }
    }

    public static TunnelManager instance() {
        return managerInstance;
    }

    /**
     * Sends an Ethernet frame through the tunnel, prepending an eight-byte VXLAN
     * header that carries the given VTI as the VNI. The send is non-blocking; when
     * the OS socket buffer is full the datagram is silently dropped.
     */
    public void sendToOuternet(int vti, byte[] payload) {
        final DatagramChannel datagramChannel = channel;
        if (datagramChannel == null || !datagramChannel.isOpen()) {
            LOGGER.warn("No socket in TunnelManager");
            return;
        }

        final byte[] buffer = new byte[payload.length + HEADER_SIZE];
        System.arraycopy(payload, 0, buffer, HEADER_SIZE, payload.length);

        buffer[0] = 0x08;
        buffer[4] = (byte) ((vti >> 16) & 0xff);
        buffer[5] = (byte) ((vti >> 8) & 0xff);
        buffer[6] = (byte) (vti & 0xff);

        try {
            datagramChannel.send(ByteBuffer.wrap(buffer), new InetSocketAddress(remoteHost,
                    remotePort));
        } catch (IOException e) {
            LOGGER.error("Failed to send VXLAN datagram", e);
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

    /** Closes the shared channel and wakes up the receive loop. Idempotent. */
    public void shutdown() {
        try {
            if (selector != null) {
                selector.wakeup();
                selector.close();
            }
        } catch (final IOException e) {
            LOGGER.error("Exception during VXLAN selector shutdown", e);
        }
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (final IOException e) {
            LOGGER.error("Exception during VXLAN channel shutdown", e);
        }
    }

    /** Stops the receive loop on server shutdown instead of leaking the bound port. */
    @SubscribeEvent
    public static void onServerStopping(final ServerStoppingEvent event) {
        if (managerInstance != null) {
            managerInstance.shutdown();
            managerInstance = null;
        }
    }

    /**
     * One end of a VXLAN tunnel as seen from the local network bus. Frames written
     * to it are forwarded to the remote tunnel endpoint; reading always yields no
     * frame, because inbound frames are delivered through the external packet queue.
     */
    public class TunnelInterface implements NetworkInterface {
        final Queue<byte[]> packetQueue;
        /** Frames dropped because the owner's packet queue was full between ticks (Ш6). */
        public final AtomicInteger droppedFrames = new AtomicInteger();

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
