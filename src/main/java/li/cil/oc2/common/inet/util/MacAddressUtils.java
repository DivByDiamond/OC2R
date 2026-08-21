package li.cil.oc2.common.inet.util;

import li.cil.oc2.common.inet.util.checksum.AddressParseException;

public final class MacAddressUtils {
    private static char hexCodeToChar(final int code) {
        if (code < 10) {
            return (char) ('0' + code);
        } else {
            return (char) ('A' + code - 10);
        }
    }

    private static void byteToHex(final StringBuilder builder, final byte code) {
        builder.append(hexCodeToChar((code & 0xFF) >>> 4))
                .append(hexCodeToChar(code & 15));
    }

    public static void macAddressToString(final StringBuilder builder, final MacAddress macAddress) {
        final short prefix = macAddress.prefix();
        final int address = macAddress.address();
        byteToHex(builder, (byte) (prefix >>> 8));
        builder.append(':');
        byteToHex(builder, (byte) prefix);
        for (int i = 3; i >= 0; --i) {
            builder.append(':');
            byteToHex(builder, (byte) (address >>> (8 * i)));
        }
    }

    public static String macAddressToString(final MacAddress macAddress) {
        final StringBuilder builder = new StringBuilder();
        macAddressToString(builder, macAddress);
        return builder.toString();
    }

    private static int hexCodeToInt(final char code) throws AddressParseException {
        if (code >= '0' && code <= '9') {
            return code - '0';
        } else if (code >= 'a' && code <= 'f') {
            return code - 'a' + 10;
        } else if (code >= 'A' && code <= 'F') {
            return code - 'A' + 10;
        } else {
            throw new AddressParseException("Illegal character '" + code + "' in address");
        }
    }

    private static byte parseMacAddressByte(final CharSequence string, final int start)
            throws AddressParseException {
        return (byte)
                ((hexCodeToInt(string.charAt(start)) << 4)
                        | hexCodeToInt(string.charAt(start + 1)));
    }

    private static AddressParseException illegalDelimiter(
            final CharSequence string, final int index) {
        final char illegal = string.charAt(index);
        return new AddressParseException(
                "Illegal character '"
                        + illegal
                        + "' at index "
                        + index
                        + " in MAC address \""
                        + string
                        + "\"");
    }

    public static MacAddress parseMacAddress(final CharSequence string) throws AddressParseException {
        if (string.length() != 17) {
            throw new AddressParseException(
                    "MAC address length must be 17 characters: \"" + string + "\"");
        }
        final byte first = parseMacAddressByte(string, 0);
        if (string.charAt(2) != ':') {
            throw illegalDelimiter(string, 2);
        }
        // Mask with 0xFF: parseMacAddressByte returns a sign-extended byte, and an
        // unmasked byte >= 0x80 would poison the upper bits of the combined value.
        final short prefix = (short)
                ((first & 0xFF) << 8 | (parseMacAddressByte(string, 3) & 0xFF));
        int address = 0;
        for (int i = 0; i < 4; ++i) {
            final int pos = i * 3 + 5;
            if (string.charAt(pos) != ':') {
                throw illegalDelimiter(string, pos);
            }
            address = (address << 8) | (parseMacAddressByte(string, pos + 1) & 0xFF);
        }
        return new MacAddress(prefix, address);
    }
}
