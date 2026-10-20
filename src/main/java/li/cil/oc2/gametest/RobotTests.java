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

    private RobotTests() {
    }
}
