package li.cil.oc2.common.inet.session.echo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Asks the operating system's {@code ping} whether a host answers echo requests.
 *
 * <p>The JVM cannot send ICMP without a raw socket, and the native library needs the same privilege, which a
 * Minecraft server almost never runs with. The system {@code ping} gets by without it (setuid, file capability or
 * an unprivileged ICMP socket), so it is the dependable middle step between the native call and the JVM's
 * TCP-port-7 probe, which most hosts on the internet do not answer.
 *
 * <p>The only argument that varies is the textual form of an IPv4 address taken from four bytes, so nothing a
 * guest writes can reach the command line.
 */
public final class SystemPing {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int EXTRA_WAIT_MS = 1000;

    private static volatile boolean unavailable = false;

    private SystemPing() {}

    /** The command for one echo request with the given timeout, in the dialect of the operating system. */
    static List<String> command(final String osName, final String address, final int timeoutMs) {
        final String os = osName.toLowerCase(Locale.ROOT);
        final int seconds = Math.max(1, (timeoutMs + 999) / 1000);
        if (os.contains("win")) {
            return List.of("ping", "-n", "1", "-w", String.valueOf(timeoutMs), address);
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return List.of("ping", "-n", "-c", "1", "-t", String.valueOf(seconds), address);
        }
        return List.of("ping", "-n", "-c", "1", "-W", String.valueOf(seconds), address);
    }

    /** Whether the host answered; false when it did not or when there is no usable {@code ping} command. */
    public static boolean isReachable(final InetAddress address, final int timeoutMs) {
        if (unavailable || address.getAddress().length != 4) {
            return false;
        }
        try {
            return succeeds(command(System.getProperty("os.name", ""), address.getHostAddress(), timeoutMs),
                    timeoutMs + EXTRA_WAIT_MS);
        } catch (final IOException e) {
            unavailable = true;
            LOGGER.warn("No usable ping command ({}); echo requests use the JVM probe only", e.getMessage());
            return false;
        }
    }

    /**
     * Runs a command and reports whether it ended with exit status 0 within the time. A command that outlives
     * the time is killed. Throws if the program cannot be started at all, which is how a missing command shows.
     */
    static boolean succeeds(final List<String> command, final int waitMs) throws IOException {
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        final Process process = builder.start();
        try {
            return process.waitFor(waitMs, TimeUnit.MILLISECONDS) && process.exitValue() == 0;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
