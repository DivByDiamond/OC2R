package li.cil.oc2.common.radio.security;

import java.security.MessageDigest;
import java.util.*;

/** Enrollment is exclusively controlled by the authoritative server, never by a received packet. */
public final class RadioTlsTrust {
    private record Pin(long generation, byte[] certificate) {}

    private final Map<UUID, Pin> pins = new HashMap<>();

    public void enroll(UUID id, long generation, byte[] certificate) {
        Objects.requireNonNull(id);
        if (generation < 0
                || certificate == null
                || certificate.length < 1
                || certificate.length > 8192)
            throw new IllegalArgumentException("Invalid enrollment");
        pins.put(id, new Pin(generation, certificate.clone()));
    }

    public void revoke(UUID id) {
        pins.remove(id);
    }

    public boolean accepts(UUID id, long generation, byte[] certificate) {
        Pin pin = pins.get(id);
        return pin != null
                && pin.generation == generation
                && MessageDigest.isEqual(pin.certificate, certificate);
    }
}
