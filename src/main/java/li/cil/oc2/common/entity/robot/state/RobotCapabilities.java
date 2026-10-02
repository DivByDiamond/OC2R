package li.cil.oc2.common.entity.robot.state;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.NeoForgeItemHandlers;

public final class RobotCapabilities {
    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerEntity(
                Capabilities.ItemHandler.ENTITY,
                Entities.ROBOT.get(),
                (robot, ctx) -> NeoForgeItemHandlers.adapt(((Robot) robot).getInventory()));
        if (Config.robotsUseEnergy()) {
registrar.registerEntity(
                    Capabilities.EnergyStorage.ENTITY,
                    Entities.ROBOT.get(),
                    (robot, side) -> ((Robot) robot).getEnergyStorage());
        }
        registrar.registerEntity(
                Capabilities.Robot.ENTITY, Entities.ROBOT.get(), (robot, ctx) -> (Robot) robot);
    }
}