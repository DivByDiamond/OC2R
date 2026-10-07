package li.cil.oc2.fabric.gametest;

import java.util.List;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import li.cil.oc2.common.item.Items;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The robot entity on Fabric: its type is registered, it is created by the robot item (like on
 * NeoForge, where {@code /summon oc2r:robot} is refused on purpose: the type is {@code noSummon}),
 * it carries its attributes and it survives a save/load cycle.
 */
public final class FabricRobotTests implements FabricGameTest {
    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);

    @GameTest(template = EMPTY_STRUCTURE)
    public void robotTypeIsRegisteredButNotSummonable(final GameTestHelper helper) {
        final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(Entities.ROBOT.get());
        if (!"oc2r:robot".equals(id.toString())) {
            helper.fail("robot entity type registered as " + id);
        }
        if (Entities.ROBOT.get().canSummon()) {
            helper.fail("robots are placed by their item, /summon must stay refused");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void robotItemPlacesRobotEntity(final GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final ItemStack stack = new ItemStack(Items.ROBOT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos absolute = helper.absolutePos(FLOOR);
        final BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        final List<Robot> robots = helper.getLevel().getEntitiesOfClass(Robot.class, new AABB(absolute).inflate(3));
        if (robots.size() != 1) {
            helper.fail("expected one robot next to " + absolute + ", found " + robots.size());
        }
        final Robot robot = robots.get(0);
        if (robot.getType() != Entities.ROBOT.get()) {
            helper.fail("wrong entity type " + robot.getType());
        }
        if (!robot.isAlive() || robot.isRemoved()) {
            helper.fail("robot was not added to the level");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void robotSurvivesSaveAndLoad(final GameTestHelper helper) {
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
        }
        final var loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), e -> e);
        if (!(loaded instanceof Robot)) {
            helper.fail("loading the saved robot gave " + loaded);
        }
        helper.succeed();
    }
}
