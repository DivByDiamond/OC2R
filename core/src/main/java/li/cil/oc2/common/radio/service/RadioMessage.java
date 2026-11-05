package li.cil.oc2.common.radio.service;

import java.util.Objects;
import java.util.UUID;
import li.cil.oc2.common.radio.RadioAddress;

/** Immutable bounded application envelope. Identities are assigned by the broker. */
public record RadioMessage(
        int version,
        UUID source,
        RadioAddress sourceAddress,
        UUID destination,
        RadioAddress destinationAddress,
        String service,
        String channel,
        UUID requestId,
        Type type,
        byte[] payload) {
    public static final int VERSION = 1;
    public static final int MAX_PAYLOAD = 16384;

    public enum Type {
        MESSAGE,
        TELEMETRY,
        COMMAND,
        RESULT,
        PING,
        PONG
    }

    public RadioMessage {
        if (version != VERSION) throw new IllegalArgumentException("Unsupported protocol version");
        Objects.requireNonNull(source);
        Objects.requireNonNull(sourceAddress);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(destinationAddress);
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(type);
        name(service);
        name(channel);
        if (payload == null || payload.length > MAX_PAYLOAD)
            throw new IllegalArgumentException("Payload limit");
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public static void name(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9_.:/-]{1,128}"))
            throw new IllegalArgumentException("Invalid service or channel");
    }
}
