package li.cil.oc2.common.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import li.cil.oc2.common.vm.context.global.interrupt.GlobalInterruptAllocator;
import li.cil.oc2.common.vm.context.global.interrupt.GlobalInterruptController;
import li.cil.oc2.common.vm.context.managed.interrupt.ManagedInterruptAllocator;
import li.cil.sedna.Sedna;
import li.cil.sedna.device.memory.ByteBufferMemory;
import li.cil.sedna.riscv.R5Board;
import org.junit.jupiter.api.Test;

/** Dependency contracts needed by device allocation and hard reboot of an OC2R VM. */
class SednaUpgradeCompatibilityTest {
    private static final long CLINT = 0x02000000L;
    private static final long PLIC = 0x0C000000L;
    private static final long RAM = 0x80000000L;

    @Test
    void highestPlicSourceCanBeClaimedRoutedAndReleased() throws Exception {
        Sedna.initialize();
        final R5Board board = new R5Board();
        final var global = new GlobalInterruptAllocator(board.getInterruptCount(), new BitSet());
        final var device = new ManagedInterruptAllocator(global, global);
        assertTrue(device.claimInterrupt(31));
        assertTrue(device.isMaskValid(1 << 31));
        assertEquals(1 << 31, device.getMaskedInterrupts(-1));
        board.getMemoryMap().store(PLIC + 31 * 4, 5, 2);
        board.getMemoryMap().store(PLIC + 0x2000, 1 << 31, 2);
        final var controller = new GlobalInterruptController(board.getInterruptController(), global);
        controller.raiseInterrupts(1 << 31);
        assertEquals(1 << 31, controller.getRaisedInterrupts());
        assertEquals(31, board.getMemoryMap().load(PLIC + 0x200004, 2));
        controller.lowerInterrupts(1 << 31);
        device.invalidate();
        assertFalse(device.isMaskValid(1 << 31));
        assertTrue(global.claimInterrupt(31));
    }

    @Test
    void hardBoardResetClearsClintSoftwareInterruptAndTimerComparator() throws Exception {
        Sedna.initialize();
        final R5Board board = new R5Board();
        board.getMemoryMap().store(CLINT, 1, 2);
        board.getMemoryMap().store(CLINT + 0x4000, 123, 3);
        assertEquals(1, board.getMemoryMap().load(CLINT, 2));
        board.reset();
        assertEquals(0, board.getMemoryMap().load(CLINT, 2));
        assertEquals(-1L, board.getMemoryMap().load(CLINT + 0x4000, 3));
    }

    @Test
    void hardBoardResetClearsPlicConfigurationAndClaimedState() throws Exception {
        Sedna.initialize();
        final R5Board board = new R5Board();
        board.getMemoryMap().store(PLIC + 4, 5, 2);
        board.getMemoryMap().store(PLIC + 0x2000, 2, 2);
        board.getMemoryMap().store(PLIC + 0x200000, 3, 2);
        board.getInterruptController().raiseInterrupts(2);
        assertEquals(1, board.getMemoryMap().load(PLIC + 0x200004, 2));
        board.reset();
        assertEquals(0, board.getMemoryMap().load(PLIC + 4, 2));
        assertEquals(0, board.getMemoryMap().load(PLIC + 0x2000, 2));
        assertEquals(0, board.getMemoryMap().load(PLIC + 0x200000, 2));
        assertEquals(0, board.getMemoryMap().load(PLIC + 0x200004, 2));
        // A new interrupt on this line must not be suppressed by its old claim.
        board.getMemoryMap().store(PLIC + 4, 5, 2);
        board.getMemoryMap().store(PLIC + 0x2000, 2, 2);
        board.getInterruptController().raiseInterrupts(2);
        assertEquals(1, board.getMemoryMap().load(PLIC + 0x200004, 2));
    }

    @Test
    void hardCpuResetClearsGuestFloatingPointRegistersAndStatus() throws Exception {
        Sedna.initialize();
        final R5Board board = new R5Board();
        try (ByteBufferMemory memory = new ByteBufferMemory(4096)) {
            assertTrue(board.addDevice(RAM, memory));
            // Enable FS, put 123 in f0, and set fcsr rounding/exception flags.
            program(memory, 0,
                0x000060B7, 0x3000A073, 0x07B00113, 0xF2010053,
                0x09F00193, 0x00319073, 0x10500073);
            // Read FS before enabling it, then read fcsr and f0 through guest instructions.
            program(memory, 64,
                0x300022F3, 0x000060B7, 0x3000A073, 0x00302373,
                0xE20003D3, 0x10500073);
            board.getCpu().reset(true, RAM);
            board.getCpu().step(100);
            board.getCpu().reset(true, RAM + 64);
            board.getCpu().step(100);
            final long[] registers = board.getCpu().getGeneralRegisters();
            assertEquals(0, registers[5] & 0x6000, "mstatus.FS must start Off after hard reset");
            assertEquals(0, registers[6], "fcsr rounding and exception flags must reset");
            assertEquals(0, registers[7], "the previous guest's floating-point register must reset");
        }
    }

    private static void program(final ByteBufferMemory memory, final int offset, final int... instructions)
            throws Exception {
        for (int i = 0; i < instructions.length; i++) {
            memory.store(offset + i * 4, instructions[i], 2);
        }
    }
}
