package li.cil.oc2.common.inet.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.common.inet.layer.LayerParametersImpl;
import li.cil.oc2.common.inet.layer.NullLayer;
import li.cil.oc2.common.inet.util.checksum.AddressParseException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class InetUtils {
    private static InetAddress getInetAddressByBytes(final byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            throw new Error("unreachable", e);
        }
    }

    public static InetAddress toJavaInetAddress(final int ipAddress) {
        final byte[] bytes = {
            (byte) (ipAddress >>> 24),
            (byte) (ipAddress >>> 16),
            (byte) (ipAddress >>> 8),
            (byte) ipAddress
        };
        return getInetAddressByBytes(bytes);
    }

    private static void fillLong(final byte[] destination, final int offset, final long value) {
        for (int position = 0; position < 8; ++position) {
            destination[offset + position] = (byte) (value >>> ((7 - position) << 3));
        }
    }

    public static InetAddress toJavaInetAddress(
            final long ipAddressMost, final long ipAddressLeast) {
        final byte[] bytes = new byte[16];
        fillLong(bytes, 0, ipAddressMost);
        fillLong(bytes, 8, ipAddressLeast);
        return getInetAddressByBytes(bytes);
    }

    public static void ipv4AddressToString(final StringBuilder builder, final int ipAddress) {
        builder.append(Integer.toUnsignedString(ipAddress >>> 24))
                .append('.')
                .append(Integer.toUnsignedString((ipAddress >>> 16) & 0xFF))
                .append('.')
                .append(Integer.toUnsignedString((ipAddress >>> 8) & 0xFF))
                .append('.')
                .append(Integer.toUnsignedString(ipAddress & 0xFF));
    }

    public static String ipv4AddressToString(final int ipAddress) {
        final StringBuilder stringBuilder = new StringBuilder();
        ipv4AddressToString(stringBuilder, ipAddress);
        return stringBuilder.toString();
    }

    public static void socketAddressToString(
            final StringBuilder builder, final int ipAddress, final short port) {
        ipv4AddressToString(builder, ipAddress);
        builder.append(':').append(Short.toUnsignedInt(port));
    }

    public static byte[] quickICMPBody(final ByteBuffer data) {
        final int tmpPosition = data.position();
        final int tmpLimit = data.limit();
        data.limit(data.capacity());
        data.position(LinkLocalLayer.FRAME_HEADER_SIZE);
        final int headerSize = (data.get() & 0xF) * 4;
        data.position(LinkLocalLayer.FRAME_HEADER_SIZE);
        data.limit(LinkLocalLayer.FRAME_HEADER_SIZE + headerSize + 8);
        final byte[] result = new byte[data.remaining() + 4];
        result[2] = 0x5;
        result[3] = (byte) 0xDC;
        // Copy the quoted header+payload INTO the result. put() here would overwrite
        // the original packet with zero bytes and return an empty quote instead.
        data.get(result, 4, data.remaining());
        data.limit(tmpLimit);
        data.position(tmpPosition);
        return result;
    }

    public static int javaInetAddressToIpAddress(final Inet4Address address) {
        final byte[] bytes = address.getAddress();
        return (Byte.toUnsignedInt(bytes[0]) << 24)
                | (Byte.toUnsignedInt(bytes[1]) << 16)
                | (Byte.toUnsignedInt(bytes[2]) << 8)
                | Byte.toUnsignedInt(bytes[3]);
    }

    public static int indexOf(final CharSequence string, final char character, final int start) {
        final int length = string.length();
        for (int i = start; i < length; ++i) {
            if (string.charAt(i) == character) {
                return i;
            }
        }
        return -1;
    }

    public static int surelyParseValidIpv4Address(final CharSequence string) {
        int position = 0;
        int address = 0;
        for (int i = 0; i < 3; ++i) {
            final int segmentEnd = indexOf(string, '.', position);
            address = (address << 8) | Integer.parseUnsignedInt(string, position, segmentEnd, 10);
            position = segmentEnd + 1;
        }
        return (address << 8) | Integer.parseUnsignedInt(string, position, string.length(), 10);
    }

    public static int parseIpv4Address(final CharSequence string) throws AddressParseException {
        if (!Ipv4Space.ipAddressPattern.matcher(string).matches()) {
            throw new AddressParseException("Not an IPv4 address: " + string);
        }
        return surelyParseValidIpv4Address(string);
    }

    public static int getSubnetByPrefix(final int prefix) {
        if (prefix > 30 || prefix < 0) {
            throw new IllegalArgumentException("Wrong subnet prefix range");
        }
        return -1 << (32 - prefix);
    }

    private static void configureIpSpace(final Ipv4Space ipSpace, final List<String> hosts) {
        int i = 1;
        for (final String hostString : hosts) {
            final String rangeString = hostString.trim();
            if (rangeString.isEmpty()) {
                continue;
            }
            try {
                ipSpace.put(rangeString);
            } catch (final Exception e) {
                throw new IllegalArgumentException(
                        "Failed to parse IPv4 address range #" + i + ": " + e.getMessage(), e);
            }
            ++i;
        }
    }

    public static Ipv4Space computeIpSpace(
            final List<String> deniedHosts, final List<String> allowedHosts) {
        final boolean deniedHostsIsEmpty = deniedHosts.isEmpty();
        final boolean allowedHostsIsEmpty = allowedHosts.isEmpty();
        if (deniedHostsIsEmpty && allowedHostsIsEmpty) {
            return new Ipv4Space(Ipv4Space.Modes.DENYLIST);
        } else if (allowedHostsIsEmpty) {
            final Ipv4Space ipSpace = new Ipv4Space(Ipv4Space.Modes.DENYLIST);
            configureIpSpace(ipSpace, deniedHosts);
            return ipSpace;
        } else if (deniedHostsIsEmpty) {
            final Ipv4Space ipSpace = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
            configureIpSpace(ipSpace, allowedHosts);
            return ipSpace;
        } else {
            throw new IllegalArgumentException("Both denied and allowed hosts are specified");
        }
    }

    public static <P, C> P createLayerIfNotStub(
            final C currentLayer, final Function<C, P> getNextLayer) {
        if (currentLayer == NullLayer.INSTANCE) {
            @SuppressWarnings("unchecked")
            final P result = (P) NullLayer.INSTANCE;
            return result;
        } else {
            return getNextLayer.apply(currentLayer);
        }
    }

    public static LayerParameters nextLayerParameters(
            final LayerParameters layerParameters, final String layerName) {
        final Optional<Tag> nextLayerState =
                layerParameters
                        .getSavedState()
                        .flatMap(
                                currentLayerState ->
                                        (currentLayerState instanceof CompoundTag tag)
                                                ? Optional.ofNullable(tag.get(layerName))
                                                : Optional.empty());
        return new LayerParametersImpl(nextLayerState, layerParameters.getInternetManager());
    }
}