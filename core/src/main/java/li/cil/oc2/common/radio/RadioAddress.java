package li.cil.oc2.common.radio;

import java.util.Locale;

/** Display address; possession conveys no authority. */
public record RadioAddress(int network, int device) {
    public static final int MAX = (1 << 20) - 1;
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    public RadioAddress {
        if (network < 1 || network > MAX || device < 1 || device > MAX) {
            throw new IllegalArgumentException("Reserved or out-of-range radio address");
        }
    }

    public static RadioAddress parse(final String input) {
        if (input == null || input.length() != 9 || input.charAt(4) != ':') {
            throw new IllegalArgumentException("Expected XXXX:XXXX");
        }
        if (input.chars().anyMatch(character -> character > 127)) {
            throw new IllegalArgumentException("Radio addresses use ASCII characters");
        }
        final String value = input.toUpperCase(Locale.ROOT);
        return new RadioAddress(decode(value.substring(0, 4)), decode(value.substring(5)));
    }

    private static int decode(final String value) {
        int result = 0;
        for (int i = 0; i < value.length(); i++) {
            final int digit = ALPHABET.indexOf(value.charAt(i));
            if (digit < 0) throw new IllegalArgumentException("Invalid radio address alphabet");
            result = (result << 5) | digit;
        }
        return result;
    }

    private static String encode(final int value) {
        final char[] chars = new char[4];
        for (int i = 0; i < 4; i++) chars[3 - i] = ALPHABET.charAt((value >>> (i * 5)) & 31);
        return new String(chars);
    }

    @Override
    public String toString() {
        return encode(network) + ":" + encode(device);
    }
}
