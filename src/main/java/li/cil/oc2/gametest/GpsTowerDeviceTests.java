package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.block.gps.GpsTowerBlock;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GpsTowerDeviceTests {
    private static final BlockPos BASE = new BlockPos(2, 2, 2);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void everyPartSelectsWholeTowerButCollisionStaysLocal(final GameTestHelper helper) {
        place(helper);
        for (int i = 0; i < 5; i++) {
            final BlockPos pos = helper.absolutePos(BASE.above(i));
            final var state = helper.getLevel().getBlockState(pos);
            final var outline = state.getShape(helper.getLevel(), pos).bounds();
            TestSupport.assertTrue(helper, "whole outline from part " + i, outline.minY == -i && outline.maxY == 5 - i);
            final var collision = state.getCollisionShape(helper.getLevel(), pos, CollisionContext.empty()).bounds();
            TestSupport.assertTrue(helper, "local collision", collision.minY == 0 && collision.maxY == 1);
            TestSupport.assertTrue(helper, "only base hosts device", (helper.getLevel().getBlockEntity(pos) != null) == (i == 0));
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void rpcReportsReferenceCoordinatesAndDimension(final GameTestHelper helper) {
        final GpsTowerBlockEntity device = place(helper);
        final int[] coordinates = device.getPosition();
        final BlockPos world = helper.absolutePos(BASE);
        TestSupport.assertTrue(helper, "reference is ready", device.isAssembled());
        TestSupport.assertTrue(helper, "absolute base coordinates", coordinates[0] == world.getX()
                && coordinates[1] == world.getY() && coordinates[2] == world.getZ());
        //? if >=26.1 {
        /*final String expectedDimension = helper.getLevel().dimension().identifier().toString();
        *///?} else {
        final String expectedDimension = helper.getLevel().dimension().location().toString();
        //?}
        TestSupport.assertTrue(helper, "dimension", device.getDimension().equals(expectedDimension));
        TestSupport.assertTrue(helper, "RPC device type", device.getDeviceTypeNames().contains("gps_tower"));
        final ObjectDevice rpc = new ObjectDevice(device);
        TestSupport.assertTrue(helper, "RPC callbacks registered", rpc.getMethodGroups().size() >= 3);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void damagedTowerRejectsStaleCoordinateReads(final GameTestHelper helper) {
        final GpsTowerBlockEntity device = place(helper);
        final var state = helper.getBlockState(BASE);
        TestSupport.assertTrue(helper, "wrench cannot rotate a single part",
                state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
                        .getValue(GpsTowerBlock.FACING) == state.getValue(GpsTowerBlock.FACING));
        helper.getLevel().destroyBlock(helper.absolutePos(BASE.above(1)), false);
        TestSupport.assertTrue(helper, "stale reference not assembled", !device.isAssembled());
        try {
            device.getPosition();
            throw TestSupport.failure(helper, "damaged tower returned coordinates");
        } catch (final IllegalStateException expected) {
            TestSupport.assertTrue(helper, "incomplete reference error",
                    "GPS tower is incomplete".equals(expected.getMessage()));
        }
        helper.succeed();
    }

    private static GpsTowerBlockEntity place(final GameTestHelper helper) {
        TestSupport.placeFloor(helper, BASE);
        TestSupport.place(helper, TestSupport.fakePlayer(helper), new ItemStack(Items.GPS_TOWER.get()), BASE);
        return TestSupport.blockEntity(helper, BASE, GpsTowerBlockEntity.class);
    }

    private GpsTowerDeviceTests() {}
}
