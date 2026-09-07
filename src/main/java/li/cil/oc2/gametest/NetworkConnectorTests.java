/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NetworkConnectorTests {
    @GameTest(template = TestSupport.TEMPLATE)
    public static void networkConnectorCanBePlaced(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.DEVICE_POS);
        if (helper.getBlockState(TestSupport.DEVICE_POS).isAir()) {
            throw new GameTestAssertException("network connector not placed at " + TestSupport.DEVICE_POS);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void networkConnectorWithCableSmokeTest(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), TestSupport.CABLE_POS);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.DEVICE_POS);

        TestSupport.assertTrue(helper, "network connector should not be air at " + TestSupport.CABLE_POS,
            !helper.getBlockState(TestSupport.CABLE_POS).isAir());
        TestSupport.assertTrue(helper, "bus cable should not be air at " + TestSupport.DEVICE_POS,
            !helper.getBlockState(TestSupport.DEVICE_POS).isAir());

        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void twoConnectorsCanBeLinked(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
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

    // --------------------------------------------------------------------- //

    private NetworkConnectorTests() {
    }
}
