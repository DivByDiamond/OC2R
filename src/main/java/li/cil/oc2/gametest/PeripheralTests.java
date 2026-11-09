package li.cil.oc2.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.misc.EnvironmentSensorBlockEntity;
import li.cil.oc2.common.blockentity.network.WirelessAccessPointBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.radio.DockStations;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PeripheralTests {
    private static final BlockPos TOWER = new BlockPos(2, TestSupport.WORK_Y, 2);
    private static final BlockPos POINT = TOWER.offset(8, 2, 0);
    private static final BlockPos SENSOR = new BlockPos(3, 3, 3);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void accessPointUsesRealBottomConnectorAndBridgesBothDirections(final GameTestHelper helper) {
        final var tower = RadioBridgeTests.tower(helper, TOWER);
        final String admin = tower.createRadioNetwork().get("adminSecret");
        helper.setBlock(POINT, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        final var point = TestSupport.blockEntity(helper, POINT, WirelessAccessPointBlockEntity.class);
        final var invite = tower.issueRadioInvite(admin, point.getRadioEndpointId());
        point.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        final var player = TestSupport.serverPlayer(helper);
        TestSupport.useOn(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), POINT, Direction.DOWN);
        final var near = TestSupport.blockEntity(helper, POINT.below(), NetworkConnectorBlockEntity.class);
        final var far = RadioBridgeTests.connector(helper, POINT.south(3).below());
        NetworkConnectorBlockEntity.connect(near, far);
        final var towerNear = RadioBridgeTests.connector(helper, TOWER.south());
        final var towerEnd = RadioBridgeTests.connector(helper, TOWER.south(3));
        NetworkConnectorBlockEntity.connect(towerNear, towerEnd);
        final var pointWire = new RadioDockEthernetTests.Wire();
        final var towerWire = new RadioDockEthernetTests.Wire();
        helper.startSequence().thenIdle(4).thenExecute(() -> {
            far.adjacentInterface = pointWire;
            towerEnd.adjacentInterface = towerWire;
            TestSupport.assertTrue(helper, "underside connector survives and resolves the point", near.adjacentInterface == point);
            for (final Direction side : Direction.values()) {
                final var capability = Platform.capabilities().getBlockCapability(Capabilities.NetworkInterface.BLOCK,
                        helper.getLevel(), helper.absolutePos(POINT), null, point, side);
                TestSupport.assertTrue(helper, "Ethernet port only at bottom", (capability == point) == (side == Direction.DOWN));
            }
            pointWire.out.add(RadioDockEthernetTests.frame(0x51));
        }).thenWaitUntil(() -> TestSupport.assertTrue(helper, "bottom cable sends to tower", towerWire.got(0x51)))
                .thenExecute(() -> towerWire.out.add(RadioDockEthernetTests.frame(0x52)))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "tower sends to bottom cable", pointWire.got(0x52)))
                .thenExecute(() -> tower.revokeRadioEndpoint(admin, point.getRadioEndpointId()))
                .thenExecute(() -> pointWire.out.add(RadioDockEthernetTests.frame(0x53)))
                .thenIdle(10)
                .thenExecute(() -> TestSupport.assertTrue(helper, "revoked access point sends nothing", !towerWire.got(0x53)))
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void accessPointCoverageRequiresItsOwnEnrollment(final GameTestHelper helper) {
        final var tower = RadioBridgeTests.tower(helper, TOWER);
        final var network = tower.createRadioNetwork();
        final var id = UUID.fromString(network.get("network"));
        helper.setBlock(POINT, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        final var point = TestSupport.blockEntity(helper, POINT, WirelessAccessPointBlockEntity.class);
        final var absolute = helper.absolutePos(POINT);
        TestSupport.assertTrue(helper, "unenrolled point is absent", DockStations.near(helper.getLevel(),
                absolute.getX(), absolute.getZ(), id).isEmpty());
        final var invite = tower.issueRadioInvite(network.get("adminSecret"), point.getRadioEndpointId());
        point.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        TestSupport.assertTrue(helper, "enrolled point covers its network", !DockStations.near(helper.getLevel(),
                absolute.getX(), absolute.getZ(), id).isEmpty());
        TestSupport.assertTrue(helper, "point does not cover unrelated network", DockStations.near(helper.getLevel(),
                absolute.getX(), absolute.getZ(), UUID.randomUUID()).isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void copiedAccessPointIdentityCannotProvideCoverageWhileOriginalIsLoaded(final GameTestHelper helper) {
        final var tower = RadioBridgeTests.tower(helper, TOWER);
        final var network = tower.createRadioNetwork();
        final var networkId = UUID.fromString(network.get("network"));
        helper.setBlock(POINT, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        final var original = TestSupport.blockEntity(helper, POINT, WirelessAccessPointBlockEntity.class);
        final var invite = tower.issueRadioInvite(network.get("adminSecret"), original.getRadioEndpointId());
        original.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        final var saved = original.saveWithoutMetadata(helper.getLevel().registryAccess());
        final var copyPos = POINT.east(2);
        helper.setBlock(copyPos, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        final var copy = TestSupport.blockEntity(helper, copyPos, WirelessAccessPointBlockEntity.class);
        copy.loadAdditional(saved, helper.getLevel().registryAccess());
        TestSupport.assertTrue(helper, "loaded identity copy has no coverage", !copy.belongsTo(networkId));
        TestSupport.assertTrue(helper, "original retains bridge ownership", original.belongsTo(networkId));
        TestSupport.assertTrue(helper, "duplicate is not attached", !Boolean.TRUE.equals(copy.getRadioStatus().get("attached")));
        helper.setBlock(POINT, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        TestSupport.assertTrue(helper, "unloaded original releases identity", copy.belongsTo(networkId));
        tower.revokeRadioEndpoint(network.get("adminSecret"), copy.getRadioEndpointId());
        TestSupport.assertTrue(helper, "revocation removes coverage immediately", !copy.belongsTo(networkId));
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 100)
    public static void sensorReportsBoundedSamplesAndEnvironmentalEvents(final GameTestHelper helper) {
        helper.setBlock(SENSOR.south(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.setBlock(SENSOR, Blocks.ENVIRONMENT_SENSOR.get().defaultBlockState());
        final var sensor = TestSupport.blockEntity(helper, SENSOR, EnvironmentSensorBlockEntity.class);
        final var initial = sensor.getEnvironment();
        TestSupport.assertTrue(helper, "environment fields", initial.keySet().containsAll(List.of(
                "biome", "temperature", "skyLight", "blockLight", "raining", "thundering", "dayTime", "sampledAt")));
        TestSupport.assertTrue(helper, "temperature uses Minecraft units", sensor.getUnits().get("temperature")
                .equals("minecraft_biome_temperature"));
        TestSupport.assertTrue(helper, "repeated calls reuse sample", sensor.getEnvironment() == initial);
        final ObjectDevice rpc = new ObjectDevice(sensor);
        TestSupport.assertTrue(helper, "RPC is visible", rpc.getMethodGroups().stream()
                .anyMatch(group -> group.getName().equals("getEnvironment")));
        final List<String> events = new ArrayList<>();
        final li.cil.oc2.api.bus.device.rpc.IEventSink sink = (id, event) -> events.add(event.toString());
        sensor.subscribe(sink, UUID.randomUUID());
        helper.setBlock(SENSOR.above(), net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState());
        helper.startSequence().thenIdle(25).thenExecute(() -> {
            TestSupport.assertTrue(helper, "new sample after interval", sensor.getEnvironment() != initial);
            TestSupport.assertTrue(helper, "light level changes", ((Number) sensor.getEnvironment().get("blockLight")).intValue() > 0);
            TestSupport.assertTrue(helper, "environment change emits event", events.stream()
                    .anyMatch(event -> event.contains("environment_changed")));
            events.clear();
            sensor.unsubscribe(sink);
            helper.setBlock(SENSOR.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }).thenIdle(25).thenExecute(() -> TestSupport.assertTrue(helper, "unsubscribe stops notifications", events.isEmpty()))
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void sensorWallPlacementWorksWithSideBusAndDropsWithoutSupport(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final var computer = ComputerFixture.place(helper, player);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        helper.setBlock(TestSupport.DEVICE_POS.south(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        TestSupport.useOn(helper, player, new ItemStack(Items.ENVIRONMENT_SENSOR.get()),
                TestSupport.DEVICE_POS.south(), Direction.NORTH);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.EAST);
        helper.startSequence().thenWaitUntil(() -> TestSupport.assertTrue(helper, "sensor RPC discovered through side bus",
                computer.devices().stream().anyMatch(device -> device instanceof final li.cil.oc2.api.bus.device.rpc.RPCDevice rpc
                        && rpc.getTypeNames().contains("environment_sensor")
                        && rpc.getMethodGroups().stream().anyMatch(group -> group.getName().equals("getEnvironment")))))
                .thenExecute(() -> helper.setBlock(TestSupport.DEVICE_POS.south(),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "sensor cannot float without wall",
                        helper.getBlockState(TestSupport.DEVICE_POS).isAir()))
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void sensorWallAndSelectionShapeAgreeInAllFourDirections(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        int index = 0;
        for (final Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            final BlockPos pos = new BlockPos(5 + 5 * index++, 3, 8);
            final BlockPos wall = pos.relative(facing.getOpposite());
            helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            TestSupport.useOn(helper, player, new ItemStack(Items.ENVIRONMENT_SENSOR.get()), wall, facing);
            final var state = helper.getBlockState(pos);
            TestSupport.assertTrue(helper, "sensor attached facing " + facing,
                    state.is(Blocks.ENVIRONMENT_SENSOR.get())
                            && state.getValue(li.cil.oc2.common.block.misc.EnvironmentSensorBlock.FACING) == facing);
            final var bounds = state.getShape(helper.getLevel(), helper.absolutePos(pos)).bounds();
            final boolean touchingWall = switch (facing) {
                case NORTH -> bounds.maxZ == 1;
                case EAST -> bounds.minX == 0;
                case SOUTH -> bounds.minZ == 0;
                case WEST -> bounds.maxX == 1;
                default -> false;
            };
            TestSupport.assertTrue(helper, "selection box meets backing wall for " + facing, touchingWall);
        }
        helper.succeed();
    }

    private PeripheralTests() {}
}
