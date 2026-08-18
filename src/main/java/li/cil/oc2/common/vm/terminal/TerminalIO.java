package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

public class TerminalIO {

    private final ReentrantLock lock = new ReentrantLock();

    private final Terminal terminal;
    private final TerminalOutput output;

    TerminalIO(final Terminal terminal) {
        this.terminal = terminal;
        this.output = new TerminalOutput(terminal, lock);
    }

    public int readInput() {
        lock.lock();
        try {

            if (terminal.input.isEmpty()) {
                return -1;
            } else {
                return terminal.input.dequeueByte() & 0xFF;
            }
        
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public ByteBuffer getInput() {
        lock.lock();
        try {

            if (terminal.input.isEmpty()) {
                return null;
            } else {
                if (!terminal.currentPrivateModeState.isAltBufferEnabled())
                    terminal.lastRowToDisplay = terminal.lastRowToDisplayMax;
                int dirtyLinesMask = 0;
                for (int i = 0; i <= 23; i++) {
                    dirtyLinesMask |= 1 << i;
                }
                terminal.markDirty(dirtyLinesMask);
                final ByteBuffer buffer = ByteBuffer.allocate(terminal.input.size());
                while (!terminal.input.isEmpty()) {
                    buffer.put(terminal.input.dequeueByte());
                }
                buffer.flip();
                return buffer;
            }
        
        } finally {
            lock.unlock();
        }
    }

    public void putInput(final String value) {
        lock.lock();
        try {

            putInput(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));

        } finally {
            lock.unlock();
        }
    }

    public void putInput(final ByteBuffer values) {
        lock.lock();
        try {

            while (values.hasRemaining()) {
                terminal.input.enqueue(values.get());
            }
        
        } finally {
            lock.unlock();
        }
    }

    public void putOutput(final ByteBuffer values) {
        output.putOutput(values);
    }

    public void putOutput(final byte value) {
        output.putOutput(value);
    }

    public void putInput(final char value) {
        lock.lock();
        try {

            putInput((byte) value);
        
        } finally {
            lock.unlock();
        }
    }

    public void putInput(final byte value) {
        lock.lock();
        try {

            terminal.input.enqueue(value);
        
        } finally {
            lock.unlock();
        }
    }

    private void enqueueInput(final byte value) {
        lock.lock();
        try {
            terminal.input.enqueue(value);
        } finally {
            lock.unlock();
        }
    }

    public void putResponse(final String value) {
        for (int i = 0; i < value.length(); i++) {
            putResponse((byte) value.charAt(i));
        }
    }

    public void putResponse(final byte value) {
        if (!terminal.displayOnly) {
            enqueueInput(value);
        }
    }
}
