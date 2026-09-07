/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RedstoneInterfaceTests {
    @GameTest(template = TestSupport.TEMPLATE)
    public static void redstoneInterfaceCanBePlaced(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.place(helper, player, new ItemStack(Items.REDSTONE_INTERFACE.get()), TestSupport.DEVICE_POS);
        if (helper.getBlockState(TestSupport.DEVICE_POS).isAir()) {
            throw new GameTestAssertException("redstone interface not placed at " + TestSupport.DEVICE_POS);
        }
        // also verify a block entity was created
        TestSupport.assertNotNull(helper, helper.getBlockEntity(TestSupport.DEVICE_POS),
            "redstone interface block entity at " + TestSupport.DEVICE_POS);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void redstoneInterfaceAttachesToComputerViaBus(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> base[0] = computer.deviceCount())
            .thenExecute(() -> placeInterface(helper))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = computer.deviceCount();
                if (withNeighbor <= base[0]) {
                    throw new GameTestAssertException(
                        "redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ") "
                            + computer.describe());
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void redstoneInterfaceDeviceCountReturnsAfterRemoval(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> base[0] = computer.deviceCount())
            .thenExecute(() -> placeInterface(helper))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = computer.deviceCount();
                if (withNeighbor <= base[0]) {
                    throw new GameTestAssertException(
                        "redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ")");
                }
            })
            .thenExecute(() -> TestSupport.breakBlock(helper, TestSupport.DEVICE_POS))
            .thenExecuteAfter(60, () -> {
                final int after = computer.deviceCount();
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "device count did not return to baseline after removing redstone interface: alone="
                            + base[0] + ", after=" + after);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ComputerFixture placeComputerAndCable(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        return computer;
    }

    private static void placeInterface(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.place(helper, player, new ItemStack(Items.REDSTONE_INTERFACE.get()), TestSupport.DEVICE_POS);
    }

    // --------------------------------------------------------------------- //

    private RedstoneInterfaceTests() {
    }
}
