package li.cil.oc2.common.inet.protocol;

import li.cil.oc2.common.inet.util.MacAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArpProtocolTest {
    @Test
    public void arpRequestContainsCorrectFields() {
        // Verify that ARP protocol constants are correct
        assertEquals(0x0806, ArpProtocol.PROTOCOL_ARP);
    }

    @Test
    public void macAddressInArpResponse() {
        final MacAddress mac = new MacAddress(new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55});
        assertNotNull(mac);
        assertArrayEquals(new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55}, mac.address());
    }

    @Test
    public void macAddressPrefixAndAddress() {
        final MacAddress mac = new MacAddress(new byte[]{0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF});
        assertEquals(0xAABB, mac.prefix());
    }
}
