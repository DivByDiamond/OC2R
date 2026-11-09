package li.cil.oc2.gametest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tablet.TabletMachine;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.common.tablet.display.TabletDisplayDevice;
import li.cil.oc2.common.tablet.display.TabletInputDevice;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/** Exercises the actual tablet VM's memory map and suspended framebuffer state. */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TabletDisplayTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void tabletHasBuiltInPixelDisplayAndAbsoluteInput(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final TabletMachine machine = boot(helper, id);
        helper.startSequence().thenWaitUntil(() -> {
            tick(helper, id);
            TestSupport.assertTrue(helper, "tablet guest running", machine.virtualMachine().getRunState() == VMRunState.RUNNING);
        }).thenExecute(() -> {
            // Join before inspecting a coherent device state; don't race guest stores.
            machine.virtualMachine().serialize();
            final CompoundTag display = machine.display().serializeNBT(helper.getLevel().registryAccess());
            final long frame = address(display, "framebuffer_address"), input = address(display, "input_address");
            final var memory = machine.virtualMachine().state.board.getMemoryMap();
            TestSupport.assertTrue(helper, "framebuffer and input have distinct mappings", frame != input);
            TestSupport.assertTrue(helper, "built-in RGB565 framebuffer mapped",
                    memory.getMemoryRange(frame).device instanceof SimpleFramebufferDevice);
            TestSupport.assertTrue(helper, "absolute input mapped",
                    memory.getMemoryRange(input).device instanceof TabletInputDevice);
            TestSupport.assertEquals(helper, "fixed framebuffer byte count", 256 * 160 * 2,
                    memory.getMemoryRange(frame).device.getLength());
            try {
                memory.store(input + 0x100, 18, 0);
                memory.store(input + 0x101, 0, 0);
                TestSupport.assertEquals(helper, "absolute X maximum", 255, (int) memory.load(input + 0x10c, 2));
                memory.store(input + 0x101, 1, 0);
                TestSupport.assertEquals(helper, "absolute Y maximum", 159, (int) memory.load(input + 0x10c, 2));
            } catch (final Exception error) { throw TestSupport.failure(helper, error.toString()); }
            forget(helper, id);
        }).thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void tabletPixelMemoryAndAddressesSurviveMachineSnapshot(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final TabletMachine machine = boot(helper, id);
        helper.startSequence().thenWaitUntil(() -> {
            tick(helper, id);
            TestSupport.assertTrue(helper, "tablet guest running", machine.virtualMachine().getRunState() == VMRunState.RUNNING);
        }).thenExecute(() -> {
            machine.virtualMachine().serialize();
            final long address = address(machine.display().serializeNBT(helper.getLevel().registryAccess()), "framebuffer_address");
            try { machine.virtualMachine().state.board.getMemoryMap().store(address + 16, 0xF81F, 1); }
            catch (final Exception error) { throw TestSupport.failure(helper, error.toString()); }
            final ByteBuffer frame = ByteBuffer.allocate(TabletDisplayDevice.FRAME_BYTES);
            TestSupport.assertTrue(helper, "frame can be copied", machine.display().copyFrame(frame, true));
            TestSupport.assertEquals(helper, "frame contains guest memory", 0xF81F,
                    frame.order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort(16) & 0xFFFF);
            final CompoundTag saved = machine.save();
            final TabletMachine restored = new TabletMachine(id, helper.getLevel(), List::of);
            restored.load(saved);
            final CompoundTag next = restored.display().serializeNBT(helper.getLevel().registryAccess());
            TestSupport.assertTrue(helper, "stable MMIO address", address(next, "framebuffer_address") == address);
            //? if >=26.1 {
            /*final byte[] pixels = next.getByteArray("pixels").orElseThrow();
            *///?} else {
            final byte[] pixels = next.getByteArray("pixels");
            //?}
            TestSupport.assertEquals(helper, "saved pixels survive", 0xF81F,
                    ByteBuffer.wrap(pixels).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort(16) & 0xFFFF);
            restored.suspend();
            forget(helper, id);
        }).thenSucceed();
    }

    private static TabletMachine boot(final GameTestHelper helper, final UUID id) {
        final TabletMachine machine = TabletManager.machineOf(helper.getLevel().getServer(), id);
        TestSupport.assertNotNull(helper, machine, "tablet");
        machine.inventory().getItemHandler(DeviceTypes.CPU).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.CPU_TIER_1.get()));
        machine.virtualMachine().start();
        return machine;
    }
    private static long address(final CompoundTag tag, final String key) {
        //? if >=26.1 {
        /*return tag.getLongOr(key, -1);
        *///?} else {
        return tag.getLong(key);
        //?}
    }
    private static void tick(final GameTestHelper helper, final UUID id) {
        TabletManager.tickFor(helper.getLevel().getServer(), Set.of(id));
    }
    private static void forget(final GameTestHelper helper, final UUID id) {
        try { TabletManager.discard(helper.getLevel().getServer(), id); }
        catch (final IOException error) { throw TestSupport.failure(helper, error.toString()); }
    }
    private TabletDisplayTests() {}
}
