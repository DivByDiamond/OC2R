
package li.cil.oc2.common.vm;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.sedna.device.serial.UART16550A;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalVMRunner extends VMRunner {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UART16550A uart;
    private final Terminal terminal;


    // Thread-local buffers for lock-free read/writes in inner loop.
    private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);


    public AbstractTerminalVMRunner(final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
        super(virtualMachine);
        this.terminal = terminal;
        uart = virtualMachine.state.builtinDevices.uart;
    }


    protected abstract void sendTerminalUpdateToClient(final ByteBuffer output);


    @Override
    protected void handleBeforeRun() {
        super.handleBeforeRun();

        int value;
        while ((value = terminal.readInput()) != -1) {
            inputBuffer.enqueue((byte) value);
        }
    }

    @Override
    protected void step(final int cyclesPerStep) {
        super.step(cyclesPerStep);

        while (!inputBuffer.isEmpty() && uart.canPutByte()) {
            uart.putByte(inputBuffer.dequeueByte());
        }
        uart.flush();

        int value;
        while ((value = uart.read()) != -1) {
            outputBuffer.enqueue((byte) value);
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
        putTerminalOutput(output);
    }


    private void putTerminalOutput(final ByteBuffer output) {
        if (!output.hasRemaining()) {
            return;
        }

        // Always update the server-side terminal first — even if the network
        // send below fails (e.g. on a Valkyrien Skies ship world where the
        // chunk's level is not a ServerLevel and PacketDistributor rejects
        // it), the server-side terminal state stays correct so a future
        // re-render / re-open of the GUI still shows the buffered output.
        try {
            terminal.putOutput(output);
        } catch (final Throwable t) {
            LOGGER.error("Failed to update server-side terminal", t);
        }

        output.flip();

        try {
            sendTerminalUpdateToClient(output);
        } catch (final Throwable t) {
            // Don't let a network-layer failure (ClassCastException on a
            // non-ServerLevel, disconnected client, etc.) kill the runner
            // thread — that would freeze the VM in an "appears on, no UART
            // output" state.
            LOGGER.error("Failed to forward terminal output to clients", t);
        }
    }
}
