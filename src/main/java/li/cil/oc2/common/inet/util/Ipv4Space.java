package li.cil.oc2.common.inet.util;

import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.RegEx;
import li.cil.oc2.common.util.misc.IntegerSpace;

public final class Ipv4Space extends IntegerSpace {

    private static final String IPADDRESS_PATTERN =
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}";
    public static final Pattern ipAddressPattern = line(group("ip", IPADDRESS_PATTERN));
    private static final Pattern ipRangePattern =
            line(group("start", IPADDRESS_PATTERN) + "-" + group("end", IPADDRESS_PATTERN));
    private static final Pattern subnetPattern =
            line(group("ip", IPADDRESS_PATTERN) + "\\/" + group("prefix", "[1-9]\\d?"));
    private static final Pattern interfaceNamePattern = line("@" + group("name", "[a-zA-Z].*"));
    private static final Pattern interfaceIdPattern = line("@" + group("id", "\\d*"));
    private final boolean isAllowListMode;

    public Ipv4Space(final Modes mode) {
        super();
        isAllowListMode = mode == Modes.ALLOWLIST;
    }

    private static Pattern line(@RegEx final String pattern) {
        return Pattern.compile('^' + pattern + '$');
    }

    private static String group(final String name, @RegEx final String pattern) {
        return "(?<" + name + ">" + pattern + ")";
    }

    @Override
    protected void elementToString(final StringBuilder builder, final int element) {
        InetUtils.ipv4AddressToString(builder, element);
    }

    private boolean putSubnet(final int ipAddress, final int prefix) {
        final int subnet = InetUtils.getSubnetByPrefix(prefix);
        final int rangeStart = ipAddress & subnet;
        final int rangeEnd = ipAddress | ~subnet;
        return put(rangeStart, rangeEnd);
    }

    private boolean putNetworkInterface(@Nullable final NetworkInterface networkInterface) {
        if (networkInterface == null) {
            throw new IllegalArgumentException("Network interface not found");
        }
        boolean result = false;
        for (final InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            final InetAddress inetAddress = address.getAddress();
            if (inetAddress instanceof Inet4Address) {
                final int ipAddress =
                        InetUtils.javaInetAddressToIpAddress((Inet4Address) inetAddress);
                result = putSubnet(ipAddress, address.getNetworkPrefixLength()) || result;
            }
        }
        return result;
    }

    public boolean put(final String string) {
        final Matcher ipAddressMatch = ipAddressPattern.matcher(string);
        if (ipAddressMatch.matches()) {
            return putIpAddress(ipAddressMatch.group("ip"));
        }

        final Matcher ipRangeMatch = ipRangePattern.matcher(string);
        if (ipRangeMatch.matches()) {
            return putRange(ipRangeMatch.group("start"), ipRangeMatch.group("end"));
        }

        final Matcher subnetMatch = subnetPattern.matcher(string);
        if (subnetMatch.matches()) {
            return putSubnet(subnetMatch.group("ip"), subnetMatch.group("prefix"));
        }

        final Matcher interfaceNameMatch = interfaceNamePattern.matcher(string);
        if (interfaceNameMatch.matches()) {
            return putNetworkInterfaceByName(interfaceNameMatch.group("name"));
        }

        final Matcher interfaceIdMatch = interfaceIdPattern.matcher(string);
        if (interfaceIdMatch.matches()) {
            return putNetworkInterfaceById(interfaceIdMatch.group("id"));
        }

        // Assume it is a hostname
        return putHostname(string);
    }

    private boolean putIpAddress(final String ip) {
        final int ipAddress = InetUtils.surelyParseValidIpv4Address(ip);
        return put(ipAddress);
    }

    private boolean putRange(final String start, final String end) {
        final int rangeStart = InetUtils.surelyParseValidIpv4Address(start);
        final int rangeEnd = InetUtils.surelyParseValidIpv4Address(end);
        return put(rangeStart, rangeEnd);
    }

    private boolean putSubnet(final String ip, final String prefix) {
        final int ipAddress = InetUtils.surelyParseValidIpv4Address(ip);
        final int prefixValue = Integer.parseInt(prefix);
        return putSubnet(ipAddress, prefixValue);
    }

    private boolean putNetworkInterfaceByName(final String name) {
        try {
            final NetworkInterface networkInterface = NetworkInterface.getByName(name);
            return putNetworkInterface(networkInterface);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get a network interface by name", e);
        }
    }

    private boolean putNetworkInterfaceById(final String id) {
        final int interfaceId = Integer.parseInt(id);
        try {
            final NetworkInterface networkInterface = NetworkInterface.getByIndex(interfaceId);
            return putNetworkInterface(networkInterface);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get a network interface by index", e);
        }
    }

    private boolean putHostname(final String hostname) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(hostname);
            boolean result = false;
            for (final InetAddress address : addresses) {
                if (address instanceof Inet4Address) {
                    final int ipAddress =
                            InetUtils.javaInetAddressToIpAddress((Inet4Address) address);
                    result = put(ipAddress) || result;
                }
            }
            return result;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public boolean isAllowed(final int ipAddress) {
        return isAllowListMode == contains(ipAddress);
    }

    public enum Modes {
        ALLOWLIST,
        DENYLIST,
    }
}