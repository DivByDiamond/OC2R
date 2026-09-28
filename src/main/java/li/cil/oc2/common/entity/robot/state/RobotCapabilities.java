package li.cil.oc2.common.entity.robot.state;

import li.cil.oc2.api.API;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.platform.NeoForgeEnergyCapabilityRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class RobotCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerEntity(
                Capabilities.ItemHandler.ENTITY,
                Entities.ROBOT.get(),
                (robot, ctx) -> robot.getInventory());
        if (Config.robotsUseEnergy()) {
            new NeoForgeEnergyCapabilityRegistrar(event).registerEntity(
                    Entities.ROBOT.get(),
                    (robot, side) -> ((Robot) robot).getEnergyStorage());
        }
        event.registerEntity(
                Capabilities.Robot.ENTITY, Entities.ROBOT.get(), (robot, ctx) -> robot);
    }
}