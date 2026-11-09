package li.cil.oc2.common.tablet.display;

import static org.junit.jupiter.api.Assertions.*;

import li.cil.sedna.device.memory.ByteBufferMemory;
import li.cil.sedna.memory.SimpleMemoryMap;
import org.junit.jupiter.api.Test;

class TabletInputDeviceTest {
    @Test void advertisesAbsolutePixelRangesAndAllPointerButtons() {
        final TabletInputDevice input = new TabletInputDevice(new SimpleMemoryMap());
        select(input, 17, 3);
        assertEquals(3, input.load(0x108, 0));
        select(input, 18, 0);
        assertEquals(0, input.load(0x108, 2));
        assertEquals(255, input.load(0x10c, 2));
        select(input, 18, 1);
        assertEquals(159, input.load(0x10c, 2));
        select(input, 17, 1);
        for (int button = 0x110; button < 0x113; button++)
            assertNotEquals(0, input.load(0x108 + button / 8, 0) & (1 << (button % 8)));
    }

    @Test void emitsCoordinatesAndButtonAsOneGuestPacket() throws Exception {
        final SimpleMemoryMap map = new SimpleMemoryMap();
        try (ByteBufferMemory ram = new ByteBufferMemory(65536)) {
            assertTrue(map.addDevice(0, ram));
            final TabletInputDevice input = new TabletInputDevice(map);
            for (int i = 0; i < 8; i++) {
                ram.store(0x1000 + i * 16, 0x4000 + i * 8, 3);
                ram.store(0x1008 + i * 16, 8, 2);
                ram.store(0x100c + i * 16, 2, 1);
                ram.store(0x2004 + i * 2, i, 1);
            }
            ram.store(0x2002, 8, 1);
            input.store(0x070, 3, 2);
            input.store(0x024, 1, 2);
            input.store(0x020, 1, 2);
            input.store(0x070, 11, 2);
            input.store(0x030, 0, 2);
            input.store(0x038, 8, 2);
            input.store(0x080, 0x1000, 2);
            input.store(0x090, 0x2000, 2);
            input.store(0x0a0, 0x3000, 2);
            input.store(0x044, 1, 2);
            input.store(0x070, 15, 2);
            input.sendPointer(30, 98, 1, 1);
            input.sendPointer(0, 0, 0, 1);
            input.step(1);
            assertEquals(8, ram.load(0x3002, 1));
            event(ram, 0, 3, 0, 30);
            event(ram, 1, 3, 1, 98);
            event(ram, 2, 1, 0x110, 1);
            event(ram, 3, 0, 0, 0);
            event(ram, 4, 3, 0, 0);
            event(ram, 5, 3, 1, 0);
            event(ram, 6, 1, 0x110, 0);
        }
    }

    private static void select(final TabletInputDevice device, final int type, final int axis) {
        device.store(0x100, type, 0);
        device.store(0x101, axis, 0);
    }
    private static void event(final ByteBufferMemory ram, final int index, final int type,
            final int code, final int value) throws Exception {
        assertEquals(type, ram.load(0x4000 + index * 8, 1));
        assertEquals(code, ram.load(0x4002 + index * 8, 1));
        assertEquals(value, (int) ram.load(0x4004 + index * 8, 2));
    }
}
