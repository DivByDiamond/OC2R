package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotTests {
    /** Robots are saved through RobotSerializer, which is first loaded by the save itself. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void robotSurvivesSaveAndLoad(final GameTestHelper helper) {
        final Robot robot = Entities.ROBOT.get().create(helper.getLevel());
        if (robot == null) {
            helper.fail("EntityType.create returned null");
            return;
        }
        final Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        robot.moveTo(pos.x, pos.y, pos.z, 90, 0);
        RobotActions.initializeData(robot);

        final CompoundTag tag = new CompoundTag();
        if (!robot.save(tag)) {
            helper.fail("robot refused to save");
            return;
        }
        final var loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), e -> e);
        if (!(loaded instanceof Robot)) {
            helper.fail("loading the saved robot gave " + loaded);
            return;
        }
        helper.succeed();
    }

    private RobotTests() {
    }
}
