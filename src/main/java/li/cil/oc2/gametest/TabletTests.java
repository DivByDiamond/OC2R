/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.vm.item.storage.HardDriveDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.tablet.TabletItem;
import li.cil.oc2.common.tablet.TabletMachine;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.common.tablet.TabletVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TabletTests {
    private static final int GRACE = 45;

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void tabletItemKeepsItsIdAndStacksToOne(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.TABLET.get());
        TestSupport.assertTrue(helper, "a new tablet has no id", TabletItem.getId(stack).isEmpty());
        final UUID id = TabletItem.getOrCreateId(stack);
        TestSupport.assertTrue(helper, "the id is stable", id.equals(TabletItem.getOrCreateId(stack)));
        TestSupport.assertTrue(helper, "the id survives a copy", id.equals(TabletItem.getId(stack.copy()).orElse(null)));
        TestSupport.assertEquals(helper, "tablets do not stack", 1, stack.getMaxStackSize());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void newTabletHasMemoryAndOnyxFlashButNoCpu(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final TabletMachine machine = TabletManager.machineOf(server(helper), id);
        TestSupport.assertNotNull(helper, machine, "machine");
        final var inventory = machine.inventory();
        TestSupport.assertTrue(helper, "16 MiB memory is built in", inventory.getItemHandler(DeviceTypes.MEMORY)
                .orElseThrow().getStackInSlot(0).is(Items.MEMORY_EXTRA_LARGE.get()));
        TestSupport.assertTrue(helper, "the OnyxOS flash is built in", inventory.getItemHandler(DeviceTypes.FLASH_MEMORY)
                .orElseThrow().getStackInSlot(0).is(Items.FLASH_MEMORY_ONYXOS.get()));
        TestSupport.assertTrue(helper, "there is no processor", inventory.getItemHandler(DeviceTypes.CPU)
                .orElseThrow().getStackInSlot(0).isEmpty());
        TestSupport.assertEquals(helper, "clock cap is a quarter of a computer", 12_500_000, TabletVirtualMachine.MAX_FREQUENCY);
        TestSupport.assertEquals(helper, "faster processors are capped", 12_500_000,
                machine.virtualMachine().limitFrequency(1_000_000_000));
        forget(helper, id);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void tabletWithoutFlashDoesNotBoot(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final TabletMachine machine = TabletManager.machineOf(server(helper), id);
        TestSupport.assertNotNull(helper, machine, "machine");
        machine.inventory().getItemHandler(DeviceTypes.FLASH_MEMORY).orElseThrow().setStackInSlot(0, ItemStack.EMPTY);
        machine.inventory().getItemHandler(DeviceTypes.CPU).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.CPU_TIER_1.get()));
        machine.virtualMachine().start();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server(helper), Set.of(id));
                    final var error = machine.virtualMachine().getBootError();
                    TestSupport.assertTrue(helper, "a boot error names the missing firmware",
                            error != null && !error.getString().isEmpty());
                })
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "it stays stopped",
                            machine.virtualMachine().getRunState() == VMRunState.STOPPED);
                    forget(helper, id);
                })
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void tabletSuspendsWithoutLosingStateAndResumes(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final MinecraftServer server = server(helper);
        final TabletMachine first = TabletManager.machineOf(server, id);
        TestSupport.assertNotNull(helper, first, "machine");
        first.inventory().getItemHandler(DeviceTypes.CPU).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.CPU_TIER_INF.get()));
        first.virtualMachine().start();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server, Set.of(id));
                    TestSupport.assertTrue(helper, "the guest is running",
                            first.virtualMachine().getRunState() == VMRunState.RUNNING);
                })
                .thenExecute(() -> TestSupport.assertTrue(helper, "clock is capped while running",
                        first.virtualMachine().state.board.getCpu().getFrequency()
                                <= TabletVirtualMachine.MAX_FREQUENCY))
                // Nobody holds the tablet any more: after the grace period it is saved and released.
                .thenExecuteFor(GRACE, () -> TabletManager.tickFor(server, Set.of()))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the machine was released", !TabletManager.isLive(id));
                    TestSupport.assertTrue(helper, "its state is in the world folder",
                            TabletManager.storeOf(server).exists(id));
                })
                .thenExecute(() -> {
                    final TabletMachine resumed = TabletManager.machineOf(server, id);
                    TestSupport.assertNotNull(helper, resumed, "resumed machine");
                    TestSupport.assertTrue(helper, "a new machine object was built", resumed != first);
                    TestSupport.assertTrue(helper, "processor survived", resumed.inventory()
                            .getItemHandler(DeviceTypes.CPU).orElseThrow().getStackInSlot(0).is(Items.CPU_TIER_INF.get()));
                    TestSupport.assertTrue(helper, "flash survived", resumed.inventory()
                            .getItemHandler(DeviceTypes.FLASH_MEMORY).orElseThrow().getStackInSlot(0)
                            .is(Items.FLASH_MEMORY_ONYXOS.get()));
                })
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server, Set.of(id));
                    TestSupport.assertTrue(helper, "the resumed guest runs again",
                            TabletManager.machineOf(server, id).virtualMachine().getRunState() == VMRunState.RUNNING);
                })
                .thenExecute(() -> forget(helper, id))
                .thenSucceed();
    }

    /** A server crash never runs the suspend path; the periodic checkpoint is all that is left. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void runningTabletIsCheckpointedAndSurvivesACrash(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final MinecraftServer server = server(helper);
        final TabletMachine first = TabletManager.machineOf(server, id);
        TestSupport.assertNotNull(helper, first, "machine");
        first.inventory().getItemHandler(DeviceTypes.CPU).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.CPU_TIER_INF.get()));
        first.virtualMachine().start();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server, Set.of(id));
                    TestSupport.assertTrue(helper, "the guest is running",
                            first.virtualMachine().getRunState() == VMRunState.RUNNING);
                })
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "a running tablet has no file before its first checkpoint",
                            !TabletManager.storeOf(server).exists(id));
                    TestSupport.assertTrue(helper, "the checkpoint was written", TabletManager.checkpointNow(server, id));
                    TestSupport.assertTrue(helper, "its state is in the world folder",
                            TabletManager.storeOf(server).exists(id));
                    TestSupport.assertTrue(helper, "the machine was not released", TabletManager.isLive(id));
                })
                .thenExecuteFor(10, () -> TabletManager.tickFor(server, Set.of(id)))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the guest keeps running after a checkpoint",
                            first.virtualMachine().getRunState() == VMRunState.RUNNING);
                    TabletManager.crash(id);
                    TestSupport.assertTrue(helper, "the crashed machine is gone", !TabletManager.isLive(id));
                    final TabletMachine restored = TabletManager.machineOf(server, id);
                    TestSupport.assertNotNull(helper, restored, "restored machine");
                    TestSupport.assertTrue(helper, "a new machine object was built", restored != first);
                    TestSupport.assertTrue(helper, "the processor came back from the checkpoint", restored.inventory()
                            .getItemHandler(DeviceTypes.CPU).orElseThrow().getStackInSlot(0).is(Items.CPU_TIER_INF.get()));
                })
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server, Set.of(id));
                    TestSupport.assertTrue(helper, "the restored guest runs again",
                            TabletManager.machineOf(server, id).virtualMachine().getRunState() == VMRunState.RUNNING);
                })
                .thenExecute(() -> forget(helper, id))
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void tabletMountsAHardDriveAndKeepsItAcrossSuspend(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final MinecraftServer server = server(helper);
        final TabletMachine first = TabletManager.machineOf(server, id);
        TestSupport.assertNotNull(helper, first, "machine");
        TestSupport.assertTrue(helper, "a new tablet has no hard drive", first.inventory()
                .getItemHandler(DeviceTypes.HARD_DRIVE).orElseThrow().getStackInSlot(0).isEmpty());
        first.inventory().getItemHandler(DeviceTypes.CPU).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.CPU_TIER_INF.get()));
        first.inventory().getItemHandler(DeviceTypes.HARD_DRIVE).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.HARD_DRIVE_SMALL.get()));
        first.virtualMachine().start();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server, Set.of(id));
                    TestSupport.assertTrue(helper, "the guest runs with a hard drive",
                            first.virtualMachine().getRunState() == VMRunState.RUNNING);
                    TestSupport.assertTrue(helper, "the hard drive is a device of the guest", hasHardDrive(first));
                })
                .thenExecuteFor(GRACE, () -> TabletManager.tickFor(server, Set.of()))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the machine was released", !TabletManager.isLive(id));
                    final TabletMachine resumed = TabletManager.machineOf(server, id);
                    TestSupport.assertNotNull(helper, resumed, "resumed machine");
                    TestSupport.assertTrue(helper, "the hard drive survived", resumed.inventory()
                            .getItemHandler(DeviceTypes.HARD_DRIVE).orElseThrow().getStackInSlot(0)
                            .is(Items.HARD_DRIVE_SMALL.get()));
                })
                .thenWaitUntil(() -> {
                    TabletManager.tickFor(server, Set.of(id));
                    final TabletMachine resumed = TabletManager.machineOf(server, id);
                    TestSupport.assertTrue(helper, "the resumed guest runs again",
                            resumed.virtualMachine().getRunState() == VMRunState.RUNNING);
                    TestSupport.assertTrue(helper, "the hard drive is mounted again", hasHardDrive(resumed));
                })
                .thenExecute(() -> forget(helper, id))
                .thenSucceed();
    }

    private static boolean hasHardDrive(final TabletMachine machine) {
        return machine.virtualMachine().busController.getDevices().stream()
                .anyMatch(HardDriveDevice.class::isInstance);
    }

    // --------------------------------------------------------------------- //

    private static MinecraftServer server(final GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    /** Releases the machine and removes its file so repeated runs do not fill the test world. */
    private static void forget(final GameTestHelper helper, final UUID id) {
        try {
            TabletManager.discard(server(helper), id);
        } catch (final IOException e) {
            throw TestSupport.failure(helper, "cannot remove test state: " + e);
        }
    }

    private TabletTests() {
    }
}
