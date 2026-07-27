package li.cil.oc2.common.blockentity.computer.vm;

import java.time.Duration;
import javax.annotation.Nullable;
import li.cil.oc2.client.audio.LoopingSoundManager;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.computer.ComputerVMRunner;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.network.message.ComputerBootErrorMessage;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.ComputerTerminalOutputMessage;
import li.cil.oc2.common.util.ChunkUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.TerminalUtils;
import li.cil.oc2.common.util.TickUtils;
import li.cil.oc2.common.vm.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.BaseAddressProvider;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class ComputerVirtualMachine extends AbstractVirtualMachine {
    private static final int MAX_RUNNING_SOUND_DELAY = TickUtils.toTicks(Duration.ofSeconds(2));

    private final ComputerBlockEntity owner;

    public ComputerVirtualMachine(
            final ComputerBlockEntity owner,
            final CommonDeviceBusController busController,
            final BaseAddressProvider baseAddressProvider) {
        super(busController);
        this.owner = owner;
        state.vmAdapter.setBaseAddressProvider(baseAddressProvider);
    }

    @Override
    public void setRunStateClient(final VMRunState value) {
        super.setRunStateClient(value);

        if (value == VMRunState.RUNNING) {
            final Level level = owner.getLevel();
            if (!LoopingSoundManager.isPlaying(owner) && level != null) {
                LoopingSoundManager.play(
                        owner,
                        SoundEvents.COMPUTER_RUNNING.get(),
                        level.getRandom().nextInt(MAX_RUNNING_SOUND_DELAY));
            }
        } else {
            LoopingSoundManager.stop(owner);
        }
    }

    @Override
    public void tick() {
        final Level level = owner.getLevel();
        assert level != null;

        if (isRunning()) {
            ChunkUtils.setLazyUnsaved(level, owner.getBlockPos());
            busController.setDeviceContainersChanged();
        }

        super.tick();
    }

    @Override
    public boolean consumeEnergy(final int amount, final boolean simulate) {
        if (!Config.computersUseEnergy()) {
            return true;
        }

        if (amount > owner.energy.getEnergyStored()) {
            return false;
        }

        owner.energy.extractEnergy(amount, simulate);
        return true;
    }

    @Override
    protected void stopRunnerAndReset() {
        super.stopRunnerAndReset();

        TerminalUtils.resetTerminal(
                owner.terminal,
                output ->
                        owner.sendToClientsTrackingComputer(
                                new ComputerTerminalOutputMessage(owner, output)));
    }

    @Override
    public AbstractTerminalVMRunner createRunner() {
        return new ComputerVMRunner(owner, this, owner.terminal);
    }

    @Override
    protected void handleBusStateChanged(final BusState value) {
        owner.sendToClientsTrackingComputer(new ComputerBusStateMessage(owner, value));

        final Level level = owner.getLevel();
        if (value == BusState.READY && level != null) {
            level.updateNeighborsAt(owner.getBlockPos(), owner.getBlockState().getBlock());
        }
    }

    @Override
    protected void handleRunStateChanged(final VMRunState value) {
        owner.sendToClientsTrackingComputer(new ComputerRunStateMessage(owner, value));
    }

    @Override
    protected void handleBootErrorChanged(@Nullable final Component value) {
        owner.sendToClientsTrackingComputer(new ComputerBootErrorMessage(owner, value));
    }
}