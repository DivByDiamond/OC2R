package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;

public class TerminalIO {
    private final Terminal terminal;
    private final TerminalOutput output;

    TerminalIO(final Terminal terminal) {
        this.terminal = terminal;
        this.output = new TerminalOutput(terminal);
    }

    public int readInput() {
        synchronized (terminal) {
            if (terminal.input.isEmpty()) {
                return -1;
            } else {
                return terminal.input.dequeueByte() & 0xFF;
            }
        }
    }

    @Nullable
    public ByteBuffer getInput() {
        synchronized (terminal) {
            if (terminal.input.isEmpty()) {
                return null;
            } else {
                if (!terminal.currentPrivateModeState.isAltBufferEnabled())
                    terminal.lastRowToDisplay = terminal.lastRowToDisplayMax;
                int dirtyLinesMask = 0;
                for (int i = 0; i <= 23; i++) {
                    dirtyLinesMask |= 1 << i;
                }
                final int finalDirtyLinesMask = dirtyLinesMask;
                terminal.renderers.forEach(
                        model ->
                                model.getDirtyMask()
                                        .accumulateAndGet(
                                                finalDirtyLinesMask, (left, right) -> left | right));
                final ByteBuffer buffer = ByteBuffer.allocate(terminal.input.size());
                while (!terminal.input.isEmpty()) {
                    buffer.put(terminal.input.dequeueByte());
                }
                buffer.flip();
                return buffer;
            }
        }
    }

    public void putInput(final String value) {
        synchronized (terminal) {
            putInput(ByteBuffer.wrap(value.getBytes()));
        }
    }

    public void putInput(final ByteBuffer values) {
        synchronized (terminal) {
            while (values.hasRemaining()) {
                terminal.input.enqueue(values.get());
            }
        }
    }

    public void putOutput(final ByteBuffer values) {
        output.putOutput(values);
    }

    public void putOutput(final byte value) {
        output.putOutput(value);
    }

    public void putInput(final char value) {
        synchronized (terminal) {
            putInput((byte) value);
        }
    }

    public void putInput(final byte value) {
        synchronized (terminal) {
            terminal.input.enqueue(value);
        }
    }

    private void enqueueInput(final byte value) {
        terminal.input.enqueue(value);
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
