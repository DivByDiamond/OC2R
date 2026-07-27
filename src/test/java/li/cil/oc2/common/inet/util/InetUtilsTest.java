package li.cil.oc2.common.inet.util;

import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.common.inet.util.checksum.AddressParseException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class InetUtilsTest {
    @Test
    public void parseIpv4AddressValid() throws AddressParseException {
        final int addr = InetUtils.parseIpv4Address("192.168.1.1");
        assertEquals("192.168.1.1", InetUtils.ipv4AddressToString(addr));
    }

    @Test
    public void parseIpv4AddressLoopback() throws AddressParseException {
        final int addr = InetUtils.parseIpv4Address("127.0.0.1");
        assertEquals("127.0.0.1", InetUtils.ipv4AddressToString(addr));
    }

    @Test
    public void parseIpv4AddressInvalid() {
        assertThrows(AddressParseException.class, () -> InetUtils.parseIpv4Address("not.an.ip"));
        assertThrows(AddressParseException.class, () -> InetUtils.parseIpv4Address("256.0.0.1"));
        assertThrows(AddressParseException.class, () -> InetUtils.parseIpv4Address("1.2.3"));
    }

    @Test
    public void quickICMPBody() {
        final ByteBuffer buffer = ByteBuffer.allocate(64);
        // Place an IPv4 header at FRAME_HEADER_SIZE. The low nibble of the first byte
        // encodes the IP header length in 32-bit words (IHL). 0x45 => IPv4, IHL=5 (20 bytes).
        buffer.put(LinkLocalLayer.FRAME_HEADER_SIZE, (byte) 0x45);

        final byte[] body = InetUtils.quickICMPBody(buffer);
        assertNotNull(body);
        // Body length = 4 (prefix) + IP header size (20) + 8 (transport header snippet)
        assertEquals(4 + 20 + 8, body.length);
        // The first 4 bytes are the ICMP "type/code" prefix injected by quickICMPBody.
        assertEquals(0x5, body[2]);
        assertEquals((byte) 0xDC, body[3]);
    }

    @Test
    public void ipv4AddressToStringAllZeroes() {
        assertEquals("0.0.0.0", InetUtils.ipv4AddressToString(0));
    }

    @Test
    public void ipv4AddressToStringMax() {
        assertEquals("255.255.255.255", InetUtils.ipv4AddressToString(0xFFFFFFFF));
    }
}
