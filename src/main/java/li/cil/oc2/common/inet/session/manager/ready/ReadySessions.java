package li.cil.oc2.common.inet.session.manager.ready;

import java.util.ArrayDeque;
import java.util.Queue;
import li.cil.oc2.api.inet.session.Session;

public final class ReadySessions {
    private final Queue<Session> toRead = new ArrayDeque<>();
    private final Queue<Session> toConnect = new ArrayDeque<>();

    public Queue<Session> getToRead() {
        return toRead;
    }

    public Queue<Session> getToConnect() {
        return toConnect;
    }
}