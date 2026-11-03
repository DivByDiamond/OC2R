package li.cil.oc2.common.radio.security;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioTlsTest {
    private record Wire(boolean toServer, byte[] bytes) { }
    private static final class Pair {
        final RadioTlsIdentity a = RadioTlsIdentity.create(UUID.randomUUID(), 1);
        final RadioTlsIdentity b = RadioTlsIdentity.create(UUID.randomUUID(), 2);
        final RadioTlsTrust trust = new RadioTlsTrust();
        final ArrayDeque<Wire> wire = new ArrayDeque<>();
        final List<byte[]> received = new ArrayList<>();
        final List<byte[]> encrypted = new ArrayList<>();
        final RadioTlsChannel client;
        final RadioTlsChannel server;
        Pair(boolean wrong) {
            trust.enroll(a.id(), a.generation(), a.certificate());
            trust.enroll(b.id(), b.generation(), wrong ? a.certificate() : b.certificate());
            client = new RadioTlsChannel(a, trust, b.id(), b.generation(), true,
                bytes -> { wire.add(new Wire(true, bytes)); encrypted.add(bytes); }, received::add);
            server = new RadioTlsChannel(b, trust, a.id(), a.generation(), false,
                bytes -> { wire.add(new Wire(false, bytes)); encrypted.add(bytes); }, received::add);
        }
        void drain() {
            for (int n = 0; n < 1000; n++) {
                while (!wire.isEmpty()) {
                    Wire next = wire.remove();
                    for (int offset = 0; offset < next.bytes.length; offset += 2048) {
                        (next.toServer ? server : client).receive(Arrays.copyOfRange(next.bytes, offset, Math.min(next.bytes.length, offset + 2048)));
                    }
                }
                client.tick(); server.tick();
                if (wire.isEmpty()) break;
            }
        }
    }

    @Test void encryptsActualTransportAndChunksLargeMessages() {
        Pair pair = new Pair(false);
        byte[] payload = new byte[180000]; new Random(42).nextBytes(payload);
        assertTrue(pair.client.send(payload)); pair.drain();
        assertTrue(pair.client.isOpen()); assertTrue(pair.server.isOpen());
        assertEquals(1, pair.received.size()); assertArrayEquals(payload, pair.received.getFirst());
        byte[] secret = "radio plaintext must never travel unencrypted".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        pair.encrypted.clear(); assertTrue(pair.client.send(secret)); pair.drain();
        for (byte[] bytes : pair.encrypted) assertFalse(contains(bytes, secret));
        assertArrayEquals(secret, pair.received.getLast());
    }

    @Test void wrongPeerAndRevokedGenerationFailClosed() {
        Pair wrong = new Pair(true); wrong.client.send(new byte[]{1}); wrong.drain();
        assertTrue(wrong.client.isClosed()); assertTrue(wrong.received.isEmpty());
        Pair pair = new Pair(false); pair.client.start(); pair.drain();
        pair.trust.revoke(pair.b.id()); pair.client.tick();
        assertTrue(pair.client.isClosed()); assertFalse(pair.client.send(new byte[]{1}));
    }

    @Test void corruptionReorderingTruncationAndOversizeCannotDeliver() {
        Pair pair = new Pair(false); pair.client.start(); pair.drain();
        pair.client.send(new byte[50000]);
        List<Wire> records = new ArrayList<>(pair.wire); pair.wire.clear();
        assertTrue(records.size() > 1);
        pair.server.receive(records.get(1).bytes);
        assertTrue(pair.server.isClosed()); assertTrue(pair.received.isEmpty());
        Pair truncated = new Pair(false); truncated.client.start(); truncated.drain();
        truncated.client.send(new byte[100]); Wire record = truncated.wire.remove();
        truncated.server.receive(Arrays.copyOf(record.bytes, record.bytes.length - 1));
        assertTrue(truncated.received.isEmpty());
        for (int i = 0; i < 205; i++) truncated.server.tick();
        assertTrue(truncated.server.isClosed());
        Pair oversized = new Pair(false); oversized.server.receive(new byte[65537]);
        assertTrue(oversized.server.isClosed());
    }

    @Test void tamperingReplayAndExplicitCloseFailClosed() {
        Pair tampered = new Pair(false); tampered.client.start(); tampered.drain();
        tampered.client.send(new byte[]{3, 4, 5});
        byte[] ciphertext = tampered.wire.remove().bytes.clone();
        ciphertext[ciphertext.length - 1] ^= 1;
        tampered.server.receive(ciphertext);
        assertTrue(tampered.server.isClosed()); assertTrue(tampered.received.isEmpty());
        Pair replay = new Pair(false); replay.client.start(); replay.drain();
        replay.client.send(new byte[]{6}); byte[] record = replay.wire.remove().bytes;
        replay.server.receive(record); replay.server.receive(record);
        assertEquals(1, replay.received.size()); assertTrue(replay.server.isClosed());
        replay.client.close(); replay.client.close();
        assertTrue(replay.client.isClosed()); assertFalse(replay.client.send(new byte[]{1}));
    }

    @Test void droppedHandshakeExpiresAndIdentityPersistsPrivately() {
        Pair pair = new Pair(false); pair.client.start(); pair.wire.clear();
        for (int i = 0; i < 205; i++) pair.client.tick();
        assertTrue(pair.client.isClosed());
        RadioTlsIdentity restored = RadioTlsIdentity.restore(pair.a.id(), 1, pair.a.encodedPrivateMaterial());
        assertArrayEquals(pair.a.certificate(), restored.certificate());
        assertThrows(IllegalArgumentException.class, () -> RadioTlsIdentity.restore(pair.a.id(), 2, pair.a.encodedPrivateMaterial()));
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (haystack[i + j] != needle[j]) continue outer;
            return true;
        }
        return false;
    }
}
