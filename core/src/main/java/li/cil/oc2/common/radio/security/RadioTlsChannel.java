package li.cil.oc2.common.radio.security;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.function.Consumer;
import javax.net.ssl.*;

/**
 * Tick-driven TLS 1.3 stream. Every outbound byte must travel over the scheduled radio link. Call
 * only on the server thread. Radio loss must close both channels: there is no retransmission here.
 */
public final class RadioTlsChannel implements AutoCloseable {
    public static final int MAX_MESSAGE = 262144;
    private static final int CAPACITY = 1048576;
    private final SSLEngine engine;
    private final RadioTlsTrust trust;
    private final UUID peer;
    private final long generation;
    private final Consumer<byte[]> outbound;
    private final Consumer<byte[]> inbound;
    private final ByteBuffer network = ByteBuffer.allocate(65536);
    private final ByteBuffer plain = ByteBuffer.allocate(MAX_MESSAGE + 4);
    private final ArrayDeque<ByteBuffer> queued = new ArrayDeque<>();
    private int queuedBytes;
    private int age;
    private int incompleteAge;
    private boolean started;
    private boolean closed;
    private boolean pumping;
    private byte[] peerCertificate;

    public RadioTlsChannel(
            RadioTlsIdentity identity,
            RadioTlsTrust trust,
            UUID peer,
            long generation,
            boolean client,
            Consumer<byte[]> outbound,
            Consumer<byte[]> inbound) {
        this.trust = Objects.requireNonNull(trust);
        this.peer = Objects.requireNonNull(peer);
        this.generation = generation;
        this.outbound = Objects.requireNonNull(outbound);
        this.inbound = Objects.requireNonNull(inbound);
        try {
            KeyStore keys = KeyStore.getInstance("PKCS12");
            keys.load(null, null);
            char[] password = new char[0];
            keys.setKeyEntry(
                    "device",
                    identity.privateKey,
                    password,
                    new java.security.cert.Certificate[] {identity.certificate});
            KeyManagerFactory managers =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            managers.init(keys, password);
            X509TrustManager pinned =
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] chain, String auth)
                                throws CertificateException {
                            check(chain);
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String auth)
                                throws CertificateException {
                            check(chain);
                        }

                        private void check(X509Certificate[] chain) throws CertificateException {
                            if (chain == null || chain.length != 1)
                                throw new CertificateException(
                                        "Expected enrolled endpoint certificate");
                            chain[0].checkValidity();
                            byte[] certificate = chain[0].getEncoded();
                            if (!trust.accepts(peer, generation, certificate))
                                throw new CertificateException("Radio endpoint is not enrolled");
                            peerCertificate = certificate;
                        }
                    };
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(
                    managers.getKeyManagers(), new TrustManager[] {pinned}, new SecureRandom());
            engine = context.createSSLEngine();
            engine.setUseClientMode(client);
            engine.setEnabledProtocols(new String[] {"TLSv1.3"});
            engine.setEnabledCipherSuites(
                    new String[] {"TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"});
            if (!client) engine.setNeedClientAuth(true);
            // Each channel has a fresh context, so no session resumption or early data is possible.
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot initialize radio TLS", exception);
        }
    }

    public void start() {
        if (started || closed) return;
        started = true;
        try {
            engine.beginHandshake();
            pump();
        } catch (Exception exception) {
            close();
        }
    }

    public boolean isOpen() {
        return started
                && !closed
                && peerCertificate != null
                && engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean send(byte[] message) {
        if (closed
                || message == null
                || message.length > MAX_MESSAGE
                || queuedBytes + message.length + 4 > CAPACITY) return false;
        ByteBuffer frame = ByteBuffer.allocate(message.length + 4);
        frame.putInt(message.length).put(message).flip();
        queued.add(frame);
        queuedBytes += frame.remaining();
        if (!started) start();
        else pump();
        return !closed;
    }

    public void receive(byte[] ciphertext) {
        if (closed) return;
        if (ciphertext == null
                || ciphertext.length == 0
                || ciphertext.length > network.remaining()) {
            close();
            return;
        }
        network.put(ciphertext);
        incompleteAge = 0;
        if (!started) start();
        else pump();
    }

    public void tick() {
        if (closed) return;
        if (!started) start();
        if (++age > 200 && !isOpen()) {
            close();
            return;
        }
        if ((network.position() > 0 || plain.position() > 0) && ++incompleteAge > 200) {
            close();
            return;
        }
        if (peerCertificate != null && !trust.accepts(peer, generation, peerCertificate)) {
            close();
            return;
        }
        pump();
    }

    private void pump() {
        if (pumping || closed || !started) return;
        pumping = true;
        try {
            for (int work = 0; work < 64 && !closed; work++) {
                if (peerCertificate != null && !trust.accepts(peer, generation, peerCertificate)) {
                    close();
                    break;
                }
                SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
                if (status == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    Runnable task = engine.getDelegatedTask();
                    if (task == null) break;
                    task.run();
                    continue;
                }
                if (status == SSLEngineResult.HandshakeStatus.NEED_WRAP
                        || (isOpen() && !queued.isEmpty())) {
                    ByteBuffer source =
                            status == SSLEngineResult.HandshakeStatus.NEED_WRAP
                                    ? ByteBuffer.allocate(0)
                                    : queued.peek();
                    ByteBuffer target = ByteBuffer.allocate(32768);
                    SSLEngineResult result = engine.wrap(source, target);
                    queuedBytes -= result.bytesConsumed();
                    if (!source.hasRemaining()
                            && !queued.isEmpty()
                            && source == queued.peek()) queued.remove();
                    if (result.getStatus() == SSLEngineResult.Status.CLOSED
                            || result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        close();
                        break;
                    }
                    target.flip();
                    if (target.hasRemaining()) {
                        byte[] bytes = new byte[target.remaining()];
                        target.get(bytes);
                        outbound.accept(bytes);
                    }
                    if (result.bytesConsumed() == 0 && result.bytesProduced() == 0) break;
                    continue;
                }
                if (network.position() == 0) break;
                network.flip();
                SSLEngineResult result = engine.unwrap(network, plain);
                network.compact();
                if (result.getStatus() == SSLEngineResult.Status.CLOSED
                        || result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                    close();
                    break;
                }
                deliver();
                if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) break;
                if (result.bytesConsumed() == 0 && result.bytesProduced() == 0) break;
            }
        } catch (Exception exception) {
            close();
        } finally {
            pumping = false;
        }
    }

    private void deliver() {
        plain.flip();
        while (plain.remaining() >= 4) {
            plain.mark();
            int length = plain.getInt();
            if (length < 0 || length > MAX_MESSAGE) {
                close();
                break;
            }
            if (plain.remaining() < length) {
                plain.reset();
                break;
            }
            byte[] message = new byte[length];
            plain.get(message);
            if (!isOpen() || !trust.accepts(peer, generation, peerCertificate)) {
                close();
                break;
            }
            inbound.accept(message);
        }
        plain.compact();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        engine.closeOutbound();
        for (ByteBuffer pending : queued) Arrays.fill(pending.array(), (byte) 0);
        queued.clear();
        queuedBytes = 0;
        Arrays.fill(network.array(), (byte) 0);
        network.clear();
        Arrays.fill(plain.array(), (byte) 0);
        plain.clear();
        peerCertificate = null;
    }
}
