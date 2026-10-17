package li.cil.oc2.common.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import li.cil.ceres.Ceres;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.sedna.device.memory.ByteBufferMemory;
import li.cil.sedna.riscv.R5Board;
import org.junit.jupiter.api.Test;

/**
 * A saved board must continue from the same program counter and registers, not reboot. RAM is
 * persisted separately (blob storage), so the same RAM object is handed to the restored board.
 * The CPU is serialized through its runtime class; the world save has the same layout under
 * {@code state/state/board/cpu} (checked against a real 26.1.2 world).
 */
class WarmContinuationTest {
    static {
        Ceres.initialize();
    }

    private static final long RAM_BASE = 0x80000000L;
    private static final int COUNTER_OFFSET = 0x1000;

    @Test
    void restoredBoardContinuesCountingInsteadOfRestarting() throws Exception {
        final ByteBufferMemory ram = new ByteBufferMemory(0x2000);
        try {
            // lui x6,0x80001; zero-extend x6 (lui sign-extends on RV64); loop: addi x5,x5,1; sw x5,0(x6); j loop
            final int[] program = {0x80001337, 0x02031313, 0x02035313, 0x00128293, 0x00532023, 0xFF9FF06F};
            for (int i = 0; i < program.length; i++) {
                ram.store(i * 4, program[i], 2);
            }

            final R5Board first = new R5Board();
            assertTrue(first.addDevice(RAM_BASE, ram));
            first.reset();
            first.getCpu().reset(true, RAM_BASE);
            first.setRunning(true);
            first.step(2_000);
            final int before = (int) first.getMemoryMap().load(RAM_BASE + COUNTER_OFFSET, 2);
            assertTrue(before > 10, "guest should have counted, was " + before);

            final var saved = NBTSerialization.serialize(first.getCpu());

            final R5Board second = new R5Board();
            assertTrue(second.addDevice(RAM_BASE, ram));
            second.reset();
            NBTSerialization.deserialize(saved, second.getCpu());
            second.setRunning(true);
            second.step(2_000);

            final int after = (int) second.getMemoryMap().load(RAM_BASE + COUNTER_OFFSET, 2);
            assertTrue(after > before, "restored board restarted: before=" + before + " after=" + after);
            assertEquals(after, (int) second.getMemoryMap().load(RAM_BASE + COUNTER_OFFSET, 2));
        } finally {
            ram.close();
        }
    }
}
