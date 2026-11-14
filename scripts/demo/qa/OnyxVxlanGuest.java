import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.vxlan.TunnelManager;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.device.Device;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.devicetree.DeviceTree;
import li.cil.sedna.api.devicetree.DeviceTreeProvider;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.AbstractVirtIODevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.device.virtio.VirtIONetworkDevice;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.memory.MemoryMaps;
import li.cil.sedna.riscv.R5Board;

/**
 * One real RV64 OnyxOS guest whose VirtIO NIC is wired to the production TunnelManager as the VXLAN
 * hub's tunnel interface. The guest is configured with netcfg and runs rshd; an ordinary peer reaches it
 * through a kernel VXLAN device. Arguments: fw_jump.bin onyx-kernel.bin onyxfs.img bindHost remoteHost
 * vni guestIp stopFile logFile.
 */
public final class OnyxVxlanGuest {
    static final class Provider implements DeviceTreeProvider {
        final String name, compatible;
        Provider(String n, String c) { name = n; compatible = c; }
        public Optional<String> getName(Device d) { return Optional.of(name); }
        public Optional<DeviceTree> createNode(DeviceTree r, MemoryMap m, Device d, String n) {
            return m.getMemoryRange((MemoryMappedDevice) d).map(range -> r.find("/soc").getChild(name, range.address()));
        }
        public void visit(DeviceTree node, MemoryMap map, Device d) {
            node.addProp("compatible", compatible);
            if (d instanceof UART16550A) node.addProp("clock-frequency", 3686400);
        }
    }

    static R5Board board = new R5Board();
    static UART16550A uart = new UART16550A();
    static VirtIONetworkDevice nic;
    static StringBuilder out = new StringBuilder();
    static int scan;
    static NetworkInterface tunnel;
    static final BlockingQueue<byte[]> inbound = new ArrayBlockingQueue<>(256);
    static long toTunnel, fromTunnel;

    static void step() throws Exception {
        board.step(10000);
        int c;
        while ((c = uart.read()) >= 0) out.append((char) c);
        byte[] frame;
        while ((frame = nic.readEthernetFrame()) != null) { tunnel.writeEthernetFrame(null, frame, 255); toTunnel++; }
        while ((frame = inbound.poll()) != null) { nic.writeEthernetFrame(frame); fromTunnel++; }
        Thread.sleep(0, 200_000);
    }

    static void waitFor(String marker, int seconds) throws Exception {
        long end = System.nanoTime() + seconds * 1_000_000_000L;
        while (System.nanoTime() < end) {
            step();
            if (out.indexOf(marker, scan) >= 0) { scan = out.length(); return; }
        }
        throw new AssertionError("timeout waiting for " + marker + "\n" + out.substring(Math.max(0, out.length() - 1500)));
    }

    static void send(String line) throws Exception {
        scan = out.length();
        for (byte b : (line + "\r").getBytes(StandardCharsets.US_ASCII)) {
            while (!uart.canPutByte()) step();
            uart.putByte(b);
            for (int i = 0; i < 3; i++) step();
        }
    }

    public static void main(String[] a) throws Exception {
        Sedna.initialize();
        DeviceTreeRegistry.putProvider(UART16550A.class, new Provider("uart", "ns16550a"));
        DeviceTreeRegistry.putProvider(AbstractVirtIODevice.class, new Provider("virtio", "virtio,mmio"));
        final int vni = Integer.parseInt(a[5]);
        Config.enable = true;
        final TunnelManager manager = new TunnelManager(InetAddress.getByName(a[3]), 4789, InetAddress.getByName(a[4]), 4789);
        final Thread listener = new Thread(() -> { try { manager.listen(); } catch (java.nio.channels.ClosedSelectorException stopped) { /* shutdown */ } catch (Exception e) { e.printStackTrace(); } });
        listener.setDaemon(true);
        listener.start();
        tunnel = manager.registerVti(vni, inbound);

        board.addDevice(0x80000000L, Memory.create(64 * 1024 * 1024));
        board.addDevice(0x10000000L, uart);
        uart.getInterrupt().set(4, board.getInterruptController());
        board.setStandardOutputDevice(uart);
        var block = new VirtIOBlockDevice(board.getMemoryMap(), ByteBufferBlockDevice.wrap(ByteBuffer.wrap(Files.readAllBytes(Path.of(a[2]))), false));
        board.addDevice(0x10001000L, block);
        block.getInterrupt().set(7, board.getInterruptController());
        nic = new VirtIONetworkDevice(board.getMemoryMap());
        board.addDevice(0x10002000L, nic);
        nic.getInterrupt().set(10, board.getInterruptController());
        MemoryMaps.store(board.getMemoryMap(), 0x80000000L, Files.newInputStream(Path.of(a[0])));
        MemoryMaps.store(board.getMemoryMap(), 0x80200000L, Files.newInputStream(Path.of(a[1])));
        board.initialize();
        board.setRunning(true);

        waitFor("login: ", 120);
        send("root");
        waitFor("osh$ ", 60);
        send("netcfg set 0 " + a[6] + " 255.255.255.0 0.0.0.0 0.0.0.0");
        waitFor("Network configured", 30);
        waitFor("osh$ ", 30);
        send("rshd 2323 pw-from-pc");
        waitFor("listening on TCP port 2323", 30);
        System.out.println("GUEST_READY vni=" + vni + " guest=" + a[6]);
        System.out.flush();
        final Path stop = Path.of(a[7]);
        while (!Files.exists(stop)) step();
        for (int i = 0; i < 2000; i++) step();
        Files.writeString(Path.of(a[8]), out.toString() + "\nframesToTunnel=" + toTunnel + " framesFromTunnel=" + fromTunnel + "\n");
        manager.shutdown();
        System.out.println("GUEST_DONE toTunnel=" + toTunnel + " fromTunnel=" + fromTunnel);
    }
}
