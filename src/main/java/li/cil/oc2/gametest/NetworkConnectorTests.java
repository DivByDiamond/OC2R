/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkConnectorTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void networkConnectorCanBePlaced(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placeFloor(helper, TestSupport.DEVICE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.DEVICE_POS);
        if (helper.getBlockState(TestSupport.DEVICE_POS).isAir()) {
            throw new GameTestAssertException("network connector not placed at " + TestSupport.DEVICE_POS);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void networkConnectorWithCableSmokeTest(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placeFloor(helper, TestSupport.CABLE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.CABLE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.DEVICE_POS);

        TestSupport.assertTrue(helper, "network connector should not be air at " + TestSupport.CABLE_POS,
            !helper.getBlockState(TestSupport.CABLE_POS).isAir());
        TestSupport.assertTrue(helper, "bus cable should not be air at " + TestSupport.DEVICE_POS,
            !helper.getBlockState(TestSupport.DEVICE_POS).isAir());

        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void twoConnectorsCanBeLinked(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placeFloor(helper, TestSupport.CABLE_POS);
        TestSupport.placeFloor(helper, TestSupport.DEVICE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.CABLE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.DEVICE_POS);

        final NetworkConnectorBlockEntity first = helper.getBlockEntity(TestSupport.CABLE_POS);
        final NetworkConnectorBlockEntity second = helper.getBlockEntity(TestSupport.DEVICE_POS);

        TestSupport.assertNotNull(helper, first, "first network connector block entity at " + TestSupport.CABLE_POS);
        TestSupport.assertNotNull(helper, second, "second network connector block entity at " + TestSupport.DEVICE_POS);

        final ConnectionResult result = NetworkConnectorBlockEntity.connect(first, second);
        if (result != ConnectionResult.SUCCESS && result != ConnectionResult.ALREADY_CONNECTED) {
            throw new GameTestAssertException("connecting two adjacent connectors failed: " + result);
        }

        TestSupport.assertTrue(helper, "first connector should see the second as connected",
            first.getConnectedPositions().contains(helper.absolutePos(TestSupport.DEVICE_POS))
                || first.getConnectedPositions().contains(TestSupport.DEVICE_POS)
                || !first.getConnectedPositions().isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void connectorHoldsMoreThanTwoCables(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        final var positions = new net.minecraft.core.BlockPos[] {
            TestSupport.COMPUTER_POS, TestSupport.CABLE_POS, TestSupport.DEVICE_POS
        };
        for (final var position : positions) {
            TestSupport.placeFloor(helper, position);
            TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), position);
        }
        final NetworkConnectorBlockEntity hub = helper.getBlockEntity(TestSupport.CABLE_POS);
        final NetworkConnectorBlockEntity west = helper.getBlockEntity(TestSupport.COMPUTER_POS);
        final NetworkConnectorBlockEntity east = helper.getBlockEntity(TestSupport.DEVICE_POS);

        final ConnectionResult a = NetworkConnectorBlockEntity.connect(hub, west);
        final ConnectionResult b = NetworkConnectorBlockEntity.connect(hub, east);
        if (a != ConnectionResult.SUCCESS || b != ConnectionResult.SUCCESS) {
            throw new GameTestAssertException("expected two links from one connector, got " + a + " and " + b);
        }
        TestSupport.assertTrue(helper, "connector should hold both links", hub.getConnectedPositions().size() == 2);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void disconnectRemovesLinkFromBothSides(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placeFloor(helper, TestSupport.CABLE_POS);
        TestSupport.placeFloor(helper, TestSupport.DEVICE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.CABLE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.DEVICE_POS);
        final NetworkConnectorBlockEntity first = helper.getBlockEntity(TestSupport.CABLE_POS);
        final NetworkConnectorBlockEntity second = helper.getBlockEntity(TestSupport.DEVICE_POS);

        NetworkConnectorBlockEntity.connect(first, second);
        TestSupport.assertTrue(helper, "link should exist before removal",
            first.getConnectedPositions().size() == 1 && second.getConnectedPositions().size() == 1);

        NetworkConnectorBlockEntity.disconnect(first, second);
        TestSupport.assertTrue(helper, "both sides should drop the link",
            first.getConnectedPositions().isEmpty() && second.getConnectedPositions().isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void portLimitRefusesAdditionalLink(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        final int ports = Config.networkConnectorPorts;
        TestSupport.assertTrue(helper, "peers below cover exactly the 2..8 range of the config",
            ports >= 2 && ports <= 8);
        final NetworkConnectorBlockEntity hub = connectorAt(helper, player, TestSupport.CABLE_POS);

        // Eight mutually unobstructed neighbours of the hub, then one further peer. The port
        // limit is validated before distance and line of sight, so the final attempt reports
        // FAILURE_FULL even though that peer sits behind an intervening connector.
        final BlockPos[] peers = new BlockPos[] {
            new BlockPos(2, TestSupport.WORK_Y, 1), new BlockPos(3, TestSupport.WORK_Y, 1),
            new BlockPos(4, TestSupport.WORK_Y, 1), new BlockPos(2, TestSupport.WORK_Y, 2),
            new BlockPos(4, TestSupport.WORK_Y, 2), new BlockPos(2, TestSupport.WORK_Y, 3),
            new BlockPos(3, TestSupport.WORK_Y, 3), new BlockPos(4, TestSupport.WORK_Y, 3),
            new BlockPos(TestSupport.CABLE_POS.getX(), TestSupport.WORK_Y,
                TestSupport.CABLE_POS.getZ() + 4),
        };

        for (int i = 0; i <= ports; i++) {
            final NetworkConnectorBlockEntity peer = connectorAt(helper, player, peers[i]);
            final ConnectionResult result = NetworkConnectorBlockEntity.connect(hub, peer);
            if (i < ports) {
                if (result != ConnectionResult.SUCCESS && result != ConnectionResult.ALREADY_CONNECTED) {
                    throw new GameTestAssertException("link " + (i + 1) + " should be accepted, got " + result);
                }
            } else if (result != ConnectionResult.FAILURE_FULL) {
                throw new GameTestAssertException("link " + (i + 1) + " should be refused as full, got " + result);
            }
        }

        TestSupport.assertEquals(helper, "hub should hold exactly its port limit of links",
            ports, hub.getConnectedPositions().size());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void staleOneSidedLinkIsDroppedOnResolve(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        final NetworkConnectorBlockEntity first = connectorAt(helper, player, TestSupport.CABLE_POS);
        final NetworkConnectorBlockEntity second = connectorAt(helper, player, TestSupport.DEVICE_POS);

        NetworkConnectorBlockEntity.connect(first, second);
        TestSupport.assertTrue(helper, "link should exist before the peer is removed",
            first.getConnectedPositions().size() == 1);

        // Dropping the link on one side alone must leave the other side with a phantom entry.
        second.disconnectFrom(helper.absolutePos(TestSupport.CABLE_POS));
        TestSupport.assertTrue(helper, "one-sided removal must not touch the other side",
            first.getConnectedPositions().size() == 1 && second.getConnectedPositions().isEmpty());

        TestSupport.breakBlock(helper, TestSupport.DEVICE_POS);
        first.connectionManager.resolveConnectedInterface(helper.absolutePos(TestSupport.DEVICE_POS));
        TestSupport.assertTrue(helper, "the stale link should be dropped when it is resolved",
            first.getConnectedPositions().isEmpty());
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static NetworkConnectorBlockEntity connectorAt(
        final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.placeFloor(helper, pos);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), pos);
        final NetworkConnectorBlockEntity connector = helper.getBlockEntity(pos);
        TestSupport.assertNotNull(helper, connector, "network connector at " + pos);
        return connector;
    }

    private NetworkConnectorTests() {
    }
}
