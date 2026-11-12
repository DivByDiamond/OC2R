package li.cil.oc2.common.inet.session.echo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemPingTest {
    private static final String ADDRESS = "192.0.2.7";
    private static final String JAVA = System.getProperty("java.home") + "/bin/java";

    @Test
    void linuxAsksForOneEchoAndWaitsWholeSeconds() {
        assertEquals(List.of("ping", "-n", "-c", "1", "-W", "2", ADDRESS), SystemPing.command("Linux", ADDRESS, 1500));
        assertEquals(List.of("ping", "-n", "-c", "1", "-W", "1", ADDRESS), SystemPing.command("Linux", ADDRESS, 1));
    }

    @Test
    void macAndWindowsUseTheirOwnOptions() {
        assertEquals(List.of("ping", "-n", "-c", "1", "-t", "1", ADDRESS), SystemPing.command("Mac OS X", ADDRESS, 1000));
        assertEquals(List.of("ping", "-n", "1", "-w", "1000", ADDRESS), SystemPing.command("Windows 11", ADDRESS, 1000));
    }

    @Test
    void theCommandLineOnlyContainsTheAddressBesidesFixedOptions() {
        final List<String> linux = SystemPing.command("Linux", "10.1.2.3", 1000);
        assertEquals("10.1.2.3", linux.get(linux.size() - 1));
        assertEquals("ping", linux.get(0));
        final List<String> windows = SystemPing.command("Windows 11", "10.1.2.3", 1000);
        assertEquals("10.1.2.3", windows.get(windows.size() - 1));
        final List<String> mac = SystemPing.command("Mac OS X", "10.1.2.3", 1000);
        assertEquals("10.1.2.3", mac.get(mac.size() - 1));
    }

    @Test
    void aCommandThatEndsWithStatusZeroSucceeds() throws IOException {
        assertTrue(SystemPing.succeeds(List.of(JAVA, "-version"), 20_000));
    }

    @Test
    void aCommandThatEndsWithAnErrorStatusFails() throws IOException {
        assertFalse(SystemPing.succeeds(List.of(JAVA, "--no-such-option"), 20_000));
    }

    @Test
    void aCommandThatCannotBeStartedIsReportedAsMissing() {
        assertThrows(IOException.class, () -> SystemPing.succeeds(List.of("no-such-ping-command-oc2r"), 1000));
    }

    @Test
    void aSilentHostIsNotReachable() throws Exception {
        assertFalse(SystemPing.isReachable(InetAddress.getByAddress(new byte[] {(byte) 192, 0, 2, 1}), 300));
    }

    @Test
    void ipv6AddressesAreNotPingedWithTheIpv4Command() throws Exception {
        assertFalse(SystemPing.isReachable(InetAddress.getByName("::1"), 300));
    }
}
