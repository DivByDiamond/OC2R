package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GpsTowerPlacementTests {
    private static final BlockPos BASE = new BlockPos(2, 2, 2);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void upperEntityCollisionPreventsWholePlacement(final GameTestHelper helper) {
        TestSupport.placeFloor(helper, BASE);
        //? if >=26.2 {
        /*final var cow = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, BASE.above(2));
        *///?} else {
        final var cow = helper.spawn(EntityType.COW, BASE.above(2));
        //?}
        final ItemStack stack = new ItemStack(Items.GPS_TOWER.get());
        TestSupport.useOn(helper, TestSupport.fakePlayer(helper), stack, BASE, Direction.UP);
        TestSupport.assertTrue(helper, "item retained when an upper part hits a mob", stack.getCount() == 1);
        for (int i = 0; i < 5; i++) {
            TestSupport.assertTrue(helper, "no part placed through entity", !helper.getBlockState(BASE.above(i)).is(Blocks.GPS_TOWER.get()));
        }
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void buildCeilingRejectsWithoutPartialBlocks(final GameTestHelper helper) {
        final var level = helper.getLevel();
        //? if >=26.1 {
        /*final int top = level.getMaxY();
        *///?} else {
        final int top = level.getMaxBuildHeight();
        //?}
        final BlockPos base = helper.absolutePos(BASE).atY(top - 3);
        level.setBlockAndUpdate(base.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        final var player = TestSupport.fakePlayer(helper);
        final ItemStack stack = new ItemStack(Items.GPS_TOWER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(base), Direction.UP, base, false)));
        TestSupport.assertTrue(helper, "item retained at ceiling", stack.getCount() == 1);
        for (int i = 0; i < 3; i++) {
            TestSupport.assertTrue(helper, "no partial tower at ceiling", !level.getBlockState(base.above(i)).is(Blocks.GPS_TOWER.get()));
        }
        level.removeBlock(base.below(), false);
        helper.succeed();
    }

    private GpsTowerPlacementTests() {}
}
