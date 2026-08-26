package li.cil.oc2.common.inet.session.echo;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.Main;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.layer.impl.DefaultSessionLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class EchoHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Executor executor =
            Executors.newSingleThreadExecutor(
                    runnable -> new Thread(runnable, "internet/blocking-session"));

    private static volatile boolean warnedAboutIcmpFallback = false;

    public static boolean deliverPendingResponse(
            final AtomicReference<EchoResponse> echoResponse,
            final SessionLayer.Receiver receiver) {
        final EchoResponse pending = echoResponse.getAndSet(null);
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
            final AtomicReference<EchoResponse> echoResponse) {
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
        executor.execute(() -> processEcho(echoSession, address, data, payload, size, echoResponse));
        return true;
    }

    private static void processEcho(
            final EchoSession session,
            final InetAddress address,
            final ByteBuffer fallbackData,
            final byte[] payload,
            final int size,
            final AtomicReference<EchoResponse> echoResponse) {
        if (Main.LoadedLibrary) {
            handleIcmpNative(session, address, payload, size, echoResponse);
        } else {
            handleIcmpFallback(session, address, fallbackData, echoResponse);
        }
    }

    private static void handleIcmpNative(
            final EchoSession session,
            final InetAddress address,
            final byte[] payload,
            final int size,
            final AtomicReference<EchoResponse> echoResponse) {
        final byte[] responseData =
                DefaultSessionLayer.sendICMP(
                        address.getAddress(),
                        payload,
                        size,
                        Config.defaultEchoRequestTimeoutMs);
        if (responseData != null) {
            echoResponse.set(new EchoResponse(ByteBuffer.wrap(responseData), session));
        }
    }

    private static void handleIcmpFallback(
            final EchoSession session,
            final InetAddress address,
            final ByteBuffer data,
            final AtomicReference<EchoResponse> echoResponse) {
        try {
            final EchoResponse response = new EchoResponse(data, session);
            if (address.isReachable(null, session.getTtl(), Config.defaultEchoRequestTimeoutMs)) {
                echoResponse.set(response);
            } else {
                warnAboutIcmpFallbackOnce(null);
            }
        } catch (IOException e) {
            warnAboutIcmpFallbackOnce(e);
            LOGGER.error("Failed to get echo response", e);
        }
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