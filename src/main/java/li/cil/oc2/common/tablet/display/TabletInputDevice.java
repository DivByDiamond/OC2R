package li.cil.oc2.common.tablet.display;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.virtio.AbstractVirtIOInputDevice;
import li.cil.sedna.evdev.EvdevKeys;

/** A keyboard and absolute pixel pointer for the tablet's fixed display. */
public final class TabletInputDevice extends AbstractVirtIOInputDevice {
    public TabletInputDevice(final MemoryMap memoryMap) {
        super(memoryMap);
    }

    public void sendKey(final int code, final boolean down) {
        putEvents(packEvent(1, code, down ? 1 : 0), packEvent(0, 0, 0));
    }

    public void sendPointer(final int x, final int y, final int buttons, final int changed) {
        final java.util.ArrayList<Long> events = new java.util.ArrayList<>();
        events.add(packEvent(3, 0, x));
        events.add(packEvent(3, 1, y));
        for (int button = 0; button < 3; button++) {
            if ((changed & (1 << button)) != 0) events.add(packEvent(1, 0x110 + button,
                    (buttons & (1 << button)) != 0 ? 1 : 0));
        }
        events.add(packEvent(0, 0, 0));
        final long[] packed = new long[events.size()];
        for (int index = 0; index < packed.length; index++) packed[index] = events.get(index);
        putEvents(packed);
    }

    @Override
    protected int generateConfigUnion(final int select, final int subsel, final ByteBuffer buffer) {
        if (select == VIRTIO_INPUT_CFG_SELECT_ID_NAME) {
            buffer.put("OC2R tablet keyboard and pointer".getBytes(StandardCharsets.US_ASCII));
        } else if (select == VIRTIO_INPUT_CFG_SELECT_EV_BITS) {
            final BitSet bits = new BitSet();
            if (subsel == 1) {
                for (final int key : EvdevKeys.ALL_KEYS) bits.set(key);
                for (final int key : new int[] {86, 89, 99, 125, 126, 127}) bits.set(key);
                bits.set(0x110, 0x113);
            } else if (subsel == 3) {
                bits.set(0, 2);
            }
            buffer.put(bits.toByteArray());
        } else if (select == 0x12 && (subsel == 0 || subsel == 1)) {
            // virtio_input_absinfo: min, max, fuzz, flat, resolution (little endian).
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(0).putInt(subsel == 0 ? TabletDisplayDevice.WIDTH - 1 : TabletDisplayDevice.HEIGHT - 1);
            buffer.putInt(0).putInt(0).putInt(0);
        }
        return buffer.position();
    }
}
