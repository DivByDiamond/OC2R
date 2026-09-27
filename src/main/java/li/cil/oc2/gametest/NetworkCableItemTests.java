/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Link lifecycle driven through {@code NetworkCableItem}: creating a link costs a cable in
 * survival and removing it again returns that cable, while a creative player neither pays one
 * nor receives one.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkCableItemTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void survivalLinkAndRemovalCostAndReturnOneCable(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        final NetworkConnectorBlockEntity first = placeConnector(helper, player, TestSupport.CABLE_POS);
        final NetworkConnectorBlockEntity second = placeConnector(helper, player, TestSupport.DEVICE_POS);
        final ItemStack cable = new ItemStack(Items.NETWORK_CABLE.get(), 3);

        TestSupport.useOn(helper, player, cable, TestSupport.CABLE_POS, Direction.UP);
        TestSupport.useOn(helper, player, cable, TestSupport.DEVICE_POS, Direction.UP);
        TestSupport.assertEquals(helper, "creating a link consumes one cable", 2, cable.getCount());
        TestSupport.assertTrue(helper, "the link should exist after the second click",
            first.getConnectedPositions().size() == 1 && second.getConnectedPositions().size() == 1);

        TestSupport.useOn(helper, player, cable, TestSupport.CABLE_POS, Direction.UP);
        TestSupport.useOn(helper, player, cable, TestSupport.DEVICE_POS, Direction.UP);
        TestSupport.assertTrue(helper, "removal should clear both connectors",
            first.getConnectedPositions().isEmpty() && second.getConnectedPositions().isEmpty());
        TestSupport.assertEquals(helper, "removing a link must not consume a cable", 2, cable.getCount());
        TestSupport.assertTrue(helper, "the cable paid for the link should drop back into the world",
            droppedCables(helper) > 0);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void creativeLinkAndRemovalCostAndReturnNoCable(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.CREATIVE);
        final NetworkConnectorBlockEntity first = placeConnector(helper, player, TestSupport.CABLE_POS);
        final NetworkConnectorBlockEntity second = placeConnector(helper, player, TestSupport.DEVICE_POS);
        final ItemStack cable = new ItemStack(Items.NETWORK_CABLE.get(), 3);

        TestSupport.useOn(helper, player, cable, TestSupport.CABLE_POS, Direction.UP);
        TestSupport.useOn(helper, player, cable, TestSupport.DEVICE_POS, Direction.UP);
        TestSupport.assertEquals(helper, "a creative player pays no cable", 3, cable.getCount());
        TestSupport.assertTrue(helper, "the link should exist after the second click",
            first.getConnectedPositions().size() == 1 && second.getConnectedPositions().size() == 1);

        TestSupport.useOn(helper, player, cable, TestSupport.CABLE_POS, Direction.UP);
        TestSupport.useOn(helper, player, cable, TestSupport.DEVICE_POS, Direction.UP);
        TestSupport.assertTrue(helper, "removal should clear both connectors",
            first.getConnectedPositions().isEmpty() && second.getConnectedPositions().isEmpty());
        TestSupport.assertEquals(helper, "removing a link must not consume a cable", 3, cable.getCount());
        TestSupport.assertTrue(helper, "a creative removal must not drop a cable",
            droppedCables(helper) == 0);
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static NetworkConnectorBlockEntity placeConnector(
        final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.placeFloor(helper, pos);
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), pos);
        final NetworkConnectorBlockEntity connector = helper.getBlockEntity(pos);
        TestSupport.assertNotNull(helper, connector, "network connector at " + pos);
        return connector;
    }

    // helper.getEntities(EntityType) scopes to helper.getBounds(), the structure's exact NBT
    // dimensions; the "empty" template is only marginally larger than CABLE_POS/DEVICE_POS, so an
    // entity spawned at their midpoint (with spawnAsEntity's random offset) can land just outside
    // it. Searching a radius around the pair instead of relying on the template's bounds avoids
    // that edge case.
    private static long droppedCables(final GameTestHelper helper) {
        final BlockPos center = TestSupport.CABLE_POS.offset(
            (TestSupport.DEVICE_POS.getX() - TestSupport.CABLE_POS.getX()) / 2,
            (TestSupport.DEVICE_POS.getY() - TestSupport.CABLE_POS.getY()) / 2,
            (TestSupport.DEVICE_POS.getZ() - TestSupport.CABLE_POS.getZ()) / 2);
        return helper.getEntities(EntityType.ITEM, center, 3.0).stream()
            .filter(item -> item.getItem().getItem() == Items.NETWORK_CABLE.get())
            .count();
    }

    private NetworkCableItemTests() {
    }
}
