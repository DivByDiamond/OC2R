/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

//? if >=26.1 {
/*// ProjectRed has no 26.x build, so there is no bundled cable to test against; the tests exist for 1.21.1 only.
public final class ProjectRedBundledTests {
    private ProjectRedBundledTests() {}
}
*///?} else {
import li.cil.oc2.api.API;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.misc.redstone.RedstoneInterfaceBlockEntity;
import li.cil.oc2.common.util.block.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Drives a real ProjectRed bundled cable between two redstone interfaces. Whatever one interface
 * writes on its cable-facing side must arrive at the other interface, whichever way each block faces.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProjectRedBundledTests {
    private static final int COLOR = 5;
    private static final BlockPos WRITER = new BlockPos(3, TestSupport.WORK_Y, 3);
    private static final BlockPos WIRE = WRITER.east();
    private static final BlockPos READER = WIRE.east();

    private ProjectRedBundledTests() {}

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 120)
    public static void bundledCableCarriesSignalBetweenInterfacesFacingNorth(final GameTestHelper helper) {
        run(helper, Direction.NORTH, Direction.NORTH);
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 120)
    public static void bundledCableCarriesSignalBetweenInterfacesFacingEastAndSouth(final GameTestHelper helper) {
        run(helper, Direction.EAST, Direction.SOUTH);
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 120)
    public static void bundledCableCarriesSignalBetweenInterfacesFacingSouthAndWest(final GameTestHelper helper) {
        run(helper, Direction.SOUTH, Direction.WEST);
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 120)
    public static void bundledCableCarriesSignalBetweenInterfacesFacingWestAndEast(final GameTestHelper helper) {
        run(helper, Direction.WEST, Direction.EAST);
    }

    private static void run(final GameTestHelper helper, final Direction writerFacing, final Direction readerFacing) {
        final Item wireItem = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("projectred_transmission", "neutral_bundled_wire"));
        if (wireItem == net.minecraft.world.item.Items.AIR) {
            throw TestSupport.failure(helper, "ProjectRed bundled wire item is not registered");
        }
        for (final BlockPos pos : new BlockPos[]{WRITER, WIRE, READER}) {
            TestSupport.placeFloor(helper, pos);
        }
        place(helper, WRITER, writerFacing);
        place(helper, READER, readerFacing);
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.useOn(helper, player, new ItemStack(wireItem), WIRE.below(), Direction.UP);

        final RedstoneInterfaceBlockEntity writer = interfaceAt(helper, WRITER);
        final RedstoneInterfaceBlockEntity reader = interfaceAt(helper, READER);
        final Side writerSide = sideFacing(helper, WRITER, Direction.EAST);
        final Side readerSide = sideFacing(helper, READER, Direction.WEST);

        helper.startSequence()
                .thenExecuteAfter(5, () -> writer.setBundledOutput(writerSide, 255, COLOR))
                .thenExecuteAfter(10, () -> {
                    final byte[] received = reader.getBundledInput(readerSide);
                    if (received == null || received.length <= COLOR || received[COLOR] == 0) {
                        throw TestSupport.failure(helper, "writer facing " + writerFacing + " wrote colour " + COLOR
                                + " but the reader facing " + readerFacing + " saw "
                                + java.util.Arrays.toString(received));
                    }
                    for (int color = 0; color < received.length; color++) {
                        if (color != COLOR && received[color] != 0) {
                            throw TestSupport.failure(helper, "colour " + color + " leaked onto the cable");
                        }
                    }
                })
                .thenExecuteAfter(2, () -> writer.setBundledOutput(writerSide, 0, COLOR))
                .thenExecuteAfter(10, () -> {
                    final byte[] received = reader.getBundledInput(readerSide);
                    if (received != null && received.length > COLOR && received[COLOR] != 0) {
                        throw TestSupport.failure(helper, "signal stayed on the cable after it was cleared");
                    }
                })
                .thenSucceed();
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final Direction facing) {
        final BlockState state = Blocks.REDSTONE_INTERFACE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
        helper.setBlock(pos, state);
    }

    private static RedstoneInterfaceBlockEntity interfaceAt(final GameTestHelper helper, final BlockPos pos) {
        return TestSupport.blockEntity(helper, pos, RedstoneInterfaceBlockEntity.class);
    }

    /** The block-relative side of the interface at {@code pos} that points to the world direction. */
    private static Side sideFacing(final GameTestHelper helper, final BlockPos pos, final Direction world) {
        final BlockState state = helper.getBlockState(pos);
        for (final Side side : new Side[]{Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST}) {
            if (HorizontalBlockUtils.toGlobal(state, side) == world) {
                return side;
            }
        }
        throw TestSupport.failure(helper, "no side of " + pos + " points " + world);
    }
}
//?}
