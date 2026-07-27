package li.cil.oc2.common.inet.util;

import li.cil.oc2.common.inet.util.checksum.AddressParseException;
import li.cil.oc2.common.inet.util.checksum.Rfc1071Checksum;
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
        final ByteBuffer body = InetUtils.quickICMPBody(0, 0);
        assertNotNull(body);
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
