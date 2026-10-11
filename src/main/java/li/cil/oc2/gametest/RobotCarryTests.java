package li.cil.oc2.gametest;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActionResult;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import li.cil.oc2.common.entity.robot.movement.MovementDirection;
import li.cil.oc2.common.entity.robot.movement.RobotMovementAction;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
import net.minecraft.world.entity.animal.pig.Pig;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
//? if <26.2 {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotCarryTests {
    private static final int SETTLE_TICKS = 10;
    private static final double TOLERANCE = 0.02;

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void itemInPathDoesNotBlock(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final Vec3 itemPos =
                RobotMovementAction.getTargetPositionInBlock(
                        robot.blockPosition().relative(robot.getDirection()));
        helper.getLevel()
                .addFreshEntity(
                        new ItemEntity(
                                helper.getLevel(),
                                itemPos.x,
                                itemPos.y,
                                itemPos.z,
                                new ItemStack(Items.DIRT),
                                0,
                                0,
                                0));

        move(
                helper,
                robot,
                MovementDirection.FORWARD,
                () -> result -> requireResult(helper, result, RobotActionResult.SUCCESS));
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void carriesAMobForward(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final Pig pig = spawnPigOnTop(helper, robot);

        move(
                helper,
                robot,
                MovementDirection.FORWARD,
                () -> {
                    final Vec3 offset = pig.position().subtract(robot.position());
                    return result -> {
                        requireResult(helper, result, RobotActionResult.SUCCESS);
                        requireNear(
                                helper,
                                pig.position().subtract(robot.position()),
                                offset,
                                "pig offset to robot");
                    };
                });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void liftsAMob(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final Pig pig = spawnPigOnTop(helper, robot);

        move(
                helper,
                robot,
                MovementDirection.UPWARD,
                () -> result -> {
                    requireResult(helper, result, RobotActionResult.SUCCESS);
                    requireNear(
                            helper,
                            pig.getBoundingBox().minY,
                            robot.getBoundingBox().maxY,
                            "pig feet vs robot top");
                });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void liftingAMobIntoACeilingFails(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final BlockPos ceiling = robot.blockPosition().above(2);
        helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        final Pig pig = spawnPigOnTop(helper, robot);

        move(
                helper,
                robot,
                MovementDirection.UPWARD,
                () -> {
                    final BlockPos start = robot.blockPosition();
                    return result -> {
                        requireResult(helper, result, RobotActionResult.FAILURE);
                        if (!robot.blockPosition().equals(start)) {
                            helper.fail("robot did not return to " + start + ", is at "
                                    + robot.blockPosition());
                        }
                        if (pig.getBoundingBox().maxY > ceiling.getY() + TOLERANCE) {
                            helper.fail("pig was pushed into the ceiling, maxY "
                                    + pig.getBoundingBox().maxY);
                        }
                    };
                });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void playerOnTopDoesNotBlockLifting(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        placePlayerOnTop(helper, robot);

        move(
                helper,
                robot,
                MovementDirection.UPWARD,
                () -> result -> requireResult(helper, result, RobotActionResult.SUCCESS));
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void liftingAPlayerIntoACeilingFails(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        helper.getLevel()
                .setBlockAndUpdate(
                        robot.blockPosition().above(2), Blocks.STONE.defaultBlockState());
        placePlayerOnTop(helper, robot);

        move(
                helper,
                robot,
                MovementDirection.UPWARD,
                () -> result -> requireResult(helper, result, RobotActionResult.FAILURE));
    }

    private interface ResultCheck {
        void check(RobotActionResult result);
    }

    private static void move(
            final GameTestHelper helper,
            final Robot robot,
            final MovementDirection direction,
            final Supplier<ResultCheck> start) {
        final AtomicReference<RobotMovementAction> action = new AtomicReference<>();
        final AtomicReference<ResultCheck> check = new AtomicReference<>();
        final AtomicReference<RobotActionResult> result =
                new AtomicReference<>(RobotActionResult.INCOMPLETE);
        helper.onEachTick(
                () -> {
                    if (action.get() != null && result.get() == RobotActionResult.INCOMPLETE) {
                        result.set(action.get().perform(robot));
                    }
                });
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(
                        () -> {
                            check.set(start.get());
                            final RobotMovementAction value =
                                    new RobotMovementAction(direction);
                            value.initialize(robot);
                            action.set(value);
                        })
                .thenWaitUntil(
                        () -> {
                            if (result.get() == RobotActionResult.INCOMPLETE) {
                                helper.fail("movement still in progress at robot " + robot.position());
                            }
                        })
                .thenExecute(() -> check.get().check(result.get()))
                .thenSucceed();
    }

    private static Robot placeRobot(final GameTestHelper helper) {
        //? if >=26.1 {
        /*final Robot robot = Entities.ROBOT.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else {
        final Robot robot = Entities.ROBOT.get().create(helper.getLevel());
        //?}
        if (robot == null) {
            helper.fail("EntityType.create returned null");
            throw new IllegalStateException("unreachable");
        }
        clearArena(helper);
        // Robots rest slightly above the block floor; spawning at the block origin would leave the
        // robot short of its own start position.
        final Vec3 pos =
                RobotMovementAction.getTargetPositionInBlock(helper.absolutePos(new BlockPos(1, 2, 1)));
        //? if >=26.1 {
        /*robot.snapTo(pos.x, pos.y, pos.z, 0, 0);
        *///?} else {
        robot.moveTo(pos.x, pos.y, pos.z, 0, 0);
        //?}
        RobotActions.initializeData(robot);
        helper.getLevel().addFreshEntity(robot);
        return robot;
    }

    // The shared template has barrier blocks at robot height; clear the space the robot moves in.
    private static void clearArena(final GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 0; z <= 4; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void placePlayerOnTop(final GameTestHelper helper, final Robot robot) {
        @SuppressWarnings("removal") final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(robot.getX(), robot.getBoundingBox().maxY, robot.getZ());
    }

    private static Pig spawnPigOnTop(final GameTestHelper helper, final Robot robot) {
        //? if >=26.2 {
        /*final Pig pig = net.minecraft.world.entity.EntityTypes.PIG.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else if >=26.1 {
        /*final Pig pig = EntityType.PIG.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else {
        final Pig pig = EntityType.PIG.create(helper.getLevel());
        //?}
        if (pig == null) {
            helper.fail("failed creating pig");
            throw new IllegalStateException("unreachable");
        }
        pig.setNoAi(true);
        //? if >=26.1 {
        /*pig.snapTo(robot.getX(), robot.getBoundingBox().maxY, robot.getZ(), 0, 0);
        *///?} else {
        pig.moveTo(robot.getX(), robot.getBoundingBox().maxY, robot.getZ(), 0, 0);
        //?}
        helper.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static void requireResult(
            final GameTestHelper helper,
            final RobotActionResult actual,
            final RobotActionResult expected) {
        if (actual != expected) {
            helper.fail("expected " + expected + ", got " + actual);
        }
    }

    private static void requireNear(
            final GameTestHelper helper,
            final Vec3 actual,
            final Vec3 expected,
            final String what) {
        if (actual.distanceTo(expected) > TOLERANCE) {
            helper.fail(what + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireNear(
            final GameTestHelper helper,
            final double actual,
            final double expected,
            final String what) {
        if (Math.abs(actual - expected) > TOLERANCE) {
            helper.fail(what + ": expected " + expected + ", got " + actual);
        }
    }

    private RobotCarryTests() {
    }
}
