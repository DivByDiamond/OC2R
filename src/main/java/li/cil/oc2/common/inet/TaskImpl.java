package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.InternetManager;

final class TaskImpl implements InternetManager.Task {
    private final Runnable action;
    private boolean closed = false;

    public TaskImpl(final Runnable action) {
        this.action = action;
    }

    public Runnable getAction() {
        return action;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
