package li.cil.oc2.common.inet.session.echo;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.DefaultSessionLayer;
import li.cil.oc2.common.setup.NativeLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class EchoHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Executor executor =
            Executors.newSingleThreadExecutor(
                    runnable -> new Thread(runnable, "internet/blocking-session"));

    private static final int MAX_PENDING_RESPONSES = 64;

    private static volatile boolean warnedAboutIcmpFallback = false;

    /** Replies waiting for the internet thread; a burst of pings must not overwrite each other. */
    public static Queue<EchoResponse> newResponseQueue() {
        return new java.util.concurrent.ArrayBlockingQueue<>(MAX_PENDING_RESPONSES);
    }

    public static boolean deliverPendingResponse(
            final Queue<EchoResponse> echoResponse,
            final SessionLayer.Receiver receiver) {
        final EchoResponse pending = echoResponse.poll();
        if (pending != null) {
            final ByteBuffer data = receiver.receive(pending.session);
            assert data != null;
            data.put(pending.payload);
            data.flip();
            return true;
        }
        return false;
    }

    public static boolean handleEchoSession(
            final Session session,
            @Nullable final ByteBuffer data,
            final Queue<EchoResponse> echoResponse) {
        if (!(session instanceof final EchoSession echoSession)) {
            return false;
        }
        if (data == null) {
            return true;
        }
        final InetAddress address = session.getDestination().getAddress();
        final int size = data.remaining();
        final byte[] payload = new byte[size];
        data.get(payload);
        // Both paths perform blocking syscalls (native sendICMP or isReachable), so
        // they must run on the dedicated blocking executor: the single "Internet"
        // thread drives all network cards and TCP/UDP sessions of the server, and a
        // ping to an unreachable host would otherwise stall it for the whole timeout.
        // Responses are delivered asynchronously via deliverPendingResponse either way.
        executor.execute(() -> processEcho(echoSession, address, payload, size, echoResponse));
        return true;
    }

    private static void processEcho(
            final EchoSession session,
            final InetAddress address,
            final byte[] payload,
            final int size,
            final Queue<EchoResponse> echoResponse) {
        // Native raw socket first (needs privileges), then the system ping command, then the JVM probe.
        if (NativeLoader.isLoadedLibrary()) {
            final byte[] responseData = DefaultSessionLayer.sendICMP(
                    address.getAddress(), payload, size, Config.defaultEchoRequestTimeoutMs);
            if (responseData != null) {
                echoResponse.offer(new EchoResponse(ByteBuffer.wrap(responseData), session));
                return;
            }
        }
        if (SystemPing.isReachable(address, Config.defaultEchoRequestTimeoutMs)
                || reachableByJvm(address, session)) {
            // The reply carries the data of the request, as an echo reply must.
            echoResponse.offer(new EchoResponse(ByteBuffer.wrap(payload), session));
        }
    }

    private static boolean reachableByJvm(final InetAddress address, final EchoSession session) {
        try {
            if (address.isReachable(null, session.getTtl(), Config.defaultEchoRequestTimeoutMs)) {
                return true;
            }
            warnAboutIcmpFallbackOnce(null);
        } catch (final IOException e) {
            warnAboutIcmpFallbackOnce(e);
            LOGGER.error("Failed to get echo response", e);
        }
        return false;
    }

    private static void warnAboutIcmpFallbackOnce(@Nullable final IOException e) {
        if (warnedAboutIcmpFallback) {
            return;
        }
        warnedAboutIcmpFallback = true;
        LOGGER.warn(
                "ICMP fallback via InetAddress.isReachable failed"
                        + " — on dedicated servers without CAP_NET_RAW this is a false negative;"
                        + " ping may report failure even though the network works",
                e);
    }
}