package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActions;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotTests {
    /** The first server tick must initialize the synchronized idle target to the robot's position. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void robotInitializesIdleMovementTarget(final GameTestHelper helper) {
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
        /*robot.snapTo(pos.x, pos.y, pos.z, 90, 0);
        *///?} else {
        robot.moveTo(pos.x, pos.y, pos.z, 90, 0);
        //?}
        helper.getLevel().addFreshEntity(robot);

        helper.runAtTickTime(2, () -> {
            final var expected = robot.blockPosition();
            final var actual = robot.getEntityData().get(Robot.TARGET_POSITION);
            if (!expected.equals(actual)) {
                helper.fail("idle movement target is " + actual + ", expected " + expected);
                return;
            }
            helper.succeed();
        });
    }

    /** Robots are saved through RobotSerializer, which is first loaded by the save itself. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void robotSurvivesSaveAndLoad(final GameTestHelper helper) {
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
        /*robot.snapTo(pos.x, pos.y, pos.z, 90, 0);
        *///?} else {
        robot.moveTo(pos.x, pos.y, pos.z, 90, 0);
        //?}
        RobotActions.initializeData(robot);

        //? if >=26.1 {
        /*final net.minecraft.world.level.storage.TagValueOutput output =
            net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        if (!robot.save(output)) {
            helper.fail("robot refused to save");
            return;
        }
        final CompoundTag tag = output.buildResult();
        *///?} else {
        final CompoundTag tag = new CompoundTag();
        if (!robot.save(tag)) {
            helper.fail("robot refused to save");
            return;
        }
        //?}
        //? if >=26.2 {
        /*final var loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD, false), e -> e);
        *///?} else if >=26.1 {
        /*final var loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD, e -> e);
        *///?} else {
        final var loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), e -> e);
        //?}
        if (!(loaded instanceof Robot)) {
            helper.fail("loading the saved robot gave " + loaded);
            return;
        }
        helper.succeed();
    }

    /** The localization origin tracks the body until calibrated, and survives save/load. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void robotLocalizationTracksMovement(final GameTestHelper helper) {
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
        /*robot.snapTo(pos.x, pos.y, pos.z, 90, 0);
        *///?} else {
        robot.moveTo(pos.x, pos.y, pos.z, 90, 0);
        //?}
        RobotActions.initializeData(robot);

        robot.updateOrigin();
        if (!robot.getPosition().equals(new Robot.Position(0, 0, 0))) {
            helper.fail("fresh origin is not (0,0,0): " + robot.getPosition());
            return;
        }
        final String expectedFacing =
                net.minecraft.core.Direction.fromYRot(90).getSerializedName();
        if (!robot.getFacing().equals(expectedFacing)) {
            helper.fail("facing is " + robot.getFacing() + ", expected " + expectedFacing);
            return;
        }

        final Vec3 moved = helper.absoluteVec(new Vec3(3.5, 2, 1.5));
        //? if >=26.1 {
        /*robot.snapTo(moved.x, moved.y, moved.z, 90, 0);
        *///?} else {
        robot.moveTo(moved.x, moved.y, moved.z, 90, 0);
        //?}
        if (!robot.getPosition().equals(new Robot.Position(2, 0, 0))) {
            helper.fail("moved robot is not at (2,0,0): " + robot.getPosition());
            return;
        }

        robot.calibratePosition();
        if (!robot.getPosition().equals(new Robot.Position(0, 0, 0))) {
            helper.fail("calibrated robot is not (0,0,0): " + robot.getPosition());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void stoppedRobotRetainsItsPrivateRootAcrossEntityReload(final GameTestHelper helper) {
        final Robot source = newRootSnapshotRobot(helper);
        final Robot restored = newRootSnapshotRobot(helper);
        final Vec3 position = helper.absoluteVec(new Vec3(2.5, 2, 4.5));
        //? if >=26.1 {
        /*source.snapTo(position.x, position.y, position.z, 0, 0);
        *///?} else {
        source.moveTo(position.x, position.y, position.z, 0, 0);
        //?}
        helper.getLevel().addFreshEntity(source);
        final long offset = 64 * 1024L - 7;
        final byte[] expected = "oc2r-stopped-robot-root".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            TestSupport.assertTrue(helper, "fresh robot root must not contain the private marker",
                    !java.util.Arrays.equals(expected, readRootMarker(restored.getVirtualMachine().state.builtinDevices.rootFilesystem, offset, expected.length)));
            try (var output = source.getVirtualMachine().state.builtinDevices.rootFilesystem.getOutputStream(offset)) {
                output.write(expected);
            }
            source.getVirtualMachine().start();
            source.getVirtualMachine().stop();
            TestSupport.assertTrue(helper, "stop itself must retain the source robot disk bytes",
                    java.util.Arrays.equals(expected, readRootMarker(source.getVirtualMachine().state.builtinDevices.rootFilesystem, offset, expected.length)));
            final CompoundTag saved = new CompoundTag();
            li.cil.oc2.common.entity.robot.state.RobotSerializer.save(source, saved);
            li.cil.oc2.common.entity.robot.state.RobotSerializer.load(restored, saved);
            TestSupport.assertTrue(helper, "restored robot must remain powered off without a worker",
                    restored.getVirtualMachine().getRunState() == li.cil.oc2.common.vm.VMRunState.STOPPED
                            && restored.getVirtualMachine().runner == null && !restored.getVirtualMachine().state.board.isRunning());
            TestSupport.assertTrue(helper, "stopped robot private disk bytes must survive entity save/load",
                    java.util.Arrays.equals(expected, readRootMarker(restored.getVirtualMachine().state.builtinDevices.rootFilesystem, offset, expected.length)));
            helper.succeed();
        } catch (final java.io.IOException failure) {
            throw TestSupport.failure(helper, "robot root roundtrip failed: " + failure);
        } finally {
            source.discard();
            restored.getVirtualMachine().dispose();
        }
    }

    private static Robot newRootSnapshotRobot(final GameTestHelper helper) {
        //? if >=26.1 {
        /*final Robot robot = Entities.ROBOT.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else {
        final Robot robot = Entities.ROBOT.get().create(helper.getLevel());
        //?}
        if (robot == null) throw TestSupport.failure(helper, "robot factory returned null");
        RobotActions.initializeData(robot);
        return robot;
    }

    private static byte[] readRootMarker(final li.cil.sedna.api.device.BlockDevice root,
                                         final long offset, final int length) throws java.io.IOException {
        try (var input = root.getInputStream(offset)) {
            return input.readNBytes(length);
        }
    }

    private RobotTests() {
    }
}
