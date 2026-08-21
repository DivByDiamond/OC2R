package li.cil.oc2.common.inet.layer.impl;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import li.cil.oc2.api.inet.*;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.api.inet.layer.TransportLayer;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import li.cil.oc2.common.inet.util.checksum.Rfc1071Checksum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class DefaultNetworkLayer implements NetworkLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Random random = new Random();

    private static final int IPv4_HEADER_SIZE = 20;
    private static final int IPv4_VERSION = 4; // obviously...

    private static final byte IP_PROTOCOL_ICMP = 1;
    private static final byte IPv4_DEFAULT_TTL = 64;
    private static final byte ICMP_TYPE_DEST_UNREACHABLE = 3;
    private static final byte ICMP_CODE_FRAG_NEEDED = 4;
    private static final byte ICMP_TYPE_TIME_EXCEEDED = 11;
    private static final byte ICMP_CODE_TTL_EXPIRED = 0;
    private static final short ICMP_ERROR_NEXT_HOP_MTU = 1500;
    private static final int ICMP_HEADER_SIZE = 8;
    private static final int ICMP_QUOTED_PAYLOAD_SIZE = 8; // RFC 792: original header + 8 bytes

    // Single-slot pending ICMP error (same pattern as the ARP reply in DefaultLinkLocalLayer):
    // queued on the guest->world send path, delivered to the guest on the next receive poll.
    @Nullable private byte[] pendingIcmpError;

    private final TransportLayer transportLayer;

    private final TransportMessage inMessage = new TransportMessage();
    private final TransportMessage outMessage = new TransportMessage();

    private final InternetManagerImpl internetManager;

    public DefaultNetworkLayer(
            final LayerParameters layerParameters, final TransportLayer transportLayer) {
        this.internetManager = (InternetManagerImpl) layerParameters.getInternetManager();
        this.transportLayer = transportLayer;
    }

    @Override
    public Optional<Tag> onSave() {
        return transportLayer
                .onSave()
                .map(
                        transportLayerState -> {
                            final CompoundTag networkLayerState = new CompoundTag();
                            networkLayerState.put(TransportLayer.LAYER_NAME, transportLayerState);
                            return networkLayerState;
                        });
    }

    @Override
    public void onStop() {
        transportLayer.onStop();
    }

    @Override
    public short receivePacket(final ByteBuffer packet) {
        if (pendingIcmpError != null) {
            final byte[] error = pendingIcmpError;
            pendingIcmpError = null;
            if (error.length <= packet.remaining()) {
                final int start = packet.position();
                packet.put(error);
                packet.position(start);
                return PROTOCOL_IPv4;
            }
        }
        // Try to receive something
        packet.position(packet.position() + IPv4_HEADER_SIZE);
        inMessage.initializeBuffer(packet);
        final byte protocol = transportLayer.receiveTransportMessage(inMessage);
        if (protocol == TransportLayer.PROTOCOL_NONE || !inMessage.isIpv4()) {
            return PROTOCOL_NONE;
        }

        // Prepare IP packet
        final int srcIpAddress = inMessage.getSrcIpv4Address();
        final int dstIpAddress = inMessage.getDstIpv4Address();
        final int bodySize = packet.remaining();

        packet.position(packet.position() - IPv4_HEADER_SIZE);
        packet.put((byte) ((IPv4_VERSION << 4) | 5));
        packet.put((byte) 0);
        packet.putShort((short) (IPv4_HEADER_SIZE + bodySize));
        packet.putShort((short) random.nextInt());
        packet.putShort((short) 0);
        packet.put(inMessage.getTtl());
        packet.put(protocol);
        packet.putShort((short) 0);
        packet.putInt(srcIpAddress);
        packet.putInt(dstIpAddress);

        // Calculate header checksum
        packet.position(packet.position() - IPv4_HEADER_SIZE);
        short checksum = Rfc1071Checksum.rfc1071Checksum(packet, IPv4_HEADER_SIZE);
        packet.position(packet.position() - 10);
        packet.putShort(checksum);
        packet.position(packet.position() + 8 - IPv4_HEADER_SIZE);

        return PROTOCOL_IPv4;
    }

    @Override
    public void sendPacket(final short protocol, final ByteBuffer packet) {
        if (protocol != PROTOCOL_IPv4) {
            LOGGER.trace("Unsupported network protocol");
            return;
        }
        if (packet.remaining() < IPv4_HEADER_SIZE) {
            LOGGER.trace("IP header is too small");
            return;
        }

        final IpPacketHeader header = parseIpPacketHeader(packet);
        if (header == null) {
            return;
        }

        /// Next layer
        LOGGER.trace("Transport message received");
        outMessage.initializeBuffer(packet);
        outMessage.updateIpv4(header.srcIpAddress, header.dstIpAddress, header.ttl);
        transportLayer.sendTransportMessage(header.transportProtocol, outMessage);
    }

    // bytesRead = how many header bytes parseIpPacketHeader consumed before the drop, so the
    // start of the original IP header is at (position - bytesRead).
    private void queueIcmpError(
            final ByteBuffer packet, final int bytesRead, final byte type, final byte code) {
        if (pendingIcmpError != null) {
            return; // one slot; the newest error wins only if none is queued yet
        }
        try {
            final int headerStart = packet.position() - bytesRead;
            final int ipHeaderSize = (packet.get(headerStart) & 0xF) * 4;
            final int totalLength = Short.toUnsignedInt(packet.getShort(headerStart + 2));
            if (ipHeaderSize < IPv4_HEADER_SIZE
                    || totalLength < ipHeaderSize
                    || headerStart + IPv4_HEADER_SIZE > packet.limit()) {
                return;
            }
            final int packetEnd = Math.min(headerStart + totalLength, packet.limit());
            final int quotedSize = Math.min(ipHeaderSize + ICMP_QUOTED_PAYLOAD_SIZE, packetEnd - headerStart);
            final int icmpSize = ICMP_HEADER_SIZE + quotedSize;

            final ByteBuffer error = ByteBuffer.allocate(IPv4_HEADER_SIZE + icmpSize);
            error.put((byte) ((IPv4_VERSION << 4) | 5));
            error.put((byte) 0);
            error.putShort((short) (IPv4_HEADER_SIZE + icmpSize));
            error.putShort((short) random.nextInt());
            error.putShort((short) 0);
            error.put(IPv4_DEFAULT_TTL);
            error.put(IP_PROTOCOL_ICMP);
            error.putShort((short) 0);
            error.putInt(packet.getInt(headerStart + 16)); // source = original destination
            error.putInt(packet.getInt(headerStart + 12)); // destination = original source

            final int icmpStart = error.position();
            error.put(type);
            error.put(code);
            error.putShort((short) 0);
            if (code == ICMP_CODE_FRAG_NEEDED) {
                error.putShort((short) 0);
                error.putShort(ICMP_ERROR_NEXT_HOP_MTU);
            } else {
                error.putInt(0);
            }
            final int savedLimit = packet.limit();
            packet.limit(headerStart + quotedSize).position(headerStart);
            while (packet.hasRemaining()) {
                error.put(packet.get());
            }
            packet.limit(savedLimit);
            packet.position(headerStart + bytesRead);

            error.position(icmpStart);
            final short icmpChecksum = Rfc1071Checksum.rfc1071Checksum(error, icmpSize);
            error.putShort(icmpStart + 2, icmpChecksum);

            error.position(icmpStart - IPv4_HEADER_SIZE);
            final short ipChecksum = Rfc1071Checksum.rfc1071Checksum(error, IPv4_HEADER_SIZE);
            error.putShort(icmpStart - IPv4_HEADER_SIZE + 10, ipChecksum);

            error.position(icmpStart - IPv4_HEADER_SIZE);
            pendingIcmpError = new byte[error.remaining()];
            error.get(pendingIcmpError);
        } catch (final IllegalArgumentException | IndexOutOfBoundsException exception) {
            LOGGER.trace("Failed to build ICMP error", exception);
        }
    }

    private IpPacketHeader parseIpPacketHeader(final ByteBuffer packet) {
        final byte versionAndIhl = packet.get();
        if ((versionAndIhl >>> 4) != IPv4_VERSION) {
            LOGGER.trace("Invalid protocol version");
            return null;
        }
        final int headerSize = (versionAndIhl & 0xF) * 4;
        if (headerSize < IPv4_HEADER_SIZE || packet.remaining() < headerSize) {
            LOGGER.trace("Invalid header size");
            return null;
        }
        packet.get(); // too hard, ignore
        int messageLength = Short.toUnsignedInt(packet.getShort());
        if (packet.remaining() + 4 < messageLength) {
            LOGGER.trace("Packet size is lower than IP message size");
            return null;
        }
        packet.getShort(); // normally, we don't expect message to be fragmented
        short flagsAndFragmentOffset = packet.getShort();
        if (((flagsAndFragmentOffset >>> 13) & 0b101) != 0) {
            LOGGER.trace("Fragmented packet prohibited (1)");
            queueIcmpError(packet, 8, ICMP_TYPE_DEST_UNREACHABLE, ICMP_CODE_FRAG_NEEDED);
            return null; // no fragments!
        }
        if ((flagsAndFragmentOffset & 0x1FFF) != 0) {
            LOGGER.trace("Fragmented packet prohibited (2)");
            queueIcmpError(packet, 8, ICMP_TYPE_DEST_UNREACHABLE, ICMP_CODE_FRAG_NEEDED);
            return null; // no fragments!
        }
        byte ttl = (byte) (packet.get() - 1);
        if (ttl == 0) {
            LOGGER.trace("Small TTL value");
            queueIcmpError(packet, 9, ICMP_TYPE_TIME_EXCEEDED, ICMP_CODE_TTL_EXPIRED);
            return null;
        }
        byte transportProtocol = packet.get();
        packet.getShort(); // I don't think, that we should expect packet corruption in Minecraft
        int srcIpAddress = packet.getInt();
        int dstIpAddress = packet.getInt();
        if (!internetManager.isAllowedToConnect(dstIpAddress)) {
            // Silent drop on purpose: deniedHosts is a security filter, the blocked
            // destination must not learn anything about our host via ICMP errors.
            LOGGER.trace("Forbidden IP address");
            return null;
        }
        packet.position(packet.position() + headerSize - IPv4_HEADER_SIZE); // skip options
        packet.limit(packet.position() + messageLength - headerSize); // set correct limit
        return new IpPacketHeader(transportProtocol, srcIpAddress, dstIpAddress, ttl);
    }

    private record IpPacketHeader(
            byte transportProtocol, int srcIpAddress, int dstIpAddress, byte ttl) {}
}