
package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.RobotInitializationRequestMessage;

public final class RobotTickHandler {
    public static void tick(final Robot robot, final boolean firstTick) {
        final boolean isClient = robot.level().isClientSide();

        if (firstTick) {
            if (isClient) {
                Network.sendToServer(new RobotInitializationRequestMessage(robot));
            } else {
                robot.getEventHandler().register();
                RobotActions.initializeData(robot);
                if (robot.getMovementController().getCurrentAction() != null) {
                    robot.getMovementController().getCurrentAction().initialize(robot);
                }
            }
        }

        if (isClient) {
            robot.getTerminal().clientTick();
        }

        if (!isClient) {
            robot.getVirtualMachine().tick();
        }

        robot.getMovementController().tick();

        if (!isClient) {
            robot.getBlockCollider().collideWithWorld();
        }
    }
}
