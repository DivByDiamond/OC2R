package li.cil.oc2.common.vm.device.input;

import static org.junit.jupiter.api.Assertions.*;

import li.cil.sedna.device.memory.ByteBufferMemory;
import li.cil.sedna.memory.SimpleMemoryMap;
import org.junit.jupiter.api.Test;

class UnifiedInputDeviceTest {
    @Test
    void advertisesKeyboardButtonsMotionAndWheelThroughVirtioConfiguration() {
        final UnifiedInputDevice device = new UnifiedInputDevice(new SimpleMemoryMap());
        select(device, 17, 1);
        assertBit(device, 1);
        assertBit(device, 30);
        assertBit(device, 97);
        for (final int key : new int[] {86, 89, 99, 125, 126, 127}) assertBit(device, key);
        for (int button = 0x110; button < 0x118; button++) assertBit(device, button);
        select(device, 17, 2);
        assertBit(device, 0);
        assertBit(device, 1);
        assertBit(device, 8);
        select(device, 17, 3);
        assertEquals(0, device.load(0x102, 0));
    }

    @Test
    void deliversEscapeRightButtonAndRelativeMotionAsGuestEvents() throws Exception {
        final SimpleMemoryMap map = new SimpleMemoryMap();
        try (ByteBufferMemory memory = new ByteBufferMemory(0x10000)) {
            assertTrue(map.addDevice(0, memory));
            final UnifiedInputDevice device = new UnifiedInputDevice(map);
            // A real VirtIO queue: 8 writable event descriptors, available ring, used ring.
            for (int i = 0; i < 8; i++) {
                memory.store(0x1000 + i * 16, 0x4000 + i * 8, 3);
                memory.store(0x1008 + i * 16, 8, 2);
                memory.store(0x100c + i * 16, 2, 1);
                memory.store(0x2004 + i * 2, i, 1);
            }
            memory.store(0x2002, 8, 1);
            device.store(0x070, 3, 2);
            device.store(0x024, 1, 2);
            device.store(0x020, 1, 2);
            device.store(0x070, 11, 2);
            device.store(0x030, 0, 2);
            device.store(0x038, 8, 2);
            device.store(0x080, 0x1000, 2);
            device.store(0x090, 0x2000, 2);
            device.store(0x0a0, 0x3000, 2);
            device.store(0x044, 1, 2);
            device.store(0x070, 15, 2);
            device.sendKeyEvent(1, true);
            device.sendKeyEvent(0x111, true);
            device.sendRelativeEvent(0, -12);
            device.sendKeyEvent(0x111, false);
            device.step(1);
            assertEquals(8, memory.load(0x3002, 1));
            assertEvent(memory, 0, 1, 1, 1);
            assertEvent(memory, 1, 0, 0, 0);
            assertEvent(memory, 2, 1, 0x111, 1);
            assertEvent(memory, 4, 2, 0, -12);
            assertEvent(memory, 6, 1, 0x111, 0);
        }
    }

    private static void select(final UnifiedInputDevice device, final int select, final int subsel) {
        device.store(0x100, select, 0);
        device.store(0x101, subsel, 0);
    }

    private static void assertBit(final UnifiedInputDevice device, final int bit) {
        assertNotEquals(0, device.load(0x108 + bit / 8, 0) & (1 << (bit % 8)), "missing capability " + bit);
    }

    private static void assertEvent(final ByteBufferMemory memory, final int index,
            final int type, final int code, final int value) throws Exception {
        final int address = 0x4000 + index * 8;
        assertEquals(type, memory.load(address, 1));
        assertEquals(code, memory.load(address + 2, 1));
        assertEquals(value, (int) memory.load(address + 4, 2));
    }
}
