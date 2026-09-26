/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.item.Items;
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

    // --------------------------------------------------------------------- //

    private NetworkConnectorTests() {
    }
}
