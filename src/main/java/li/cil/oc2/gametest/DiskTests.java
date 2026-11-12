/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.bus.device.vm.item.storage.HardDriveDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.storage.LevelResource;

/** Hard drives: the data lives in a blob file that follows the item, and files nobody uses are cleaned up. */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DiskTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void hardDriveKeepsItsDataAcrossHotSwapAndMovingToAnotherComputer(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture first = bootable(ComputerFixture.place(helper, player));
        first.install(DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_SMALL.get()));
        final BlockPos secondPos = new BlockPos(6, WORK_Y, 6);
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), secondPos.west());
        final ComputerFixture second = bootable(ComputerFixture.place(helper, player, secondPos));
        final UUID[] handle = new UUID[1];
        final ItemStack[] drive = new ItemStack[1];

        helper.startSequence()
            .thenExecute(first::start)
            .thenWaitUntil(() -> {
                first.assertRunState(VMRunState.RUNNING, "guest with a hard drive");
                handle[0] = mountedHandle(helper, first);
                assertTrue(helper, "the mounted drive has a blob", handle[0] != null);
                assertTrue(helper, "its blob file exists", Files.isRegularFile(blobFile(helper, handle[0])));
            })
            // Hot swap: out while the guest runs, and back in.
            .thenExecute(() -> drive[0] = first.uninstall(DeviceTypes.HARD_DRIVE))
            .thenExecuteAfter(20, () -> {
                first.assertRunState(VMRunState.RUNNING, "guest after its drive was pulled");
                first.assertNoError();
                assertTrue(helper, "the blob stays while the drive is out", Files.isRegularFile(blobFile(helper, handle[0])));
                first.install(DeviceTypes.HARD_DRIVE, drive[0]);
            })
            .thenWaitUntil(() -> {
                first.assertRunState(VMRunState.RUNNING, "guest after its drive was put back");
                assertTrue(helper, "the same blob is mounted again", handle[0].equals(mountedHandle(helper, first)));
            })
            // Move it to another computer.
            .thenExecute(() -> {
                first.stop();
                drive[0] = first.uninstall(DeviceTypes.HARD_DRIVE);
                second.install(DeviceTypes.HARD_DRIVE, drive[0]);
                second.start();
            })
            .thenWaitUntil(() -> {
                second.assertRunState(VMRunState.RUNNING, "second guest with the moved drive");
                assertTrue(helper, "the second computer mounts the very same blob", handle[0].equals(mountedHandle(helper, second)));
            })
            .thenExecute(() -> {
                second.stop();
                assertTrue(helper, "the blob survives all of it", Files.isRegularFile(blobFile(helper, handle[0])));
            })
            .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void orphanedBlobsAreCleanedUpButUsedOnesStay(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture computer = bootable(ComputerFixture.place(helper, player));
        computer.install(DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_SMALL.get()));
        final UUID stray = UUID.randomUUID();
        final UUID[] used = new UUID[1];

        helper.startSequence()
            .thenExecute(computer::start)
            .thenWaitUntil(() -> {
                used[0] = mountedHandle(helper, computer);
                assertTrue(helper, "the drive is mounted", used[0] != null);
            })
            .thenExecute(() -> {
                try {
                    Files.write(blobFile(helper, stray), new byte[] {1, 2, 3});
                } catch (final IOException e) {
                    throw failure(helper, "cannot create the stray blob: " + e);
                }
                BlobStorage.cleanupOrphaned();
            })
            .thenWaitUntil(() -> assertTrue(helper, "the orphaned blob is deleted", !Files.exists(blobFile(helper, stray))))
            .thenExecute(() -> assertTrue(helper, "the blob of a mounted drive is kept",
                Files.isRegularFile(blobFile(helper, used[0]))))
            .thenSucceed();
    }

    private static ComputerFixture bootable(final ComputerFixture computer) {
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        return computer;
    }

    private static Path blobFile(final GameTestHelper helper, final UUID handle) {
        return helper.getLevel().getServer().getWorldPath(new LevelResource(API.MOD_ID + "-blobs")).resolve(handle.toString());
    }

    /** The blob handle of the hard drive that is currently mounted in the computer, or null before it has one. */
    private static UUID mountedHandle(final GameTestHelper helper, final ComputerFixture computer) {
        for (final Object device : computer.devices()) {
            if (device instanceof HardDriveDevice drive) {
                try {
                    final Field field = AbstractBlockStorageDevice.class.getDeclaredField("blobHandle");
                    field.setAccessible(true); // NOPMD - the handle is internal state with no accessor
                    final UUID handle = (UUID) field.get(drive);
                    if (handle != null) return handle;
                } catch (final ReflectiveOperationException e) {
                    throw failure(helper, "cannot read the blob handle: " + e);
                }
            }
        }
        return null;
    }

    private DiskTests() {
    }
}
