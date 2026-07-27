package li.cil.oc2.common.inet;

import li.cil.oc2.common.inet.util.InetUtils;
import li.cil.oc2.common.inet.util.Ipv4Space;
import li.cil.oc2.common.inet.util.checksum.AddressParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Ipv4SpaceExtendedTest {
    @Test
    public void emptySpaceAllowsNothing() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("1.1.1.1")));
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("127.0.0.1")));
    }

    @Test
    public void singleHostAllow() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        space.put("10.0.0.1");
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.1")));
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.2")));
    }

    @Test
    public void subnetAllow() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        space.put("10.0.0.0/24");
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.1")));
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.254")));
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("10.0.1.1")));
    }

    @Test
    public void rangeAllow() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        space.put("10.0.0.1-10.0.0.10");
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.5")));
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.11")));
    }

    @Test
    public void denylistMode() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.DENYLIST);
        space.put("10.0.0.0/8");
        // In denylist mode, everything is allowed EXCEPT the listed ranges
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.1")));
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("192.168.1.1")));
    }

    @Test
    public void overlappingRanges() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        space.put("10.0.0.0/24");
        space.put("10.0.0.128/25"); // Overlaps with above
        // Should merge into single range
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.200")));
    }

    @Test
    public void computeIpSpaceWithDenylist() throws AddressParseException {
        // InetUtils.computeIpSpace only supports a single mode at a time
        // (either denylist OR allowlist, not both). Use a pure denylist here.
        final Ipv4Space space = InetUtils.computeIpSpace(
            List.of("10.0.0.0/8", "127.0.0.0/8"),
            List.of()
        );
        // Everything allowed except 10.x and 127.x
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("192.168.1.1")));
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.1")));
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("127.0.0.1")));
    }

    @Test
    public void computeIpSpaceRejectsBothModes() {
        // Specifying both denied and allowed hosts is rejected by design.
        assertThrows(
            IllegalArgumentException.class,
            () -> InetUtils.computeIpSpace(
                List.of("0.0.0.0/0"),
                List.of("10.0.0.0/8", "127.0.0.0/8")
            )
        );
    }

    @Test
    public void hostnameResolution() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        space.put("one.one.one.one"); // DNS resolution of 1.1.1.1
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("1.1.1.1")));
    }

    @Test
    public void cidr30Subnet() throws AddressParseException {
        final Ipv4Space space = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
        space.put("10.0.0.0/30");
        // Ipv4Space is a pure integer-range set: a /30 covers all four
        // addresses (10.0.0.0..10.0.0.3). It does not exclude the network
        // or broadcast addresses; that distinction is the network stack's
        // responsibility, not this class's.
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.0")));
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.1")));
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.2")));
        assertTrue(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.3")));
        // Address just outside the /30 must be rejected by the allowlist.
        assertFalse(space.isAllowed(InetUtils.parseIpv4Address("10.0.0.4")));
    }
}
