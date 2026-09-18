package li.cil.oc2.common.vm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.sedna.riscv.R5Board;
import li.cil.sedna.riscv.R5CPU;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.*;

class VMRunnerTest {

    // Mirrors VMRunner's private TICKS_PER_SECOND constant and its hardcoded cyclesPerStep.
    private static final int TICKS_PER_SECOND = 20;
    private static final int CYCLES_PER_STEP = 1_000;

    private R5Board board;
    private R5CPU cpu;
    private GlobalVMContext context;
    private VMRunner runner;

    @BeforeEach
    void setUp() {
        board = mock(R5Board.class);
        cpu = mock(R5CPU.class);
        when(board.getCpu()).thenReturn(cpu);
        when(cpu.getFrequency()).thenReturn(1_000_000); // 1 MHz
        when(board.isRunning()).thenReturn(true);
        when(board.getDefaultProgramStart()).thenReturn(0L);

        // Use a real CommonDeviceBusController with a mock root element
        DeviceBusElement rootElement = mock(DeviceBusElement.class);
        CommonDeviceBusController busController = new CommonDeviceBusController(rootElement, 100);

        AbstractVirtualMachine vm = new StubVirtualMachine(busController);

        // Set up the state with our mocks
        vm.state.board = board;
        context = mock(GlobalVMContext.class);
        vm.state.context = context;
        vm.state.rpcAdapter = mock(RPCDeviceBusAdapter.class);

        runner = new VMRunner(vm);
    }

    // Concrete test implementation of AbstractVirtualMachine
    static class StubVirtualMachine extends AbstractVirtualMachine {
        public StubVirtualMachine(CommonDeviceBusController busController) {
            super(busController);
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void tick() {}

        @Override
        public boolean consumeEnergy(int amount, boolean simulate) {
            return true;
        }

        @Override
        public AbstractTerminalVMRunner createRunner() {
            return null;
        }
    }

    @Test
    void testConstructorInitializesFields() {
        assertNotNull(runner);
        assertNull(runner.getRuntimeError());
    }

    @Test
    void testGetRuntimeErrorInitiallyNull() {
        assertNull(runner.getRuntimeError());
    }

    @Test
    void testTickSchedulesWork() {
        when(board.isRunning()).thenReturn(true);
        runner.tick();
    }

    @Test
    void testTickDoesNotScheduleWhenNotRunning() {
        when(board.isRunning()).thenReturn(false);
        runner.tick();
    }

    @Test
    void testJoinWaitsForCompletion() throws Exception {
        when(board.isRunning()).thenReturn(false);
        runner.tick();
        runner.join();
    }

    @Test
    void testRunHandlesException() {
        when(board.isRunning()).thenReturn(true);
        when(cpu.getFrequency()).thenReturn(1_000_000);
        doThrow(new RuntimeException("Test exception")).when(board).step(anyInt());

        runner.run();

        assertNotNull(runner.getRuntimeError());
        assertTrue(runner.getRuntimeError().getString().contains("RuntimeException"));
    }

    @Test
    void testRunRespectsCycleLimit() {
        // 20 MHz / 20 ticks-per-second = 1,000,000 cycles/tick budget, in 1000-cycle steps: with
        // cycleLimit at its default of 0 (tick() was never called to grant any), the outer
        // do-while loop must never iterate more than once, so the step count can never exceed
        // the per-tick budget however the (timing-dependent) inner quota check cuts it short.
        when(board.isRunning()).thenReturn(true);
        when(cpu.getFrequency()).thenReturn(20_000_000);

        runner.run();

        final int maxPossibleSteps = 20_000_000 / TICKS_PER_SECOND / CYCLES_PER_STEP;
        verify(board, atLeastOnce()).step(CYCLES_PER_STEP);
        verify(board, atMost(maxPossibleSteps)).step(CYCLES_PER_STEP);
        assertTrue(runner.getCycles() <= (long) maxPossibleSteps * CYCLES_PER_STEP);
    }

    @Test
    void testRunRespectsTimeQuota() {
        when(board.isRunning()).thenReturn(true);
        when(cpu.getFrequency()).thenReturn(1_000_000);

        runner.run();

        // The per-tick cycle budget is a hard ceiling regardless of how the wall-clock quota
        // check (inherently timing-dependent, so not asserted exactly here) cuts the loop short.
        final int maxPossibleSteps = 1_000_000 / TICKS_PER_SECOND / CYCLES_PER_STEP;
        verify(board, atLeastOnce()).step(CYCLES_PER_STEP);
        assertTrue(runner.getCycles() <= (long) maxPossibleSteps * CYCLES_PER_STEP);
    }

    @Test
    void testRunHandlesVMInitializationException() {
        when(board.isRunning()).thenReturn(true);
        final Component errorMessage = Component.literal("init failed");
        doThrow(new VMInitializationException(errorMessage))
                .when(context)
                .postEvent(any(VMInitializingEvent.class));

        runner.run();

        assertNotNull(runner.getRuntimeError());
        assertEquals(errorMessage, runner.getRuntimeError());
        verify(board).setRunning(false);
    }

    @Test
    void testRunStopsWhenBoardNotRunning() {
        when(board.isRunning()).thenReturn(false);

        runner.run();

        verify(board, never()).step(anyInt());
    }
}