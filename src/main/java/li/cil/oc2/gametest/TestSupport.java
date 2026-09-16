/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import javax.annotation.Nullable;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TestSupport {
    public static final String MOD_ID = "oc2";
    public static final String TEMPLATE = "empty";
    public static final String TEMPLATE_NAMESPACE = li.cil.oc2.api.API.MOD_ID;
    public static final int WORK_Y = 2;
    public static final int MAX_X = 39;

    public static final BlockPos POWER_POS = new BlockPos(1, WORK_Y, 2);
    public static final BlockPos COMPUTER_POS = new BlockPos(2, WORK_Y, 2);
    public static final BlockPos CABLE_POS = new BlockPos(3, WORK_Y, 2);
    public static final BlockPos DEVICE_POS = new BlockPos(4, WORK_Y, 2);

    public static GameTestAssertException failure(final GameTestHelper helper, final String message) {
        return new GameTestAssertException(message);
    }

    public static void assertTrue(final GameTestHelper helper, final String what, final boolean condition) {
        if (!condition) {
            throw failure(helper, what);
        }
    }

    public static void assertEquals(final GameTestHelper helper, final String what, final long expected, final long actual) {
        if (expected != actual) {
            throw failure(helper, what + ": expected " + expected + ", got " + actual);
        }
    }

    public static Player fakePlayer(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        return player;
    }

    /**
     * Sets a solid block below {@code pos}. The shared {@code empty} test structure is entirely
     * air, so face-attached blocks (e.g. {@code NetworkConnectorBlock}, a
     * {@code FaceAttachedHorizontalDirectionalBlock}) have nothing to attach to and silently
     * fail to place unless a fixture first gives them a floor.
     */
    public static void placeFloor(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
    }

    public static void place(final GameTestHelper helper, final Player player, final ItemStack stack, final BlockPos pos) {
        useOn(helper, player, stack, pos, Direction.UP);
        if (helper.getBlockState(pos).isAir()) {
            throw new GameTestAssertException("nothing placed at " + pos + " from " + stack);
        }
    }

    public static void useOn(final GameTestHelper helper, final Player player, final ItemStack stack, final BlockPos pos, final Direction face) {
        final BlockPos absolute = helper.absolutePos(pos);
        final Vec3 location = Vec3.atCenterOf(absolute)
            .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);

        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(location, face, absolute, false)));
    }

    public static void placePower(final GameTestHelper helper, final Player player) {
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), POWER_POS);
    }

    /**
     * Wires a bus cable's given side up as a device interface, the way a player would by
     * wrenching that side of the cable. Adjacency alone does not connect a device to a cable --
     * {@code BusCableBusElement.canDetectDevicesTowards} only scans a side once its connection
     * type is {@code INTERFACE}, which only {@link
     * li.cil.oc2.common.block.cable.BusCableStateProperties#addInterface} sets.
     */
    public static void connectInterface(
            final GameTestHelper helper, final BlockPos cablePos, final Direction side) {
        final BlockPos absoluteCablePos = helper.absolutePos(cablePos);
        final boolean added = li.cil.oc2.common.block.cable.BusCableStateProperties.addInterface(
            helper.getLevel(), absoluteCablePos, helper.getBlockState(cablePos), side);
        if (!added) {
            throw new GameTestAssertException("addInterface(" + cablePos + ", " + side
                + ") returned false; blockState=" + helper.getBlockState(cablePos));
        }
    }

    public static void breakBlock(final GameTestHelper helper, final BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), false, null);
    }

    public static void breakBlockAndDrop(final GameTestHelper helper, final BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true, null);
    }

    public static void assertNotNull(final GameTestHelper helper, @Nullable final Object value, final String what) {
        if (value == null) {
            throw new GameTestAssertException(what + " is null");
        }
    }

    // --------------------------------------------------------------------- //

    private TestSupport() {
    }
}
