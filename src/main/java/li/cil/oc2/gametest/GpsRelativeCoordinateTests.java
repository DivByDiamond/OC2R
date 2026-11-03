package li.cil.oc2.gametest;

import java.util.Arrays;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
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
public final class GpsRelativeCoordinateTests {
    private static final BlockPos BASE = new BlockPos(2, TestSupport.WORK_Y, 2);
    private static final BlockPos OTHER = BASE.offset(4, 1, 3);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void explicitlyPairedTowersShareOneReference(final GameTestHelper helper) {
        withRelativeMode(helper, () -> {
            final GpsTowerBlockEntity reference = place(helper, BASE);
            reference.setAsReference();
            final GpsTowerBlockEntity other = place(helper, OTHER);
            expectFailure(helper, other::getPosition, "GPS reference is not configured");
            other.joinNetwork(reference.getNetworkId());
            TestSupport.assertTrue(helper, "reference is zero", Arrays.equals(reference.getPosition(), new int[] {0, 0, 0}));
            TestSupport.assertTrue(helper, "paired offset", Arrays.equals(other.getPosition(), new int[] {4, 1, 3}));
            expectFailure(helper, other::setAsReference, "GPS network already has a reference");
            reference.clearReference();
            other.setAsReference();
            TestSupport.assertTrue(helper, "explicit reselection", Arrays.equals(reference.getPosition(), new int[] {-4, -1, -3}));
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void destroyedReferenceNeverFallsBackToWorldCoordinates(final GameTestHelper helper) {
        withRelativeMode(helper, () -> {
            final GpsTowerBlockEntity tower = place(helper, BASE);
            final GpsTowerBlockEntity reference = place(helper, OTHER);
            reference.setAsReference();
            tower.joinNetwork(reference.getNetworkId());
            helper.getLevel().destroyBlock(helper.absolutePos(OTHER.above()), false);
            expectFailure(helper, tower::getPosition, "GPS reference is not configured");
            TestSupport.assertTrue(helper, "no automatic election", !tower.isReference() && !tower.hasSignal());
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void networkIdAloneDoesNotProvideOutOfRangeCoordinates(final GameTestHelper helper) {
        withRelativeMode(helper, () -> {
            Config.gpsTowerRange = 3;
            final GpsTowerBlockEntity reference = place(helper, BASE);
            reference.setAsReference();
            final GpsTowerBlockEntity other = place(helper, OTHER);
            other.joinNetwork(reference.getNetworkId());
            expectFailure(helper, other::getPosition, "GPS signal is unavailable");
            TestSupport.assertTrue(helper, "outside radio range", !other.hasSignal());
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void disablingRelativeModeRestoresWorldCoordinates(final GameTestHelper helper) {
        withRelativeMode(helper, () -> {
            final GpsTowerBlockEntity tower = place(helper, BASE);
            tower.setAsReference();
            Config.relativeCoordinates = false;
            final BlockPos absolute = helper.absolutePos(BASE);
            TestSupport.assertTrue(helper, "absolute mode", Arrays.equals(tower.getPosition(),
                    new int[] {absolute.getX(), absolute.getY(), absolute.getZ()}));
            expectFailure(helper, tower::setAsReference, "Relative coordinates are disabled");
            Config.relativeCoordinates = true;
            TestSupport.assertTrue(helper, "selection survives mode switch", tower.isReference());
        });
    }

    private static void withRelativeMode(final GameTestHelper helper, final Runnable action) {
        final boolean previousMode = Config.relativeCoordinates;
        final int previousRange = Config.gpsTowerRange;
        try {
            Config.relativeCoordinates = true;
            Config.gpsTowerRange = 20;
            action.run();
            helper.succeed();
        } finally {
            Config.relativeCoordinates = previousMode;
            Config.gpsTowerRange = previousRange;
        }
    }

    private static void expectFailure(final GameTestHelper helper, final Runnable action, final String message) {
        try {
            action.run();
        } catch (final IllegalStateException expected) {
            TestSupport.assertTrue(helper, "coordinate-free failure", message.equals(expected.getMessage()));
            return;
        }
        throw TestSupport.failure(helper, "expected GPS failure");
    }

    private static GpsTowerBlockEntity place(final GameTestHelper helper, final BlockPos pos) {
        TestSupport.placeFloor(helper, pos);
        TestSupport.place(helper, TestSupport.fakePlayer(helper), new ItemStack(Items.GPS_TOWER.get()), pos);
        return TestSupport.blockEntity(helper, pos, GpsTowerBlockEntity.class);
    }

    private GpsRelativeCoordinateTests() {}
}
