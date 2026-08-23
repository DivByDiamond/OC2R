package li.cil.oc2.common.blockentity.computer;

import li.cil.oc2.common.network.message.computer.terminal.ComputerTerminalDiffMessage;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalDiff;

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
    protected void sendTerminalDiffToClient(final TerminalDiff.Snapshot snapshot) {
        blockEntity.terminalManager.sendToClientsTrackingComputer(
                new ComputerTerminalDiffMessage(blockEntity, snapshot));
    }
}