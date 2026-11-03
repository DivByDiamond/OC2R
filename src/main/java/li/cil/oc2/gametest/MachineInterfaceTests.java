package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.block.machine.MachineInterfaceBlock;
import li.cil.oc2.common.blockentity.machine.MachineInterfaceBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineInterfaceTests {
    private static final BlockPos BASE = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = BASE.east();

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineInterfaceTransfersItemsAndComponentsThroughRealChest(final GameTestHelper helper) {
        final var device = place(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        final ChestBlockEntity chest = TestSupport.blockEntity(helper, TARGET, ChestBlockEntity.class);
        final ItemStack named = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 40);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Conservation test"));
        device.inventory.setStackInSlot(0, named.copy());
        TestSupport.assertEquals(helper, "pushed", 25, device.pushItems(0, 0, 25));
        TestSupport.assertEquals(helper, "buffer remaining", 15, device.getBufferItem(0).getCount());
        TestSupport.assertEquals(helper, "chest", 25, chest.getItem(0).getCount());
        TestSupport.assertTrue(helper, "components preserved", ItemStack.isSameItemSameComponents(named, chest.getItem(0)));
        TestSupport.assertEquals(helper, "pulled", 10, device.pullItems(0, 1, 10));
        TestSupport.assertEquals(helper, "conserved", 40,
                device.getBufferItem(0).getCount() + device.getBufferItem(1).getCount() + chest.getItem(0).getCount());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineInterfaceHonorsSidedFurnaceRestrictions(final GameTestHelper helper) {
        final var device = place(helper);
        helper.setBlock(TARGET, Blocks.FURNACE.defaultBlockState());
        device.inventory.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 8));
        final int slots = device.getTargetSlotCount();
        for (int slot = 0; slot < slots; slot++) {
            TestSupport.assertEquals(helper, "side cannot fill input or result slot", 0, device.pushItems(0, slot, 8));
        }
        device.inventory.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.COAL, 8));
        int inserted = 0;
        for (int slot = 0; slot < slots; slot++) inserted += device.pushItems(0, slot, 8);
        TestSupport.assertEquals(helper, "fuel accepted", 8, inserted);
        TestSupport.assertEquals(helper, "empty source", 0, device.getBufferItem(0).getCount());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineInterfaceExposesReadOnlyStateAndRpcMethods(final GameTestHelper helper) {
        final var device = place(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        TestSupport.assertTrue(helper, "target ID", device.getTargetBlock().equals("minecraft:chest"));
        TestSupport.assertTrue(helper, "state snapshot", device.getTargetState().containsKey("facing"));
        final ObjectDevice rpc = new ObjectDevice(device);
        TestSupport.assertTrue(helper, "default-interface callbacks visible", rpc.getMethodGroups().size() >= 15);
        TestSupport.assertTrue(helper, "named RPC", device.getDeviceTypeNames().contains("machine_interface"));
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineInterfaceRejectsStaleHandles(final GameTestHelper helper) {
        final var device = place(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(BASE), false);
        try {
            device.getTargetState();
            throw TestSupport.failure(helper, "stale device read target");
        } catch (final IllegalStateException expected) {
            TestSupport.assertTrue(helper, "detached error", expected.getMessage().contains("no longer attached"));
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 160)
    public static void machineInterfaceDiscoveredThroughBus(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final var computer = ComputerFixture.place(helper, player);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        helper.setBlock(TestSupport.DEVICE_POS, li.cil.oc2.common.block.common.Blocks.MACHINE_INTERFACE.get()
                .defaultBlockState());
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.EAST);
        helper.runAfterDelay(60, () -> {
            TestSupport.assertTrue(helper, "machine RPC discovered", computer.devices().stream().anyMatch(
                    device -> device instanceof li.cil.oc2.api.bus.device.rpc.RPCDevice rpc
                            && rpc.getTypeNames().contains("machine_interface")));
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineBreakDropsNormalAndRecoveryItemsOnce(final GameTestHelper helper) {
        final var device = place(helper);
        device.inventory.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 13));
        device.inventory.setStackInSlot(18, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
        helper.getLevel().destroyBlock(helper.absolutePos(BASE), true);
        device.setRemoved(); // Repeated removal must not repeat inventory drops.
        final var bounds = new net.minecraft.world.phys.AABB(helper.absolutePos(BASE)).inflate(1);
        final int count = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds)
                .stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(stack -> stack.is(net.minecraft.world.item.Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
        TestSupport.assertEquals(helper, "all owned items dropped once", 20, count);
        helper.succeed();
    }

    private static MachineInterfaceBlockEntity place(final GameTestHelper helper) {
        helper.setBlock(BASE, li.cil.oc2.common.block.common.Blocks.MACHINE_INTERFACE.get().defaultBlockState()
                .setValue(MachineInterfaceBlock.FACING, Direction.EAST));
        return TestSupport.blockEntity(helper, BASE, MachineInterfaceBlockEntity.class);
    }

    private MachineInterfaceTests() {}
}
