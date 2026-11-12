/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.computer.CPUItem;
import li.cil.oc2.common.item.computer.GPUItem;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import net.minecraft.core.Direction;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Every processor and graphics card tier in a real computer: the clock and the screen follow the item. */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HardwareTierTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void cpuTier1SetsTheGuestClock(final GameTestHelper helper) {
        cpuTier(helper, Items.CPU_TIER_1.get());
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void cpuTier2SetsTheGuestClock(final GameTestHelper helper) {
        cpuTier(helper, Items.CPU_TIER_2.get());
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void cpuTier3SetsTheGuestClock(final GameTestHelper helper) {
        cpuTier(helper, Items.CPU_TIER_3.get());
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void cpuTier4SetsTheGuestClock(final GameTestHelper helper) {
        cpuTier(helper, Items.CPU_TIER_4.get());
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void gpuTier1SizesTheMonitor(final GameTestHelper helper) {
        gpuTier(helper, Items.GPU_TIER_1.get(), 320, 200);
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void gpuTier2SizesTheMonitor(final GameTestHelper helper) {
        gpuTier(helper, Items.GPU_TIER_2.get(), 640, 400);
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void gpuTier3SizesTheMonitor(final GameTestHelper helper) {
        gpuTier(helper, Items.GPU_TIER_3.get(), 1024, 768);
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void gpuTier4SizesTheMonitor(final GameTestHelper helper) {
        gpuTier(helper, Items.GPU_TIER_4.get(), 1920, 1080);
    }

    /** Without a graphics card the guest still runs on its UART terminal, but the monitor gets no framebuffer. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void monitorStaysDarkWithoutAGpu(final GameTestHelper helper) {
        final ComputerFixture computer = computerWithMonitor(helper);
        final MonitorBlockEntity monitor = blockEntity(helper, DEVICE_POS, MonitorBlockEntity.class);
        helper.startSequence()
            .thenExecuteAfter(60, computer::start)
            .thenWaitUntil(() -> computer.assertRunState(VMRunState.RUNNING, "guest without a graphics card"))
            .thenExecuteAfter(40, () -> {
                computer.assertNoBootError();
                assertTrue(helper, "the monitor is on the bus", computer.devices().stream()
                    .anyMatch(MonitorDevice.class::isInstance));
                assertTrue(helper, "a monitor without a graphics card has no framebuffer", !monitor.isMounted());
            })
            .thenSucceed();
    }

    private static void cpuTier(final GameTestHelper helper, final CPUItem cpu) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(cpu));
        assertTrue(helper, "the tier has a real clock", cpu.getFrequency() > 0);
        helper.startSequence()
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> computer.assertRunState(VMRunState.RUNNING, "guest on " + cpu))
            .thenExecute(() -> {
                computer.assertNoBootError();
                final int clock = ((AbstractVirtualMachine) computer.virtualMachine())
                    .state.board.getCpu().getFrequency();
                assertEquals(helper, "guest clock of " + cpu, cpu.getFrequency(), clock);
            })
            .thenSucceed();
    }

    private static void gpuTier(final GameTestHelper helper, final GPUItem gpu, final int width, final int height) {
        assertEquals(helper, "width of " + gpu, width, gpu.getWidth());
        assertEquals(helper, "height of " + gpu, height, gpu.getHeight());
        final ComputerFixture computer = computerWithMonitor(helper);
        computer.install(DeviceTypes.GPU, new ItemStack(gpu));
        final MonitorBlockEntity monitor = blockEntity(helper, DEVICE_POS, MonitorBlockEntity.class);
        helper.startSequence()
            .thenExecuteAfter(60, computer::start)
            .thenWaitUntil(() -> {
                computer.assertRunState(VMRunState.RUNNING, "guest with " + gpu);
                assertTrue(helper, "the monitor framebuffer is mounted", monitor.isMounted());
            })
            .thenExecute(() -> {
                computer.assertNoBootError();
                final MonitorDevice device = (MonitorDevice) computer.devices().stream()
                    .filter(MonitorDevice.class::isInstance).findFirst()
                    .orElseThrow(() -> failure(helper, "no monitor on the bus: " + computer.describe()));
                assertEquals(helper, "framebuffer width with " + gpu, width, device.getWidth());
                assertEquals(helper, "framebuffer height with " + gpu, height, device.getHeight());
            })
            .thenSucceed();
    }

    private static ComputerFixture computerWithMonitor(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), CABLE_POS);
        connectInterface(helper, CABLE_POS, Direction.WEST);
        place(helper, player, new ItemStack(Items.MONITOR.get()), DEVICE_POS);
        connectInterface(helper, CABLE_POS, Direction.EAST);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_INF.get()));
        return computer;
    }

    private HardwareTierTests() {
    }
}
