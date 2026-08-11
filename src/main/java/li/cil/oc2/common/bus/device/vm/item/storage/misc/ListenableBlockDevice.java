package li.cil.oc2.common.bus.device.vm.item.storage.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import li.cil.oc2.common.util.event.Event;
import li.cil.sedna.api.device.BlockDevice;

public final class ListenableBlockDevice implements BlockDevice {
    private final BlockDevice inner;

    public final Set<Runnable> accessCallbacks = new Event();

    public ListenableBlockDevice(final BlockDevice inner) {
        this.inner = inner;
    }

    @Override
    public boolean isReadonly() {
        return inner.isReadonly();
    }

    @Override
    public long getCapacity() {
        return inner.getCapacity();
    }

    @Override
    public InputStream getInputStream(final long offset) {
        final ListenableInputStream stream =
                new ListenableInputStream(inner.getInputStream(offset));
        stream.accessCallbacks.add(() -> accessCallbacks.forEach(Runnable::run));
        return stream;
    }

    @Override
    public OutputStream getOutputStream(final long offset) {
        final ListenableOutputStream stream =
                new ListenableOutputStream(inner.getOutputStream(offset));
        stream.accessCallbacks.add(() -> accessCallbacks.forEach(Runnable::run));
        return stream;
    }

    @Override
    public void flush() {
        inner.flush();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }
}

final class ListenableInputStream extends InputStream {
    private final InputStream inner;

    public final Set<Runnable> accessCallbacks = new Event();

    ListenableInputStream(final InputStream inner) {
        this.inner = inner;
    }

    @Override
    public int read() throws IOException {
        fireAccess();
        return inner.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        fireAccess();
        return inner.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        fireAccess();
        return inner.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        fireAccess();
        return inner.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inner.available();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    @Override
    public synchronized void mark(final int limit) {
        inner.mark(limit);
    }

    @Override
    public synchronized void reset() throws IOException {
        fireAccess();
        inner.reset();
    }

    @Override
    public boolean markSupported() {
        return inner.markSupported();
    }

    private void fireAccess() {
        accessCallbacks.forEach(Runnable::run);
    }
}

final class ListenableOutputStream extends OutputStream {
    private final OutputStream inner;

    public final Set<Runnable> accessCallbacks = new Event();

    ListenableOutputStream(final OutputStream inner) {
        this.inner = inner;
    }

    @Override
    public void write(final int b) throws IOException {
        fireAccess();
        inner.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        fireAccess();
        inner.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        fireAccess();
        inner.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        inner.flush();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    private void fireAccess() {
        accessCallbacks.forEach(Runnable::run);
    }
}
