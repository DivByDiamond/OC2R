package li.cil.oc2.common.blockentity.computer;

import java.nio.ByteBuffer;
import li.cil.oc2.common.network.message.computer.terminal.ComputerTerminalOutputMessage;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;

public class ComputerVMRunner extends AbstractTerminalVMRunner {
    private final ComputerBlockEntity blockEntity;

    public ComputerVMRunner(
            final ComputerBlockEntity blockEntity,
            final AbstractVirtualMachine virtualMachine,
            final Terminal terminal) {
        super(virtualMachine, terminal);
        this.blockEntity = blockEntity;
    }

    @Override
    protected void sendTerminalUpdateToClient(final ByteBuffer output) {
        blockEntity.terminalManager.sendToClientsTrackingComputer(
                new ComputerTerminalOutputMessage(blockEntity, output));
    }
}