/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.common.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DeviceBusTests {
    @GameTest(template = TestSupport.TEMPLATE)
    public static void busTracksNeighborLifecycle(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> base[0] = computer.deviceCount())
            .thenExecute(() -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = computer.deviceCount();
                if (withNeighbor <= base[0]) {
                    throw new GameTestAssertException(
                        "attaching a redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ")");
                }
            })
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenExecuteAfter(60, () -> {
                final int after = computer.deviceCount();
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "device count did not return to baseline after removing the neighbour: alone="
                            + base[0] + ", after=" + after);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void computerStartsWithoutBootError(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        computer.start();

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer.assertNoBootError())
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ComputerFixture placeComputerAndCable(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), CABLE_POS);
        return computer;
    }

    private static void placeDevice(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTests() {
    }
}
