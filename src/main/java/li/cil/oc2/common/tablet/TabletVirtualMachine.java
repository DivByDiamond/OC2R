package li.cil.oc2.common.tablet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.tablet.TabletStateMessage;
import li.cil.oc2.common.network.message.tablet.TabletTerminalDiffMessage;
import li.cil.oc2.common.util.tick.TerminalUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachineShutdown;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** The small RISC-V machine of one tablet: no energy use, clock capped below a computer. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The machine keeps its live level and terminal.")
public final class TabletVirtualMachine extends AbstractVirtualMachine {
    /** A quarter of the computer clock, whatever processor item is inserted. */
    public static final int MAX_FREQUENCY = Constants.CPU_FREQUENCY / 4;

    private final UUID id;
    private final Level level;
    private final Terminal terminal;
    private final Supplier<Collection<ServerPlayer>> viewers;

    public TabletVirtualMachine(
            final UUID id,
            final Level level,
            final CommonDeviceBusController busController,
            final Terminal terminal,
            final TabletInventory inventory,
            final Supplier<Collection<ServerPlayer>> viewers) {
        super(busController);
        this.id = id;
        this.level = level;
        this.terminal = terminal;
        this.viewers = viewers;
        state.vmAdapter.setBaseAddressProvider(inventory::getDeviceAddressBase);
        // Publish only after the owner and all constructor dependencies are initialized.
        VirtualMachineShutdown.track(this);
    }

    @Override
    protected Level getLevelForShutdown() {
        return level;
    }

    @Override
    public boolean consumeEnergy(final int amount, final boolean simulate) {
        return true;
    }

    @Override
    public int limitFrequency(final int requested) {
        return Math.min(requested, MAX_FREQUENCY);
    }

    @Override
    protected void stopRunnerAndReset() {
        super.stopRunnerAndReset();
        TerminalUtils.resetTerminal(terminal, this::sendDiff);
    }

    @Override
    public AbstractTerminalVMRunner createRunner() {
        return new TabletRunner(this, terminal);
    }

    /** Sends the current run, bus and error state to every player looking at this tablet. */
    public void sendState(final ServerPlayer player) {
        NetworkMessages.sendToClient(
                new TabletStateMessage(id, runState, busState(), errorOrEmpty()), player);
    }

    @Override
    protected void handleBusStateChanged(final BusState value) {
        broadcastState();
    }

    @Override
    protected void handleRunStateChanged(final VMRunState value) {
        broadcastState();
    }

    @Override
    protected void handleBootErrorChanged(@Nullable final Component value) {
        broadcastState();
    }

    private void broadcastState() {
        for (final ServerPlayer player : viewers.get()) sendState(player);
    }

    private BusState busState() {
        return getBusState();
    }

    private Component errorOrEmpty() {
        return bootError == null ? Component.literal("") : bootError;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // One message per viewer.
    private void sendDiff(final TerminalDiff.Snapshot snapshot) {
        for (final ServerPlayer player : viewers.get()) {
            NetworkMessages.sendToClient(new TabletTerminalDiffMessage(id, snapshot), player);
        }
    }

    private final class TabletRunner extends AbstractTerminalVMRunner {
        TabletRunner(final AbstractVirtualMachine machine, final Terminal terminal) {
            super(machine, terminal);
        }

        @Override
        protected void sendTerminalDiffToClient(final TerminalDiff.Snapshot snapshot) {
            sendDiff(snapshot);
        }
    }
}
