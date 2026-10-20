/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
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
                    final var cableBe = TestSupport.blockEntity(helper, TestSupport.CABLE_POS, BusCableBlockEntity.class);
                    throw failure(helper,
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
                    throw failure(helper,
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
                    throw failure(helper,
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

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void installedInterfacesSurviveCableNeighborChanges(final GameTestHelper helper) {
        final var world = helper.getLevel();
        final var cable = li.cil.oc2.common.block.common.Blocks.BUS_CABLE.get();
        for (final Direction side : Direction.values()) {
            final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2 + (side.get3DDataValue() % 3) * 2, 3, 2 + (side.get3DDataValue() / 3) * 3));
            final var neighbor = pos.relative(side);
            final var property = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side);
            world.setBlockAndUpdate(pos, cable.defaultBlockState().setValue(property,
                    li.cil.oc2.common.block.types.ConnectionType.INTERFACE));
            world.setBlockAndUpdate(neighbor, cable.defaultBlockState());
            assertTrue(helper, "Cable fixture missing on " + side, world.getBlockState(pos).is(cable));
            assertTrue(helper, "Neighbor cable fixture missing on " + side, world.getBlockState(neighbor).is(cable));
            assertTrue(helper, "Installed interface replaced by adjacent cable on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.INTERFACE);
            assertEquals(helper, "Interface must remain an owned/droppable part on " + side, 2,
                    li.cil.oc2.common.block.cable.BusCableStateProperties.getPartCount(world.getBlockState(pos)));
            world.setBlockAndUpdate(neighbor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            assertTrue(helper, "Removing neighbor deleted installed interface on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.INTERFACE);

            // Ordinary inferred links still appear and disappear with the neighboring cable.
            world.setBlockAndUpdate(pos, cable.defaultBlockState());
            world.setBlockAndUpdate(neighbor, cable.defaultBlockState());
            assertTrue(helper, "Cable fixture missing on " + side, world.getBlockState(pos).is(cable));
            assertTrue(helper, "Neighbor cable fixture missing on " + side, world.getBlockState(neighbor).is(cable));
            assertTrue(helper, "Missing inferred cable connection on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.CABLE);
            world.setBlockAndUpdate(neighbor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            assertTrue(helper, "Phantom cable connection after removal on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void interfaceWithoutCableDoesNotCreateHalfConnections(final GameTestHelper helper) {
        final var world = helper.getLevel();
        final var cable = li.cil.oc2.common.block.common.Blocks.BUS_CABLE.get();
        for (final Direction side : Direction.values()) {
            final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2 + (side.get3DDataValue() % 3) * 2, 3, 2 + (side.get3DDataValue() / 3) * 3));
            final var neighbor = pos.relative(side);
            final var property = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side);
            final var opposite = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side.getOpposite());
            world.setBlockAndUpdate(pos, cable.defaultBlockState()
                    .setValue(li.cil.oc2.common.block.cable.BusCableStateProperties.HAS_CABLE, false)
                    .setValue(opposite, li.cil.oc2.common.block.types.ConnectionType.INTERFACE));
            world.setBlockAndUpdate(neighbor, cable.defaultBlockState());
            assertTrue(helper, "Cable fixture missing on " + side, world.getBlockState(pos).is(cable));
            assertTrue(helper, "Neighbor cable fixture missing on " + side, world.getBlockState(neighbor).is(cable));
            assertTrue(helper, "Interface-only block created a phantom half-cable on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.NONE);
            assertTrue(helper, "Cable connected to an absent cable core on " + side,
                    world.getBlockState(neighbor).getValue(opposite)
                            == li.cil.oc2.common.block.types.ConnectionType.NONE);
            assertTrue(helper, "Unable to add cable to interface-only block on " + side,
                    li.cil.oc2.common.block.cable.BusCableStateProperties.addCable(world, pos, world.getBlockState(pos)));
            assertTrue(helper, "Adding cable did not connect both ends on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.CABLE
                    && world.getBlockState(neighbor).getValue(opposite)
                            == li.cil.oc2.common.block.types.ConnectionType.CABLE);
            world.setBlockAndUpdate(pos, world.getBlockState(pos)
                    .setValue(li.cil.oc2.common.block.cable.BusCableStateProperties.HAS_CABLE, false));
            assertTrue(helper, "Removing cable left a phantom half-cable on " + side,
                    world.getBlockState(pos).getValue(property)
                            == li.cil.oc2.common.block.types.ConnectionType.NONE
                    && world.getBlockState(neighbor).getValue(opposite)
                            == li.cil.oc2.common.block.types.ConnectionType.NONE);
            assertEquals(helper, "Removing cable lost the interface part on " + side, 1,
                    li.cil.oc2.common.block.cable.BusCableStateProperties.getPartCount(world.getBlockState(pos)));
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void loadingInterfaceOnlyBlockRepairsLegacyHalfConnections(final GameTestHelper helper) {
        final var world = helper.getLevel();
        final var cable = li.cil.oc2.common.block.common.Blocks.BUS_CABLE.get();
        for (final Direction side : Direction.values()) {
            final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2 + (side.get3DDataValue() % 3) * 2, 3, 2 + (side.get3DDataValue() / 3) * 3));
            final var property = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side);
            final var opposite = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side.getOpposite());
            // Reproduce a saved pre-fix state, without a neighbor update that could repair it.
            world.setBlock(pos, cable.defaultBlockState()
                    .setValue(li.cil.oc2.common.block.cable.BusCableStateProperties.HAS_CABLE, false)
                    .setValue(opposite, li.cil.oc2.common.block.types.ConnectionType.INTERFACE)
                    .setValue(property, li.cil.oc2.common.block.types.ConnectionType.CABLE),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                            | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
        }
        helper.startSequence().thenExecuteAfter(2, () -> {
            for (final Direction side : Direction.values()) {
                final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2 + (side.get3DDataValue() % 3) * 2, 3, 2 + (side.get3DDataValue() / 3) * 3));
                final var state = world.getBlockState(pos);
                assertTrue(helper, "Cable fixture missing after load on " + side, state.is(cable));
                final var property = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side);
                final var opposite = li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side.getOpposite());
                assertTrue(helper, "Load retained legacy half-cable on " + side,
                        state.getValue(property) == li.cil.oc2.common.block.types.ConnectionType.NONE);
                assertTrue(helper, "Load deleted the installed interface on " + side,
                        state.getValue(opposite) == li.cil.oc2.common.block.types.ConnectionType.INTERFACE);
                assertEquals(helper, "Load changed owned parts on " + side, 1,
                        li.cil.oc2.common.block.cable.BusCableStateProperties.getPartCount(state));
            }
        }).thenSucceed();
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

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 200)
    public static void monitorMergeReplacesCachedDevices(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.MONITOR.get()), DEVICE_POS);
        connectInterface(helper, CABLE_POS, Direction.EAST);
        final var previous = blockEntity(helper, DEVICE_POS, MonitorBlockEntity.class);

        helper.startSequence()
            .thenExecuteAfter(40, () -> assertTrue(helper, "single monitor must be discovered",
                computer.devices().containsAll(previous.stateManager.deviceGroup.getDevices())))
            .thenExecute(() -> place(helper, player, new ItemStack(Items.MONITOR.get()), DEVICE_POS.above()))
            .thenExecuteAfter(40, () -> {
                final var origin = blockEntity(helper, DEVICE_POS.above(), MonitorBlockEntity.class);
                assertTrue(helper, "upper monitor must become the origin", origin.isOrigin() && !previous.isOrigin());
                assertTrue(helper, "bus must discover the new origin without touching its cable",
                    computer.devices().containsAll(origin.stateManager.deviceGroup.getDevices()));
                assertTrue(helper, "bus must discard the displaced monitor devices",
                    previous.stateManager.deviceGroup.getDevices().stream().noneMatch(computer.devices()::contains));
            })
            .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void firstBusScanDiscoversSavedInterfaces(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.MONITOR.get()), DEVICE_POS);
        final var monitor = blockEntity(helper, DEVICE_POS, MonitorBlockEntity.class);
        final var state = li.cil.oc2.common.block.common.Blocks.BUS_CABLE.get().defaultBlockState()
            .setValue(li.cil.oc2.common.block.cable.BusCableStateProperties.FACING_TO_CONNECTION_MAP
                .get(Direction.EAST), li.cil.oc2.common.block.types.ConnectionType.INTERFACE);
        // Chunk loading constructs and attaches the cable before its deferred onLoad
        // discovery runs. A controller can walk the saved interface in that interval.
        final var cable = new BusCableBlockEntity(helper.absolutePos(CABLE_POS), state);
        cable.setLevel(helper.getLevel());
        assertTrue(helper, "loaded neighbours must be available", cable.busElement.getNeighbors().isPresent());
        assertTrue(helper, "first bus walk must publish the saved interface devices before VM resume",
            cable.busElement.getLocalDevices().containsAll(monitor.stateManager.deviceGroup.getDevices()));
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTests() {
    }
}
