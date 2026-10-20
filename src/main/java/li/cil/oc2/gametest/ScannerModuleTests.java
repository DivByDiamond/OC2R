package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.module.ScannerModuleDevice;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import li.cil.oc2.common.item.Items;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScannerModuleTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void scannerInspectsAndScans(final GameTestHelper helper) {
        //? if >=26.1 {
        /*final Robot robot = Entities.ROBOT.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else {
        final Robot robot = Entities.ROBOT.get().create(helper.getLevel());
        //?}
        if (robot == null) {
            helper.fail("EntityType.create returned null");
            return;
        }
        final Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        //? if >=26.1 {
        /*robot.snapTo(pos.x, pos.y, pos.z, 0, 0);
        *///?} else {
        robot.moveTo(pos.x, pos.y, pos.z, 0, 0);
        //?}
        RobotActions.initializeData(robot);

        // Yaw 0 faces south, so FRONT is the block at relative (1, 2, 2).
        helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE);
        // The template area is not guaranteed empty (the framework may put containment blocks
        // around), so pin the robot's own space to dirt instead of assuming air.
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.DIRT);

        final ItemStack stack = new ItemStack(Items.SCANNER_MODULE.get());
        final ScannerModuleDevice scanner = new ScannerModuleDevice(stack, robot);

        final var inspection = scanner.inspect(RobotOperationSide.FRONT);
        if (!"minecraft:stone".equals(inspection.block())) {
            helper.fail("inspect saw block " + inspection.block() + ", expected minecraft:stone");
            return;
        }
        if (inspection.fluid() != null) {
            helper.fail("inspect saw fluid " + inspection.fluid() + ", expected none");
            return;
        }
        if (inspection.entities() == null) {
            helper.fail("inspect returned no entity list");
            return;
        }

        final var result = scanner.scan();
        if (result.hardness().length != 7 * 7 * 7) {
            helper.fail("scan returned " + result.hardness().length + " bytes, expected 343");
            return;
        }
        // Stone at offset (0, 0, 1): index (0+3)*49 + (1+3)*7 + (0+3), must be a regular hardness.
        final int stoneByte = result.hardness()[3 * 49 + 4 * 7 + 3] & 0xFF;
        if (stoneByte < 1 || stoneByte > 253) {
            helper.fail("scan encoded stone as " + stoneByte + ", expected 1-253");
            return;
        }
        // Robot's own space holds the dirt pinned above: hardness 0.5 -> 5.
        final int homeByte = result.hardness()[3 * 49 + 3 * 7 + 3] & 0xFF;
        if (homeByte != 5) {
            final var level = helper.getLevel();
            final var bodyPos = robot.blockPosition();
            helper.fail("scan encoded the robot space as "
                    + homeByte
                    + ", expected 5 (dirt); blockPosition="
                    + bodyPos
                    + " state="
                    + level.getBlockState(bodyPos)
                    + " box="
                    + robot.getBoundingBox());
            return;
        }

        try {
            scanner.scan();
            helper.fail("second scan during cooldown succeeded");
            return;
        } catch (final IllegalStateException expected) {
            TestSupport.assertTrue(helper, "unexpected scanner cooldown error",
                    "scanner is recharging".equals(expected.getMessage()));
        }

        final byte[] layer = scanner.getScanLayer(3);
        if (layer.length != 49) {
            helper.fail("scan layer has " + layer.length + " bytes, expected 49");
            return;
        }
        try {
            scanner.getScanLayer(7);
            helper.fail("out-of-range scan layer succeeded");
            return;
        } catch (final IllegalArgumentException expected) {
            TestSupport.assertTrue(helper, "unexpected scan-layer error",
                    "layer out of range: 7 (expected 0 to 6)".equals(expected.getMessage()));
        }

        // Cooldown and last scan survive an item round trip.
        final CompoundTag tag = new CompoundTag();
        scanner.exportToItemStack(tag);
        final ScannerModuleDevice restored = new ScannerModuleDevice(stack, robot);
        restored.importFromItemStack(tag);
        final byte[] restoredLayer = restored.getScanLayer(3);
        for (int i = 0; i < layer.length; i++) {
            if (layer[i] != restoredLayer[i]) {
                helper.fail("scan layer differs after item round trip at index " + i);
                return;
            }
        }
        try {
            restored.scan();
            helper.fail("restored scanner ignores cooldown");
        } catch (final IllegalStateException expected) {
            TestSupport.assertTrue(helper, "unexpected scanner cooldown error",
                    "scanner is recharging".equals(expected.getMessage()));
        }
        helper.succeed();
    }

    private ScannerModuleTests() {
    }
}
