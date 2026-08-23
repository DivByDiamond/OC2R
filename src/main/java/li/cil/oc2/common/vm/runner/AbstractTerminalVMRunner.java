package li.cil.oc2.common.vm.runner;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import java.nio.ByteBuffer;
import li.cil.oc2.common.vm.VMRunner;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import li.cil.sedna.device.serial.UART16550A;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractTerminalVMRunner extends VMRunner {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UART16550A uart;
    private final Terminal terminal;

    // Thread-local buffers for lock-free read/writes in inner loop.
    private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

    public AbstractTerminalVMRunner(
            final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
        super(virtualMachine);
        this.terminal = terminal;
        uart = virtualMachine.state.builtinDevices.uart;
    }

    /** Ships a screen diff/full snapshot to clients; the client never sees raw UART bytes. */
    protected abstract void sendTerminalDiffToClient(TerminalDiff.Snapshot snapshot);

    @Override
    protected void handleBeforeRun() {
        super.handleBeforeRun();

        int value = terminal.io.readInput();
        while (value != -1) {
            inputBuffer.enqueue((byte) value);
            value = terminal.io.readInput();
        }
    }

    @Override
    protected void step(final int cyclesPerStep) {
        super.step(cyclesPerStep);

        while (!inputBuffer.isEmpty() && uart.canPutByte()) {
            uart.putByte(inputBuffer.dequeueByte());
        }
        uart.flush();

        int value = uart.read();
        while (value != -1) {
            outputBuffer.enqueue((byte) value);
            value = uart.read();
        }
    }

    @Override
    protected void handleAfterRun() {
        super.handleAfterRun();

        if (outputBuffer.isEmpty()) {
            return;
        }

        final ByteBuffer output = ByteBuffer.allocate(outputBuffer.size());
        while (!outputBuffer.isEmpty()) {
            output.put(outputBuffer.dequeueByte());
        }
        output.flip();

        // Always update the server-side terminal first: it owns VT100 parsing and
        // accumulates the dirty rows the diff below ships to clients. Even if the
        // send fails (VS ship world, disconnected client), server state stays correct.
        try {
            terminal.io.putOutput(output);
        } catch (final Exception t) {
            LOGGER.error("Failed to update server-side terminal", t);
        }

        sendDiffToClient();
    }

    private void sendDiffToClient() {
        try {
            sendTerminalDiffToClient(TerminalDiff.capture(terminal));
        } catch (final Exception t) {
            // Don't let a network-layer failure (disconnected client, etc.) kill
            // the runner thread — that would freeze the VM in an "appears on, no
            // terminal output" state.
            LOGGER.error("Failed to forward terminal diff to clients", t);
        }
    }
}