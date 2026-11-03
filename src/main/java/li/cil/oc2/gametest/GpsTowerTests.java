package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.gps.GpsTowerBlock;
import li.cil.oc2.common.block.gps.GpsTowerStructure;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GpsTowerTests {
    private static final BlockPos BASE = new BlockPos(2, 2, 2);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void oneItemPlacesFiveParts(final GameTestHelper helper) {
        final ItemStack stack = place(helper, BASE);
        TestSupport.assertTrue(helper, "one item consumed", stack.isEmpty());
        final Direction facing = helper.getBlockState(BASE).getValue(GpsTowerBlock.FACING);
        for (int i = 0; i < GpsTowerStructure.HEIGHT; i++) {
            final var state = helper.getBlockState(BASE.above(i));
            TestSupport.assertTrue(helper, "part " + i, state.is(Blocks.GPS_TOWER.get())
                    && state.getValue(GpsTowerBlock.PART) == i && state.getValue(GpsTowerBlock.FACING) == facing);
        }
        helper.runAfterDelay(3, () -> {
            TestSupport.assertTrue(helper, "stable after scheduled checks",
                    GpsTowerStructure.isComplete(helper.getLevel(), helper.absolutePos(BASE)));
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void everyObstructedUpperSlotRejectsPlacement(final GameTestHelper helper) {
        for (int slot = 1; slot < 5; slot++) {
            final BlockPos base = BASE.offset(slot * 5, 0, 0);
            TestSupport.placeFloor(helper, base);
            helper.setBlock(base.above(slot), net.minecraft.world.level.block.Blocks.STONE);
            final ItemStack stack = new ItemStack(Items.GPS_TOWER.get());
            TestSupport.useOn(helper, TestSupport.fakePlayer(helper), stack, base, Direction.UP);
            TestSupport.assertTrue(helper, "item retained for slot " + slot, stack.getCount() == 1);
            for (int i = 0; i < 5; i++) {
                TestSupport.assertTrue(helper, "no partial tower", !helper.getBlockState(base.above(i)).is(Blocks.GPS_TOWER.get()));
            }
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void destroyingAnyPartDropsOneWholeTower(final GameTestHelper helper) {
        for (int part = 0; part < 5; part++) {
            final BlockPos base = BASE.offset(part * 6, 0, 0);
            place(helper, base);
            helper.getLevel().destroyBlock(helper.absolutePos(base.above(part)), true);
            assertGone(helper, base);
            TestSupport.assertTrue(helper, "exactly one drop for part " + part, drops(helper, base) == 1);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void replacingMiddlePartPreservesReplacement(final GameTestHelper helper) {
        place(helper, BASE);
        helper.setBlock(BASE.above(2), net.minecraft.world.level.block.Blocks.STONE);
        assertGone(helper, BASE);
        TestSupport.assertTrue(helper, "replacement preserved",
                helper.getBlockState(BASE.above(2)).is(net.minecraft.world.level.block.Blocks.STONE));
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void creativeBreakDoesNotDropItem(final GameTestHelper helper) {
        place(helper, BASE);
        final BlockPos pos = helper.absolutePos(BASE.above(3));
        final var level = helper.getLevel();
        final var state = level.getBlockState(pos);
        final Player player = helper.makeMockPlayer(GameType.CREATIVE);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        assertGone(helper, BASE);
        TestSupport.assertTrue(helper, "creative has no loot", drops(helper, BASE) == 0);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void removingFoundationCollapsesTowerOnce(final GameTestHelper helper) {
        place(helper, BASE);
        helper.getLevel().destroyBlock(helper.absolutePos(BASE.below()), false);
        helper.runAfterDelay(3, () -> {
            assertGone(helper, BASE);
            TestSupport.assertTrue(helper, "one foundation-collapse drop", drops(helper, BASE) == 1);
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void unsupportedPlacementDoesNotConsumeItem(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.GPS_TOWER.get());
        TestSupport.useOn(helper, TestSupport.fakePlayer(helper), stack, BASE, Direction.UP);
        TestSupport.assertTrue(helper, "item retained", stack.getCount() == 1);
        assertGone(helper, BASE);
        helper.succeed();
    }

    private static ItemStack place(final GameTestHelper helper, final BlockPos base) {
        TestSupport.placeFloor(helper, base);
        final ItemStack stack = new ItemStack(Items.GPS_TOWER.get());
        TestSupport.place(helper, TestSupport.fakePlayer(helper), stack, base);
        return stack;
    }

    private static void assertGone(final GameTestHelper helper, final BlockPos base) {
        for (int i = 0; i < 5; i++) {
            TestSupport.assertTrue(helper, "part removed " + i,
                    !helper.getBlockState(base.above(i)).is(Blocks.GPS_TOWER.get()));
        }
    }

    private static int drops(final GameTestHelper helper, final BlockPos base) {
        final AABB bounds = new AABB(helper.absolutePos(base)).inflate(1).expandTowards(0, 5, 0);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(Items.GPS_TOWER.get()))
                .mapToInt(ItemStack::getCount).sum();
    }

    private GpsTowerTests() {}
}
