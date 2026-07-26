/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity.computer;

import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.message.ComputerTerminalOutputMessage;
import li.cil.oc2.common.vm.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;

import java.nio.ByteBuffer;

public class ComputerVMRunner extends AbstractTerminalVMRunner {
    private final ComputerBlockEntity blockEntity;

    public ComputerVMRunner(final ComputerBlockEntity blockEntity, final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
        super(virtualMachine, terminal);
        this.blockEntity = blockEntity;
    }

    @Override
    protected void sendTerminalUpdateToClient(final ByteBuffer output) {
        blockEntity.sendToClientsTrackingComputer(new ComputerTerminalOutputMessage(blockEntity, output));
    }
}
