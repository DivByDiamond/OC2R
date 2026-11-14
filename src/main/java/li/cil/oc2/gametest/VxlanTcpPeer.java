/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A minimal Ethernet host on the far side of a VXLAN tunnel: it answers ARP for its own address,
 * pings a guest and runs one TCP connection as the client. It stands in for a PC with a Linux VXLAN
 * device, speaking raw frames over the same UDP datagrams, so a test can talk to a guest service
 * the way {@code nc} would. All state is advanced by {@link #poll()}, which never blocks.
 */
final class VxlanTcpPeer {
    private static final int HEADER = 8;
    private static final int ETHERNET = 14;
    private static final int ARP_LENGTH = 28;
    private static final int PROTO_ICMP = 1;
    private static final int PROTO_TCP = 6;
    private static final int SYN = 0x02;
    private static final int ACK = 0x10;
    private static final int PSH = 0x08;
    private static final int FIN = 0x01;
    private static final int RST = 0x04;

    private final DatagramChannel channel;
    private final InetSocketAddress tunnel;
    private final int vni;
    private final byte[] mac = {0x02, 0x00, 0x00, 0x00, 0x77, 0x01};
    private final byte[] ip;
    private final byte[] guestIp;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(2048);
    private final StringBuilder received = new StringBuilder();
    private byte[] guestMac;
    private boolean pinged;
    private int port;
    private long sendSequence = 0x1000;
    private long receiveSequence;
    private boolean established;
    private boolean reset;
    private boolean finished;
    private int datagrams;

    /** @param channel a non-blocking datagram channel bound to the address the tunnel manager sends to */
    VxlanTcpPeer(final DatagramChannel channel, final InetSocketAddress tunnel, final int vni,
                 final byte[] ip, final byte[] guestIp) {
        this.channel = channel;
        this.tunnel = tunnel;
        this.vni = vni;
        this.ip = ip.clone();
        this.guestIp = guestIp.clone();
    }

    /** How many tunnel datagrams have arrived from the hub so far. */
    int datagrams() {
        return datagrams;
    }

    boolean resolvedGuest() {
        return guestMac != null;
    }

    boolean gotEchoReply() {
        return pinged;
    }

    boolean connected() {
        return established;
    }

    boolean wasReset() {
        return reset;
    }

    boolean guestClosed() {
        return finished;
    }

    /** Everything the guest has written to the connection and nobody consumed yet. */
    String received() {
        return received.toString();
    }

    void consume(final int length) {
        received.delete(0, length);
    }

    void arpRequest() throws IOException {
        final byte[] arp = new byte[ARP_LENGTH];
        arpHeader(arp, 1);
        System.arraycopy(mac, 0, arp, 8, 6);
        System.arraycopy(ip, 0, arp, 14, 4);
        System.arraycopy(guestIp, 0, arp, 24, 4);
        sendFrame(new byte[] {-1, -1, -1, -1, -1, -1}, 0x0806, arp);
    }

    void ping(final int sequence) throws IOException {
        final byte[] icmp = new byte[16];
        icmp[0] = 8;
        icmp[4] = 0x12;
        icmp[5] = 0x34;
        icmp[7] = (byte) sequence;
        final int sum = checksum(icmp, 0, icmp.length);
        icmp[2] = (byte) (sum >> 8);
        icmp[3] = (byte) sum;
        sendIp(PROTO_ICMP, icmp);
    }

    void connect(final int destinationPort) throws IOException {
        port = destinationPort;
        sendTcp(SYN, new byte[0], new byte[] {2, 4, 0x02, 0x18});
        sendSequence++;
    }

    void send(final String text) throws IOException {
        final byte[] data = text.getBytes(StandardCharsets.US_ASCII);
        sendTcp(ACK | PSH, data, new byte[0]);
        sendSequence += data.length;
    }

    void close() throws IOException {
        sendTcp(ACK | FIN, new byte[0], new byte[0]);
        sendSequence++;
    }

    /** Handles every datagram that is waiting. */
    void poll() throws IOException {
        for (;;) {
            receiveBuffer.clear();
            if (channel.receive(receiveBuffer) == null) {
                return;
            }
            receiveBuffer.flip();
            datagrams++;
            if (receiveBuffer.remaining() > HEADER + ETHERNET) {
                handle(Arrays.copyOfRange(receiveBuffer.array(), HEADER, receiveBuffer.limit()));
            }
        }
    }

    private void handle(final byte[] frame) throws IOException {
        final int type = (frame[12] & 0xff) << 8 | frame[13] & 0xff;
        if (type == 0x0806 && frame.length >= ETHERNET + ARP_LENGTH) {
            handleArp(frame);
        } else if (type == 0x0800 && frame.length >= ETHERNET + 20 && Arrays.equals(Arrays.copyOfRange(frame, 30, 34), ip)) {
            final int headerLength = (frame[14] & 0x0f) * 4;
            final byte[] payload = Arrays.copyOfRange(frame, ETHERNET + headerLength, ETHERNET + (frame[16] & 0xff) * 256 + (frame[17] & 0xff));
            if (frame[23] == PROTO_ICMP && payload.length >= 8 && payload[0] == 0) {
                pinged = true;
            } else if (frame[23] == PROTO_TCP && payload.length >= 20) {
                handleTcp(payload);
            }
        }
    }

    private void handleArp(final byte[] frame) throws IOException {
        final int operation = frame[21] & 0xff;
        final byte[] senderMac = Arrays.copyOfRange(frame, 22, 28);
        final byte[] senderIp = Arrays.copyOfRange(frame, 28, 32);
        if (Arrays.equals(senderIp, guestIp)) {
            guestMac = senderMac;
        }
        if (operation == 1 && Arrays.equals(Arrays.copyOfRange(frame, 38, 42), ip)) {
            final byte[] arp = new byte[ARP_LENGTH];
            arpHeader(arp, 2);
            System.arraycopy(mac, 0, arp, 8, 6);
            System.arraycopy(ip, 0, arp, 14, 4);
            System.arraycopy(senderMac, 0, arp, 18, 6);
            System.arraycopy(senderIp, 0, arp, 24, 4);
            sendFrame(senderMac, 0x0806, arp);
        }
    }

    private void handleTcp(final byte[] segment) throws IOException {
        final int sourcePort = (segment[0] & 0xff) << 8 | segment[1] & 0xff;
        final int destinationPort = (segment[2] & 0xff) << 8 | segment[3] & 0xff;
        final int flags = segment[13] & 0xff;
        if (sourcePort != port || destinationPort != 40000) {
            return;
        }
        final long sequence = u32(segment, 4);
        final int dataOffset = (segment[12] & 0xf0) >> 2;
        final int length = segment.length - dataOffset;
        if ((flags & RST) != 0) {
            reset = true;
            return;
        }
        if ((flags & SYN) != 0 && (flags & ACK) != 0) {
            receiveSequence = sequence + 1;
            established = true;
            sendTcp(ACK, new byte[0], new byte[0]);
            return;
        }
        if (sequence != receiveSequence) {
            sendTcp(ACK, new byte[0], new byte[0]);
            return;
        }
        if (length > 0) {
            received.append(new String(segment, dataOffset, length, StandardCharsets.ISO_8859_1));
            receiveSequence += length;
        }
        if ((flags & FIN) != 0) {
            receiveSequence++;
            finished = true;
        }
        if (length > 0 || (flags & FIN) != 0) {
            sendTcp(ACK, new byte[0], new byte[0]);
        }
    }

    private void sendTcp(final int flags, final byte[] data, final byte[] options) throws IOException {
        final int headerLength = 20 + options.length;
        final byte[] segment = new byte[headerLength + data.length];
        segment[0] = (byte) (40000 >> 8);
        segment[1] = (byte) 40000;
        segment[2] = (byte) (port >> 8);
        segment[3] = (byte) port;
        put32(segment, 4, sendSequence);
        put32(segment, 8, (flags & ACK) != 0 ? receiveSequence : 0);
        segment[12] = (byte) (headerLength / 4 << 4);
        segment[13] = (byte) flags;
        segment[14] = 0x20;
        System.arraycopy(options, 0, segment, 20, options.length);
        System.arraycopy(data, 0, segment, headerLength, data.length);
        final byte[] pseudo = new byte[12 + segment.length];
        System.arraycopy(ip, 0, pseudo, 0, 4);
        System.arraycopy(guestIp, 0, pseudo, 4, 4);
        pseudo[9] = PROTO_TCP;
        pseudo[10] = (byte) (segment.length >> 8);
        pseudo[11] = (byte) segment.length;
        System.arraycopy(segment, 0, pseudo, 12, segment.length);
        final int sum = checksum(pseudo, 0, pseudo.length);
        segment[16] = (byte) (sum >> 8);
        segment[17] = (byte) sum;
        sendIp(PROTO_TCP, segment);
    }

    private void sendIp(final int protocol, final byte[] payload) throws IOException {
        final byte[] packet = new byte[20 + payload.length];
        packet[0] = 0x45;
        packet[2] = (byte) (packet.length >> 8);
        packet[3] = (byte) packet.length;
        packet[6] = 0x40;
        packet[8] = 64;
        packet[9] = (byte) protocol;
        System.arraycopy(ip, 0, packet, 12, 4);
        System.arraycopy(guestIp, 0, packet, 16, 4);
        final int sum = checksum(packet, 0, 20);
        packet[10] = (byte) (sum >> 8);
        packet[11] = (byte) sum;
        System.arraycopy(payload, 0, packet, 20, payload.length);
        if (guestMac == null) {
            throw new IllegalStateException("guest address is not resolved yet");
        }
        sendFrame(guestMac, 0x0800, packet);
    }

    private void sendFrame(final byte[] destination, final int type, final byte[] payload) throws IOException {
        final byte[] datagram = new byte[HEADER + ETHERNET + payload.length];
        datagram[0] = 0x08;
        datagram[4] = (byte) (vni >> 16);
        datagram[5] = (byte) (vni >> 8);
        datagram[6] = (byte) vni;
        System.arraycopy(destination, 0, datagram, HEADER, 6);
        System.arraycopy(mac, 0, datagram, HEADER + 6, 6);
        datagram[HEADER + 12] = (byte) (type >> 8);
        datagram[HEADER + 13] = (byte) type;
        System.arraycopy(payload, 0, datagram, HEADER + ETHERNET, payload.length);
        channel.send(ByteBuffer.wrap(datagram), tunnel);
    }

    private static void arpHeader(final byte[] arp, final int operation) {
        arp[1] = 1;
        arp[2] = 0x08;
        arp[4] = 6;
        arp[5] = 4;
        arp[7] = (byte) operation;
    }

    private static long u32(final byte[] data, final int offset) {
        return (data[offset] & 0xffL) << 24 | (data[offset + 1] & 0xffL) << 16
            | (data[offset + 2] & 0xffL) << 8 | data[offset + 3] & 0xffL;
    }

    private static void put32(final byte[] data, final int offset, final long value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }

    private static int checksum(final byte[] data, final int offset, final int length) {
        long sum = 0;
        for (int index = 0; index < length; index += 2) {
            sum += (data[offset + index] & 0xff) << 8 | (index + 1 < length ? data[offset + index + 1] & 0xff : 0);
        }
        while (sum >> 16 != 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        return (int) (~sum & 0xffff);
    }
}
