/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeviceBusTests {
    // Three sequential 60-tick waits (180 ticks total) exceed @GameTest's default
    // timeoutTicks=100, so the sequence was always going to time out before it could
    // report the actual assertion result.
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 250)
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

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void computerStartsWithoutBootError(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
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
        // A cable only auto-connects to an adjacent cable (BusCableStateProperties.canHaveCableTo
        // requires the neighbor to literally be a BUS_CABLE block); linking to a computer needs
        // the same explicit wiring a device does.
        connectInterface(helper, CABLE_POS, Direction.WEST);
        return computer;
    }

    private static void placeDevice(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);
        connectInterface(helper, CABLE_POS, Direction.EAST);
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTests() {
    }
}
