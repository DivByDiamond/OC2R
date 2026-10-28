package li.cil.oc2.common.vm.device.input;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.virtio.AbstractVirtIOInputDevice;
import li.cil.sedna.evdev.EvdevKeys;

/** A single VirtIO input device advertising both keyboard and relative pointer capabilities. */
public final class UnifiedInputDevice extends AbstractVirtIOInputDevice {
    public UnifiedInputDevice(final MemoryMap memoryMap) {
        super(memoryMap);
    }

    /** Emits a key or pointer button followed by SYN_REPORT. */
    public void sendKeyEvent(final int code, final boolean down) {
        putEvents(packEvent(1, code, down ? 1 : 0), packEvent(0, 0, 0));
    }

    /** Emits relative X/Y motion or vertical wheel movement followed by SYN_REPORT. */
    public void sendRelativeEvent(final int axis, final int amount) {
        putEvents(packEvent(2, axis, amount), packEvent(0, 0, 0));
    }

    @Override
    protected int generateConfigUnion(final int select, final int subsel, final ByteBuffer buffer) {
        if (select == VIRTIO_INPUT_CFG_SELECT_ID_NAME) {
            buffer.put("OC2R keyboard and mouse".getBytes(StandardCharsets.US_ASCII));
        } else if (select == VIRTIO_INPUT_CFG_SELECT_EV_BITS) {
            final BitSet bits = new BitSet();
            if (subsel == 1) {
                for (final int key : EvdevKeys.ALL_KEYS) bits.set(key);
                // International, Print Screen, Meta and Compose keys absent from Sedna's list.
                for (final int key : new int[] {86, 89, 99, 125, 126, 127}) bits.set(key);
                // Linux BTN_LEFT through BTN_TASK, including side/extra buttons.
                bits.set(0x110, 0x118);
            } else if (subsel == 2) {
                bits.set(0);
                bits.set(1);
                bits.set(8);
            } else if (subsel == 20) {
                bits.set(0);
            }
            buffer.put(bits.toByteArray());
        }
        return buffer.position();
    }
}
