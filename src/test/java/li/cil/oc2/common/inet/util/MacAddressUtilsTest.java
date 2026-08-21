package li.cil.oc2.common.inet.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import li.cil.oc2.common.inet.util.checksum.AddressParseException;
import org.junit.jupiter.api.Test;

final class MacAddressUtilsTest {
    @Test
    void parseAndFormatRoundTrip() throws AddressParseException {
        final MacAddress macAddress = MacAddressUtils.parseMacAddress("5E:D1:0A:00:12:FF");
        assertEquals((short) 0x5ED1, macAddress.prefix());
        assertEquals(0x0A0012FF, macAddress.address());
        assertEquals("5E:D1:0A:00:12:FF", MacAddressUtils.macAddressToString(macAddress));
    }

    @Test
    void parseAcceptsLowercaseHex() throws AddressParseException {
        final MacAddress macAddress = MacAddressUtils.parseMacAddress("5e:d1:0a:00:12:ff");
        assertEquals("5E:D1:0A:00:12:FF", MacAddressUtils.macAddressToString(macAddress));
    }

    @Test
    void formatZeroAndBroadcastAddresses() throws AddressParseException {
        final MacAddress zero = new MacAddress((short) 0, 0);
        assertEquals("00:00:00:00:00:00", MacAddressUtils.macAddressToString(zero));
        final MacAddress broadcast = MacAddressUtils.parseMacAddress("FF:FF:FF:FF:FF:FF");
        assertEquals("FF:FF:FF:FF:FF:FF", MacAddressUtils.macAddressToString(broadcast));
    }

    @Test
    void parseRejectsWrongLength() {
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5E:D1"));
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress(""));
        assertThrows(
                AddressParseException.class,
                () -> MacAddressUtils.parseMacAddress("5E:D1:0A:00:12:FF:00"));
    }

    @Test
    void parseRejectsIllegalDelimiter() {
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5E-D1-0A-00-12-FF"));
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5ED10A0012FF"));
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5E:D1:0A:00:12 FF"));
    }

    @Test
    void parseRejectsIllegalCharacters() {
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5E:D1:0A:00:12:FZ"));
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5E:D1:0A:00:1 2:FF"));
        assertThrows(AddressParseException.class, () -> MacAddressUtils.parseMacAddress("5E:D1:0A:GG:12:FF"));
    }

    @Test
    void builderOverloadMatchesSimpleOverload() throws AddressParseException {
        final MacAddress macAddress = MacAddressUtils.parseMacAddress("5E:D1:0A:00:12:FF");
        final StringBuilder builder = new StringBuilder();
        MacAddressUtils.macAddressToString(builder, macAddress);
        assertEquals(MacAddressUtils.macAddressToString(macAddress), builder.toString());
    }
}
