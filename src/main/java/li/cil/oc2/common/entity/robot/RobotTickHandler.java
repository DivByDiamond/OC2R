package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActions;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.robot.RobotInitializationRequestMessage;

public final class RobotTickHandler {
    public static void tick(final Robot robot, final boolean firstTick) {
        final boolean isClient = robot.level().isClientSide();

        if (firstTick) {
            if (isClient) {
                NetworkMessages.sendToServer(new RobotInitializationRequestMessage(robot));
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
            robot.updateOrigin();
            robot.getRadioEndpoint().tick();
            if (robot.tickCount % 20 == 0 && robot.getRadioEndpoint().enrolled()) {
                robot.getRadioEndpoint()
                        .publish(
                                "robot",
                                "energy",
                                "{\"stored\":"
                                        + robot.getEnergyStorage().getEnergyStored()
                                        + ",\"capacity\":"
                                        + robot.getEnergyStorage().getMaxEnergyStored()
                                        + "}");
                int occupied = 0;
                for (int slot = 0; slot < robot.getInventory().getSlots(); slot++) {
                    if (!robot.getInventory().getStackInSlot(slot).isEmpty()) occupied++;
                }
                robot.getRadioEndpoint()
                        .publish(
                                "robot",
                                "storage",
                                "{\"occupiedSlots\":"
                                        + occupied
                                        + ",\"slots\":"
                                        + robot.getInventory().getSlots()
                                        + "}");
                robot.getRadioEndpoint()
                        .publish(
                                "robot",
                                "tasks",
                                "{\"queued\":"
                                        + robot.getMovementController().getQueuedActionCount()
                                        + ",\"lastActionId\":"
                                        + robot.getMovementController().getLastActionId()
                                        + "}");
            }
            robot.getVirtualMachine().tick();
        }

        robot.getMovementController().tick();

        if (!isClient) {
            robot.getBlockCollider().collideWithWorld();
        }
    }
}
