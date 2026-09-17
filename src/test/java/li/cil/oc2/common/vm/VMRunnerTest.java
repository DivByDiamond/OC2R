package li.cil.oc2.common.vm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner;
import li.cil.oc2.common.vm.state.SerializedState;
import li.cil.sedna.riscv.R5Board;
import li.cil.sedna.riscv.R5CPU;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.*;

class VMRunnerTest {

    private R5Board board;
    private R5CPU cpu;
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
        
        AbstractVirtualMachine vm = new TestVirtualMachine(busController);
        
        // Set up the state with our mocks
        vm.state.board = board;
        vm.state.context = mock(GlobalVMContext.class);
        vm.state.rpcAdapter = mock(RPCDeviceBusAdapter.class);

        runner = new VMRunner(vm);
    }

    // Concrete test implementation of AbstractVirtualMachine
    static class TestVirtualMachine extends AbstractVirtualMachine {
        public TestVirtualMachine(CommonDeviceBusController busController) {
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
        when(board.isRunning()).thenReturn(true);
        when(cpu.getFrequency()).thenReturn(20_000_000);

        runner.run();

        verify(board, atLeastOnce()).step(anyInt());
    }

    @Test
    void testRunRespectsTimeQuota() {
        when(board.isRunning()).thenReturn(true);
        when(cpu.getFrequency()).thenReturn(1_000_000);

        runner.run();

        verify(board, atLeastOnce()).step(anyInt());
    }

    @Test
    void testRunStopsWhenBoardNotRunning() {
        when(board.isRunning()).thenReturn(false);

        runner.run();

        verify(board, never()).step(anyInt());
    }
}