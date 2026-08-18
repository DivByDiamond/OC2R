package li.cil.oc2.common.vm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent;
import li.cil.oc2.api.bus.device.vm.event.VMSynchronizeEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VMRunner implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TICKS_PER_SECOND = 20;

    private static final ExecutorService VM_RUNNERS =
            Executors.newCachedThreadPool(
                    r -> {
                        final Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        thread.setName("VirtualMachine Runner");
                        return thread;
                    });

    private final R5Board board;
    private final GlobalVMContext context;
    private final RPCDeviceBusAdapter rpcAdapter;
    private final AtomicInteger timeQuotaInMillis = new AtomicInteger();
    private Future<?> lastSchedule;

    private boolean firedResumedRunningEvent;
    @Serialized private boolean firedInitializationEvent;
    @Serialized private Component runtimeError;

    @Serialized private long cycleLimit;
    @Serialized private long cycles;

    public VMRunner(final AbstractVirtualMachine virtualMachine) {
        this.board = virtualMachine.state.board;
        context = virtualMachine.state.context;
        rpcAdapter = virtualMachine.state.rpcAdapter;
    }

    @Nullable
    public Component getRuntimeError() {
        return runtimeError;
    }

    public void tick() {
        rpcAdapter.tick();

        final int cyclesPerTick = getCyclesPerTick();
        cycleLimit = Math.min(cycleLimit + cyclesPerTick, 2L * cyclesPerTick);

        final int timeQuota =
                timeQuotaInMillis.updateAndGet(
                        x -> Math.min(x + Config.vmTimeQuotaMs, Config.vmTimeQuotaMs));
        final boolean needsScheduling =
                lastSchedule == null || lastSchedule.isDone() || lastSchedule.isCancelled();
        if (cycleLimit > 0 && timeQuota > 0 && needsScheduling) {
            lastSchedule = VM_RUNNERS.submit(this);
        }
    }

    public void join() {
        context.postEvent(new VMSynchronizeEvent());
        firedResumedRunningEvent = false;
        if (lastSchedule != null) {
            try {
                lastSchedule.get();
            } catch (final InterruptedException ignored) {
                // We do not mind this.
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        try {
            do {
                final long start = System.currentTimeMillis();

                final int cycleBudget = getCyclesPerTick();
                final int cyclesPerStep = 1_000;
                final int maxSteps = cycleBudget / cyclesPerStep;

                handleBeforeRun();

                if (!board.isRunning()) {
                    break;
                }

                for (int i = 0; i < maxSteps; i++) {
                    cycles += cyclesPerStep;
                    board.step(cyclesPerStep);
                    step(cyclesPerStep);

                    if (System.currentTimeMillis() - start > timeQuotaInMillis.get()) {
                        break;
                    }
                }

                handleAfterRun();

                final int elapsed = (int) (System.currentTimeMillis() - start);
                timeQuotaInMillis.addAndGet(-elapsed);
            } while (cycles < cycleLimit && timeQuotaInMillis.get() > 0);
        } catch (final Exception t) {
            LOGGER.error("Unhandled exception in VM runner", t);
            runtimeError = Component.literal(t.getClass().getSimpleName() + ": " + t.getMessage());
            board.setRunning(false);
        }
    }

    protected void handleBeforeRun() {
        if (!firedInitializationEvent) {
            firedInitializationEvent = true;
            try {
                context.postEvent(new VMInitializingEvent(board.getDefaultProgramStart()));
            } catch (final VMInitializationException e) {
                board.setRunning(false);
                runtimeError =
                        e.getErrorMessage()
                                .orElse(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            } catch (final Exception t) {
                // Firmware loaders that fail with something other than
                // VMInitializationException (e.g. NPE from a missing
                // Buildroot resource, IOException from a corrupt firmware
                // blob) used to escape here and silently kill the runner.
                // Capture them so the user actually sees an error message
                // in the GUI instead of a frozen "running" state.
                LOGGER.error("Failed to initialize VM", t);
                board.setRunning(false);
                runtimeError =
                        Component.literal(t.getClass().getSimpleName() + ": " + t.getMessage());
                return;
            }
        }

        if (!firedResumedRunningEvent) {
            firedResumedRunningEvent = true;
            try {
                context.postEvent(new VMResumedRunningEvent());
            } catch (final Exception t) {
                LOGGER.error("Failed to dispatch VMResumedRunningEvent", t);
            }
        }
    }

    protected void step(final int cyclesPerStep) {
        rpcAdapter.step(cyclesPerStep);
    }

    protected void handleAfterRun() {}

    private int getCyclesPerTick() {
        return board.getCpu().getFrequency() / TICKS_PER_SECOND;
    }
}