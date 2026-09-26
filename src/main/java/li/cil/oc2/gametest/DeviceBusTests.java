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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeviceBusTests {
    private static final Logger LOGGER = LogManager.getLogger();

    // Three sequential 60-tick waits (180 ticks total) exceed @GameTest's default
    // timeoutTicks=100, so the sequence was always going to time out before it could
    // report the actual assertion result.
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 250)
    public static void busTracksNeighborLifecycle(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> {
                base[0] = computer.deviceCount();
                LOGGER.info("[busTracks] base={} {}", base[0], computer.describe());
            })
            .thenExecute(() -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = computer.deviceCount();
                LOGGER.info("[busTracks] withNeighbor={} base={} {}", withNeighbor, base[0], computer.describe());
                if (withNeighbor <= base[0]) {
                    final var cableBe = (li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity)
                        helper.getBlockEntity(TestSupport.CABLE_POS);
                    throw new GameTestAssertException(
                        "attaching a redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ") " + computer.describe()
                            + "; cableDevices=" + (cableBe == null ? "null" : cableBe.busElement.getLocalDevices())
                            + "; connEast=" + (cableBe == null ? "null" : li.cil.oc2.common.block.cable.BusCableStateProperties.getConnectionType(cableBe.getBlockState(), net.minecraft.core.Direction.EAST)));
                }
            })
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenExecuteAfter(60, () -> {
                final int after = computer.deviceCount();
                LOGGER.info("[busTracks] after={} base={} {}", after, base[0], computer.describe());
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "device count did not return to baseline after removing the neighbour: alone="
                            + base[0] + ", after=" + after + " " + computer.describe());
                }
            })
            .thenSucceed();
    }

    // Two computers on one cable used to freeze both in MULTIPLE_CONTROLLERS; they now share the wiring
    // and each keeps its own devices (docs/CABLE-SYSTEM.md, "Device ownership").
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 250)
    public static void twoComputersShareOneBus(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture first = ComputerFixture.place(helper, player, COMPUTER_POS);
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), CABLE_POS);
        connectInterface(helper, CABLE_POS, Direction.WEST);
        final ComputerFixture second = ComputerFixture.place(helper, player, DEVICE_POS);
        connectInterface(helper, CABLE_POS, Direction.EAST);

        helper.startSequence()
            .thenExecuteAfter(80, () -> {
                final var firstState = first.virtualMachine().getBusState();
                final var secondState = second.virtualMachine().getBusState();
                if (firstState != li.cil.oc2.common.bus.controller.BusState.READY
                    || secondState != li.cil.oc2.common.bus.controller.BusState.READY) {
                    throw new GameTestAssertException(
                        "both computers must reach READY on a shared bus: first=" + firstState
                            + ", second=" + secondState);
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
