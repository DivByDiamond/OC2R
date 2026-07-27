package li.cil.oc2.api.inet.layer;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.InternetDeviceLifecycle;
import li.cil.oc2.api.inet.session.Session;

/** A session layer interface of TCP/IP stack. */
public interface SessionLayer extends InternetDeviceLifecycle {

    String LAYER_NAME = "Session";

    ////////////////////////////////////////////////////////////////////////////////////

    default void receiveSession(final Receiver receiver) {}

    default void sendSession(final Session session, @Nullable final ByteBuffer data) {
        session.close();
    }

    interface Receiver {
        @Nullable
        ByteBuffer receive(Session session);
    }
}