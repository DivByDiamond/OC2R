package li.cil.oc2.common.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import li.cil.sedna.device.memory.ByteBufferMemory;
import li.cil.sedna.z80.Z80Board;
import org.junit.jupiter.api.Test;

class Z80BoardExecutionTest {
    @Test
    void executesGuestProgramFromMappedRam() throws Exception {
        final Z80Board board = new Z80Board();
        try (ByteBufferMemory ram = new ByteBufferMemory(0x10000)) {
            assertTrue(board.addDevice(0, ram));

            // LD A, 0x42; LD (0x2000), A; HALT
            final byte[] program = {0x3E, 0x42, 0x32, 0x00, 0x20, 0x76};
            for (int i = 0; i < program.length; i++) {
                ram.store(i, program[i], 0);
            }

            board.reset();
            board.setRunning(true);
            board.step(100);

            assertEquals(0x42, board.getMemoryMap().load(0x2000, 0));
            assertTrue(board.isHalted());
        }
    }
}
