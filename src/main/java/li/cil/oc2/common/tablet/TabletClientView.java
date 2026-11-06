package li.cil.oc2.common.tablet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.vm.VMErrorCalculator;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.network.chat.Component;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

/**
 * What a client knows about a tablet whose screen is open: a display-only terminal and the run state
 * the server reports. Power requests go through the open menu, so the control methods do nothing.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // The field backs its same-named accessor.
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The screen draws the live terminal.")
public final class TabletClientView implements VirtualMachine {
    private static final Map<UUID, Counted> VIEWS = new ConcurrentHashMap<>();

    private final Terminal terminal = new Terminal();
    private BusState busState = BusState.SCAN_PENDING;
    private VMRunState runState = VMRunState.STOPPED;
    @Nullable private Component bootError = Component.literal("");

    private static final class Counted {
        final TabletClientView view = new TabletClientView();
        int users;
    }

    private TabletClientView() {
        terminal.setDisplayOnly(true);
    }

    /** Called by every menu opened for the tablet; pair with {@link #release(UUID)}. */
    public static TabletClientView acquire(final UUID id) {
        final Counted counted = VIEWS.computeIfAbsent(id, k -> new Counted());
        counted.users++;
        return counted.view;
    }

    public static void release(final UUID id) {
        VIEWS.computeIfPresent(id, (k, counted) -> --counted.users <= 0 ? null : counted);
    }

    /** The view of an open screen, empty if no menu is open for the tablet. */
    public static Optional<TabletClientView> find(final UUID id) {
        return Optional.ofNullable(VIEWS.get(id)).map(counted -> counted.view);
    }

    public Terminal terminal() {
        return terminal;
    }

    @Override
    public BusState getBusState() {
        return busState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBusStateClient(final BusState value) {
        busState = value;
    }

    @Override
    public int getBusOverflow() {
        return 0;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBusOverflowClient(final int value) {}

    @Override
    public VMRunState getRunState() {
        return runState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setRunStateClient(final VMRunState value) {
        runState = value;
    }

    @Override
    @Nullable
    public Component getBootError() {
        return bootError;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBootErrorClient(@Nullable final Component value) {
        bootError = value;
    }

    @Override
    @Nullable
    public Component getError() {
        return VMErrorCalculator.getError(busState, 0, runState, bootError);
    }

    @Override
    public boolean isRunning() {
        return busState == BusState.READY && runState == VMRunState.RUNNING;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public void shutdown() {}

    @Override
    public void reboot() {}
}
