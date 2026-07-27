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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import li.cil.oc2.common.inet.layer.DefaultSessionLayer;

public final class EchoHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Executor executor =
            Executors.newSingleThreadExecutor(
                    runnable -> new Thread(runnable, "internet/blocking-session"));

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
        byte[] payload = new byte[data.remaining()];
        data.get(payload);
        int size = data.remaining();
        if (Main.LoadedLibrary) {
            byte[] responseData =
                    DefaultSessionLayer.sendICMP(
                            address.getAddress(),
                            payload,
                            size,
                            Config.defaultEchoRequestTimeoutMs);
            if (responseData != null) {
                final EchoResponse response =
                        new EchoResponse(ByteBuffer.wrap(responseData), echoSession);
                echoResponse.set(response);
            }
        } else {
            executor.execute(
                    () -> {
                        try {
                            final EchoResponse response = new EchoResponse(data, echoSession);
                            if (address.isReachable(
                                    null,
                                    echoSession.getTtl(),
                                    Config.defaultEchoRequestTimeoutMs)) {
                                echoResponse.set(response);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Failed to get echo response", e);
                        }
                    });
        }
        return true;
    }
}