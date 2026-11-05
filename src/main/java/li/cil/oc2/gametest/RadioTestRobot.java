package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.radio.OraEndpoint;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.movement.MovementDirection;
import li.cil.oc2.common.entity.robot.movement.RobotMovementAction;
import li.cil.oc2.common.entity.robot.rotation.RotationDirection;
import li.cil.oc2.common.entity.robot.state.RobotDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A real robot for radio GameTests. It boots OnyxOS from its own CPU, memory and flash, and its
 * actions are queued through the robot bus device callbacks a guest program calls, which accept
 * actions only while the robot's computer is really running. Nothing about the VM is faked.
 */
final class RadioTestRobot {
    final Robot robot;
    final Vec3 home;
    private final GameTestHelper helper;
    private final RobotDevice device;

    private RadioTestRobot(final GameTestHelper helper, final Robot robot, final Vec3 home) {
        this.helper = helper;
        this.robot = robot;
        this.home = home;
        this.device = new RobotDevice(robot);
    }

    /** Spawns a robot standing on a stone floor in the given block of the test structure. */
    static RadioTestRobot spawn(final GameTestHelper helper, final BlockPos relative) {
        TestSupport.placeFloor(helper, relative);
        //? if >=26.1 {
        /*final Robot robot = Entities.ROBOT.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else {
        final Robot robot = Entities.ROBOT.get().create(helper.getLevel());
        //?}
        if (robot == null) throw TestSupport.failure(helper, "EntityType.create returned null");
        final Vec3 home = RobotMovementAction.getTargetPositionInBlock(helper.absolutePos(relative));
        robot.setPos(home.x, home.y, home.z);
        helper.getLevel().addFreshEntity(robot);
        return new RadioTestRobot(helper, robot, home);
    }

    OraEndpoint endpoint() {
        return robot.getRadioEndpoint();
    }

    /** Installs a CPU, memory and the OnyxOS flash, fills the battery and powers the robot on. */
    void boot() {
        install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        robot.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false);
        robot.start();
    }

    /** Fails until the robot's computer runs; use inside {@code thenWaitUntil}. */
    void assertRunning() {
        TestSupport.assertTrue(helper, "robot computer is running (" + robot.getVirtualMachine().getRunState()
                        + ", bus " + robot.getVirtualMachine().getBusState()
                        + ", error " + robot.getVirtualMachine().getError() + ")",
                robot.getVirtualMachine().isRunning());
    }

    /** Queues a movement the way a guest program does and returns the action id the robot assigned. */
    int move(final MovementDirection direction) {
        TestSupport.assertTrue(helper, "robot accepted the " + direction + " movement", device.move(direction));
        return device.getLastActionId();
    }

    /** Queues a turn the way a guest program does and returns the action id the robot assigned. */
    int turn(final RotationDirection direction) {
        TestSupport.assertTrue(helper, "robot accepted the " + direction + " turn", device.turn(direction));
        return device.getLastActionId();
    }

    int lastActionId() {
        return device.getLastActionId();
    }

    boolean attached() {
        return endpoint().status().get("attached").equals(true);
    }

    /** Puts the robot at an X offset from its home position, as if it had driven there. */
    void placeAtOffset(final double dx) {
        robot.setPos(home.x + dx, home.y, home.z);
    }

    private void install(final DeviceType type, final ItemStack stack) {
        final ItemHandler handler = robot.getItemStackHandlers().getItemHandler(type)
                .orElseThrow(() -> TestSupport.failure(helper, "robot has no " + type + " slot"));
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack, false).isEmpty()) return;
        }
        throw TestSupport.failure(helper, "robot rejected " + stack);
    }
}
