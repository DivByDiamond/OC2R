package li.cil.oc2.common.vxlan;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.config.Config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Queue;

public class TunnelManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final HashMap<Integer, TunnelInterface> tunnels = new HashMap<>();
    private DatagramSocket socket;
    private static TunnelManager INSTANCE;
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
            INSTANCE =
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
                                    INSTANCE.listen();
                                } catch (IOException e) {
                                    LOGGER.error(e);
                                }
                            });
            bgThread.setName("VXLAN Background Thread");
            bgThread.start();
        }
    }

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
        // TODO shut this thread down more cleanly on server shutdown?
        //noinspection InfiniteLoopStatement
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            if (packet.getLength() < 8) {
                continue;
            }

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
                byte[] inner = new byte[packet.getLength() - 8];
                System.arraycopy(packet.getData(), 8, inner, 0, packet.getLength() - 8);

                // CircularFifoQueue isn't thread-safe, so we have to synchronize on it.
                synchronized (iface.packetQueue) {
                    iface.packetQueue.offer(inner);
                }
            }
        }
    }

    public static TunnelManager instance() {
        return INSTANCE;
    }

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

    public NetworkInterface registerVti(int vti, Queue<byte[]> packetQueue) {
        TunnelInterface tuniface = new TunnelInterface(vti, packetQueue);
        tunnels.put(vti, tuniface);
        return tuniface;
    }

    public void unregisterVti(int vti) {
        tunnels.remove(vti);
    }

    public class TunnelInterface implements NetworkInterface {
        final Queue<byte[]> packetQueue;
        private final int vti;

        public TunnelInterface(int vti, Queue<byte[]> packetQueue) {
            this.vti = vti;
            this.packetQueue = packetQueue;
        }

        @Override
        public byte[] readEthernetFrame() {
            return null;
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
