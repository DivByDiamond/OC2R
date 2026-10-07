package li.cil.oc2.common.capabilities;

import li.cil.oc2.common.blockentity.computer.capability.ComputerBlockEntityCapabilities;
import li.cil.oc2.common.blockentity.disk.DiskDriveBlockEntity;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import li.cil.oc2.common.blockentity.energy.CreativeEnergyBlockCapabilities;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.misc.PciCardCageBlockEntity;
import li.cil.oc2.common.blockentity.misc.flash.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.blockentity.misc.gateway.InternetGateWayCapabilities;
import li.cil.oc2.common.blockentity.monitor.misc.MonitorCapabilities;
import li.cil.oc2.common.blockentity.network.cable.BusCableCapabilities;
import li.cil.oc2.common.blockentity.network.connector.interfaces.NetworkConnectorLifecycle;
import li.cil.oc2.common.blockentity.network.hub.NetworkHubBlockEntity;
import li.cil.oc2.common.blockentity.network.vxlan.VxlanBlockEntity;
import li.cil.oc2.common.blockentity.projector.ProjectorCapabilities;
import li.cil.oc2.common.bus.device.rpc.item.card.RedstoneInterfaceCardItemDevice;
import li.cil.oc2.common.bus.device.vm.item.network.NetworkInterfaceCardDevice;
import li.cil.oc2.common.entity.robot.state.RobotCapabilities;
import li.cil.oc2.platform.CapabilityRegistrar;

/**
 * Single list of everything that exposes capabilities, invoked once by the loader module with its
 * {@link CapabilityRegistrar} so no provider depends on a loader-specific setup event.
 */
public final class CapabilityProviders {
    private CapabilityProviders() {}

    public static void registerAll(final CapabilityRegistrar registrar) {
        ComputerBlockEntityCapabilities.registerCapabilities(registrar);
        DiskDriveBlockEntity.registerCapabilities(registrar);
        ChargerBlockEntity.registerCapabilities(registrar);
        CreativeEnergyBlockCapabilities.registerCapabilities(registrar);
        KeyboardBlockEntity.registerCapabilities(registrar);
        FlashMemoryFlasherBlockEntity.registerCapabilities(registrar);
        InternetGateWayCapabilities.registerCapabilities(registrar);
        PciCardCageBlockEntity.registerCapabilities(registrar);
        MonitorCapabilities.registerCapabilities(registrar);
        ProjectorCapabilities.registerCapabilities(registrar);
        BusCableCapabilities.registerCapabilities(registrar);
        NetworkConnectorLifecycle.registerCapabilities(registrar);
        NetworkHubBlockEntity.registerCapabilities(registrar);
        VxlanBlockEntity.registerCapabilities(registrar);
        RedstoneInterfaceCardItemDevice.registerCapabilities(registrar);
        NetworkInterfaceCardDevice.registerCapabilities(registrar);
        RobotCapabilities.registerCapabilities(registrar);
    }
}
