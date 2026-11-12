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

        /**
         * Takes back the session handed to {@link #receive}: nothing is delivered for it. Used when the data that
         * made a session ready turns out not to be deliverable, so that no empty message is built from it.
         */
        default void cancel() {}
    }
}