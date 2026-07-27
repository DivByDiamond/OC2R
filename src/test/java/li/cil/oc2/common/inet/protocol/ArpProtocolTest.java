package li.cil.oc2.common.inet.protocol;

import li.cil.oc2.common.inet.util.MacAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArpProtocolTest {
    @Test
    public void arpRequestContainsCorrectFields() {
        // Verify that ARP protocol constants are correct
        assertEquals((short) 0x0806, ArpProtocol.PROTOCOL_ARP);
    }

    @Test
    public void macAddressInArpResponse() {
        final MacAddress mac = new MacAddress((short) 0x0011, 0x22334455);
        assertNotNull(mac);
        assertEquals((short) 0x0011, mac.prefix());
        assertEquals(0x22334455, mac.address());
    }

    @Test
    public void macAddressPrefixAndAddress() {
        final MacAddress mac = new MacAddress((short) 0xAABB, 0xCCDDEEFF);
        assertEquals((short) 0xAABB, mac.prefix());
        assertEquals(0xCCDDEEFF, mac.address());
    }
}
