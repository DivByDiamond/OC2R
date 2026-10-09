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
        //? if >=26.1 {
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

    private RobotTests() {
    }
}
