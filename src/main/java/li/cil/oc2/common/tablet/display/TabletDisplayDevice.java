package li.cil.oc2.common.tablet.display;

import java.nio.ByteBuffer;
import java.util.BitSet;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.bus.device.util.optional.OptionalAddress;
import li.cil.oc2.common.bus.device.util.optional.OptionalInterrupt;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.oc2.common.vm.device.frame.FramePresentAttachment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Built-in display/input; retained with the tablet machine, without a removable GPU or keyboard. */
public final class TabletDisplayDevice implements VMDevice {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 160;
    public static final int FRAME_BYTES = WIDTH * HEIGHT * 2;
    private final OptionalAddress framebufferAddress = new OptionalAddress();
    private final OptionalAddress inputAddress = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private final FramePresentAttachment presentation = new FramePresentAttachment();
    private final BitSet heldKeys = new BitSet(768);
    @Nullable private SimpleFramebufferDevice framebuffer;
    @Nullable private TabletInputDevice input;
    @Nullable private ByteBuffer pixels;
    private byte[] savedPixels = new byte[FRAME_BYTES];
    private CompoundTag savedInput;
    private int buttons;
    private int pointerX = -1;
    private int pointerY = -1;

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(8192)) return VMDeviceLoadResult.fail();
        pixels = ByteBuffer.allocateDirect(FRAME_BYTES);
        pixels.put(savedPixels).clear();
        framebuffer = new SimpleFramebufferDevice(WIDTH, HEIGHT, pixels);
        input = new TabletInputDevice(context.getMemoryMap());
        if (!framebufferAddress.claim(context, framebuffer) || !presentation.mount(context, framebuffer)
                || !inputAddress.claim(context, input) || !interrupt.claim(context)) {
            unmount();
            return VMDeviceLoadResult.fail();
        }
        input.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        if (savedInput != null) NBTSerialization.deserialize(savedInput, input);
        releaseInput();
        return VMDeviceLoadResult.success();
    }

    public boolean copyFrame(final ByteBuffer target, final boolean force) {
        final SimpleFramebufferDevice current = framebuffer;
        return current != null && current.copyFrame(target, force);
    }

    public void key(final int code, final boolean down) {
        if (input == null || code < 0 || code > 767 || (code >= 0x110 && code <= 0x117)) return;
        if (heldKeys.get(code) == down) return;
        heldKeys.set(code, down);
        input.sendKey(code, down);
    }

    public void pointer(final int x, final int y, final int nextButtons) {
        if (input == null || x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || (nextButtons & ~7) != 0) return;
        final int changed = buttons ^ nextButtons;
        if (changed == 0 && pointerX == x && pointerY == y) return;
        pointerX = x;
        pointerY = y;
        buttons = nextButtons;
        input.sendPointer(x, y, buttons, changed);
    }

    public void releaseInput() {
        final TabletInputDevice current = input;
        if (current != null) {
            for (int key = heldKeys.nextSetBit(0); key >= 0; key = heldKeys.nextSetBit(key + 1)) {
                current.sendKey(key, false);
            }
            if (buttons != 0) {
                // Moving off the application canvas before release cancels a pending click/drag.
                current.sendPointer(0, 0, 0, buttons);
                pointerX = 0;
                pointerY = 0;
            }
        }
        heldKeys.clear();
        buttons = 0;
    }

    @Override
    public void unmount() {
        releaseInput();
        capturePixels();
        if (input != null) savedInput = NBTSerialization.serialize(input);
        input = null;
        if (framebuffer != null) {
            presentation.unmount(framebuffer);
            framebuffer.close();
            framebuffer = null;
        }
        pixels = null;
    }

    @Override
    public void dispose() {
        savedPixels = new byte[FRAME_BYTES];
        savedInput = null;
        framebufferAddress.clear();
        inputAddress.clear();
        interrupt.clear();
        presentation.clear();
    }

    private void capturePixels() {
        if (pixels != null) pixels.duplicate().clear().get(savedPixels);
    }

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        capturePixels();
        final CompoundTag tag = new CompoundTag();
        tag.putByteArray("pixels", savedPixels);
        if (framebufferAddress.isPresent()) tag.putLong("framebuffer_address", framebufferAddress.getAsLong());
        if (inputAddress.isPresent()) tag.putLong("input_address", inputAddress.getAsLong());
        if (interrupt.isPresent()) tag.putInt("interrupt", interrupt.getAsInt());
        if (input != null) savedInput = NBTSerialization.serialize(input);
        if (savedInput != null) tag.put("input", savedInput);
        tag.put("presentation", presentation.save(framebuffer));
        return tag;
    }

    @Override
    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag tag) {
        //? if >=26.1 {
        /*final byte[] restored = tag.getByteArray("pixels").orElse(new byte[0]);
        if (tag.contains("framebuffer_address")) framebufferAddress.set(tag.getLongOr("framebuffer_address", 0));
        if (tag.contains("input_address")) inputAddress.set(tag.getLongOr("input_address", 0));
        if (tag.contains("interrupt")) interrupt.set(tag.getIntOr("interrupt", 0));
        *///?} else {
        final byte[] restored = tag.getByteArray("pixels");
        if (tag.contains("framebuffer_address")) framebufferAddress.set(tag.getLong("framebuffer_address"));
        if (tag.contains("input_address")) inputAddress.set(tag.getLong("input_address"));
        if (tag.contains("interrupt")) interrupt.set(tag.getInt("interrupt"));
        //?}
        savedPixels = restored.length == FRAME_BYTES ? restored : new byte[FRAME_BYTES];
        savedInput = NBTUtils.getChildTag(tag, "input");
        presentation.load(NBTUtils.getChildTag(tag, "presentation"));
    }
}
