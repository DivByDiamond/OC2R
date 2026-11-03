package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.media.MediaStationBlock;
import li.cil.oc2.common.blockentity.media.MediaStationBlockEntity;
import li.cil.oc2.common.bus.device.DeviceGroup;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.ItemHandler;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MediaStationTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void mediaStationHasIndependentTypedSlotsAndTwoDevices(final GameTestHelper helper) {
        final var station = place(helper);
        final var inventory = inventory(station);
        TestSupport.assertEquals(helper, "two slots", 2, inventory.getSlots());
        TestSupport.assertTrue(helper, "reject flash in floppy bay", !inventory.insertItem(0,
                new ItemStack(Items.FLASH_MEMORY_SMALL.get()), false).isEmpty());
        TestSupport.assertTrue(helper, "reject floppy in programmer bay", !inventory.insertItem(1,
                new ItemStack(Items.FLOPPY.get()), false).isEmpty());
        fill(helper, inventory);
        final var devices = Platform.capabilities().getBlockCapability(Capabilities.Device.BLOCK,
                helper.getLevel(), station.getBlockPos(), Direction.SOUTH);
        TestSupport.assertTrue(helper, "two storage identities in device group",
                devices instanceof DeviceGroup group && group.getDevices().size() == 2);
        TestSupport.assertTrue(helper, "front is not the bus port", Platform.capabilities().getBlockCapability(
                Capabilities.Device.BLOCK, helper.getLevel(), station.getBlockPos(), Direction.NORTH) == null);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void mediaStationFlashWriteLockDoesNotLockFloppy(final GameTestHelper helper) {
        final var station = place(helper);
        final var inventory = inventory(station);
        fill(helper, inventory);
        TestSupport.assertTrue(helper, "exclusive firmware lease", station.beginWriting());
        try {
            TestSupport.assertTrue(helper, "flash removal locked", inventory.extractItem(1, 1, false).isEmpty());
            TestSupport.assertTrue(helper, "floppy remains usable", !inventory.extractItem(0, 1, false).isEmpty());
            TestSupport.assertTrue(helper, "floppy reinsertion", inventory.insertItem(0,
                    new ItemStack(Items.FLOPPY.get()), false).isEmpty());
        } finally { station.endWriting(); }
        TestSupport.assertTrue(helper, "flash unlocks after lease", !inventory.extractItem(1, 1, false).isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void mediaStationBothSlotsSurviveDetachedSaveAndLoad(final GameTestHelper helper) {
        final var station = place(helper);
        final var inventory = inventory(station);
        fill(helper, inventory);
        inventory.getStackInSlot(0).set(DataComponents.CUSTOM_NAME, Component.literal("Floppy marker"));
        inventory.getStackInSlot(1).set(DataComponents.CUSTOM_NAME, Component.literal("Firmware marker"));
        final var lookup = helper.getLevel().registryAccess();
        final var saved = station.saveWithFullMetadata(lookup);
        final var loaded = BlockEntity.loadStatic(station.getBlockPos(), station.getBlockState(), saved, lookup);
        TestSupport.assertTrue(helper, "registered station type restored", loaded instanceof MediaStationBlockEntity);
        final var restored = (MediaStationBlockEntity) loaded;
        TestSupport.assertTrue(helper, "detached load", restored.getLevel() == null);
        TestSupport.assertTrue(helper, "floppy components survive",
                ItemStack.isSameItemSameComponents(inventory.getStackInSlot(0), restored.getRenderedFloppy()));
        TestSupport.assertTrue(helper, "flash components survive",
                ItemStack.isSameItemSameComponents(inventory.getStackInSlot(1), restored.getFloppy()));
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void mediaStationDestroyedDuringWriteDropsEachMediaOnce(final GameTestHelper helper) {
        final var station = place(helper);
        final var inventory = inventory(station);
        fill(helper, inventory);
        TestSupport.assertTrue(helper, "lease acquired", station.beginWriting());
        helper.getLevel().destroyBlock(station.getBlockPos(), true);
        station.setRemoved();
        station.endWriting();
        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(station.getBlockPos()).inflate(1));
        TestSupport.assertEquals(helper, "one floppy", 1, drops.stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(Items.FLOPPY.get())).mapToInt(ItemStack::getCount).sum());
        TestSupport.assertEquals(helper, "one flash", 1, drops.stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(Items.FLASH_MEMORY_SMALL.get())).mapToInt(ItemStack::getCount).sum());
        TestSupport.assertTrue(helper, "stale handlers cannot extract dropped media",
                inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void mediaStationCanExtractFloppyWithoutEjectingFlash(final GameTestHelper helper) {
        final var station = place(helper);
        final var inventory = inventory(station);
        fill(helper, inventory);
        station.ejectFloppy(null);
        TestSupport.assertTrue(helper, "floppy removed", station.getRenderedFloppy().isEmpty());
        TestSupport.assertTrue(helper, "flash retained", !station.getDiskItemStack().isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
            timeoutTicks = 160)
    public static void mediaStationBothDevicesAreDiscoveredThroughOneBusPort(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final var computer = ComputerFixture.place(helper, player);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        helper.setBlock(TestSupport.DEVICE_POS, Blocks.MEDIA_STATION.get().defaultBlockState()
                .setValue(MediaStationBlock.FACING, Direction.EAST));
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.EAST);
        helper.runAfterDelay(60, () -> {
            TestSupport.assertTrue(helper, "floppy storage discovered", computer.devices().stream().anyMatch(
                    li.cil.oc2.common.bus.device.vm.block.disk.DiskDriveDevice.class::isInstance));
            TestSupport.assertTrue(helper, "flash storage discovered", computer.devices().stream().anyMatch(
                    li.cil.oc2.common.bus.device.vm.block.flash.FlashMemoryFlasherDevice.class::isInstance));
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void mediaStationAcceptsHddInProgrammerBayOnly(final GameTestHelper helper) {
        final var station = place(helper);
        final var inventory = inventory(station);
        TestSupport.assertTrue(helper, "HDD does not fit floppy bay", !inventory.insertItem(0,
                new ItemStack(Items.HARD_DRIVE_SMALL.get()), false).isEmpty());
        TestSupport.assertTrue(helper, "HDD fits programmer bay", inventory.insertItem(1,
                new ItemStack(Items.HARD_DRIVE_SMALL.get()), false).isEmpty());
        TestSupport.assertTrue(helper, "HDD retained", station.getDiskItemStack().is(Items.HARD_DRIVE_SMALL.get()));
        helper.succeed();
    }

    private static void fill(final GameTestHelper helper, final ItemHandler inventory) {
        TestSupport.assertTrue(helper, "insert floppy", inventory.insertItem(0,
                new ItemStack(Items.FLOPPY.get()), false).isEmpty());
        TestSupport.assertTrue(helper, "insert flash", inventory.insertItem(1,
                new ItemStack(Items.FLASH_MEMORY_SMALL.get()), false).isEmpty());
    }

    private static MediaStationBlockEntity place(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.MEDIA_STATION.get().defaultBlockState()
                .setValue(MediaStationBlock.FACING, Direction.NORTH));
        return TestSupport.blockEntity(helper, POS, MediaStationBlockEntity.class);
    }

    private static ItemHandler inventory(final MediaStationBlockEntity station) {
        return Platform.capabilities().getBlockCapability(Capabilities.ItemHandler.BLOCK,
                station.getLevel(), station.getBlockPos(), Direction.NORTH);
    }

    private MediaStationTests() {}
}
