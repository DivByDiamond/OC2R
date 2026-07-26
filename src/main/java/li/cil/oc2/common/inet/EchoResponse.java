package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.EchoSession;

import java.nio.ByteBuffer;

final class EchoResponse {
    final byte[] payload;
    final EchoSession session;

    EchoResponse(final ByteBuffer payload, final EchoSession session) {
        this.payload = new byte[payload.remaining()];
        payload.get(this.payload);
        this.session = session;
    }
}
