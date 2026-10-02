package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.API;
import li.cil.oc2.platform.BlockCapability;
import li.cil.oc2.platform.EntityCapability;
import li.cil.oc2.platform.ItemCapability;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-independent keys for the capabilities this mod exposes and consumes. Registration goes
 * through {@link CapabilitySetup#registrar}, lookups through {@code Platform#capabilities()}; no
 * loader type appears here, so the same keys work on every loader.
 *
 * <p>Energy, item handler and fluid handler keep the ids NeoForge (and Fabric) already use for
 * them, so our providers stay visible to other mods and their providers stay visible to us.
 */
public final class Capabilities {
    public static final class DeviceBusElement {
        public static final BlockCapability<li.cil.oc2.api.bus.DeviceBusElement> BLOCK =
                BlockCapability.createSided(
                        id("device_bus_element"), li.cil.oc2.api.bus.DeviceBusElement.class);
    }

    public static final class Device {
        public static final BlockCapability<li.cil.oc2.api.bus.device.Device> BLOCK =
                BlockCapability.createSided(
                        id("device"), li.cil.oc2.api.bus.device.Device.class);
        public static final ItemCapability<li.cil.oc2.api.bus.device.Device> ITEM =
                ItemCapability.createVoid(id("device"), li.cil.oc2.api.bus.device.Device.class);
    }

    public static final class RedstoneEmitter {
        public static final BlockCapability<li.cil.oc2.api.capabilities.RedstoneEmitter> BLOCK =
                BlockCapability.createSided(
                        id("redstone_emitter"), li.cil.oc2.api.capabilities.RedstoneEmitter.class);
    }

    public static final class NetworkInterface {
        public static final BlockCapability<li.cil.oc2.api.capabilities.NetworkInterface> BLOCK =
                BlockCapability.createSided(
                        id("network_interface"),
                        li.cil.oc2.api.capabilities.NetworkInterface.class);
    }

    public static final class TerminalUserProvider {
        public static final BlockCapability<
                        li.cil.oc2.api.capabilities.TerminalUserProvider>
                BLOCK =
                        BlockCapability.createVoid(
                                id("terminal_user_provider"),
                                li.cil.oc2.api.capabilities.TerminalUserProvider.class);
        public static final EntityCapability<
                        li.cil.oc2.api.capabilities.TerminalUserProvider>
                ENTITY =
                        EntityCapability.createVoid(
                                id("terminal_user_provider"),
                                li.cil.oc2.api.capabilities.TerminalUserProvider.class);
    }

    public static final class Robot {
        public static final EntityCapability<li.cil.oc2.api.capabilities.Robot> ENTITY =
                EntityCapability.createVoid(
                        id("robot"), li.cil.oc2.api.capabilities.Robot.class);
    }

    public static final class EnergyStorage {
        public static final BlockCapability<li.cil.oc2.platform.EnergyStorage> BLOCK =
                BlockCapability.createSided(
                        standard("energy"), li.cil.oc2.platform.EnergyStorage.class);
        public static final EntityCapability<li.cil.oc2.platform.EnergyStorage> ENTITY =
                EntityCapability.createSided(
                        standard("energy"), li.cil.oc2.platform.EnergyStorage.class);
        public static final ItemCapability<li.cil.oc2.platform.EnergyStorage> ITEM =
                ItemCapability.createVoid(
                        standard("energy"), li.cil.oc2.platform.EnergyStorage.class);
    }

    public static final class ItemHandler {
        public static final BlockCapability<li.cil.oc2.platform.ItemHandler> BLOCK =
                BlockCapability.createSided(
                        standard("item_handler"), li.cil.oc2.platform.ItemHandler.class);
        public static final EntityCapability<li.cil.oc2.platform.ItemHandler> ENTITY =
                EntityCapability.createVoid(
                        standard("item_handler"), li.cil.oc2.platform.ItemHandler.class);
        public static final ItemCapability<li.cil.oc2.platform.ItemHandler> ITEM =
                ItemCapability.createVoid(
                        standard("item_handler"), li.cil.oc2.platform.ItemHandler.class);
    }

    public static final class FluidHandler {
        public static final BlockCapability<li.cil.oc2.platform.FluidHandler> BLOCK =
                BlockCapability.createSided(
                        standard("fluid_handler"), li.cil.oc2.platform.FluidHandler.class);
        public static final EntityCapability<li.cil.oc2.platform.FluidHandler> ENTITY =
                EntityCapability.createSided(
                        standard("fluid_handler"), li.cil.oc2.platform.FluidHandler.class);
        public static final ItemCapability<li.cil.oc2.platform.FluidHandler> ITEM =
                ItemCapability.createVoid(
                        standard("fluid_handler"), li.cil.oc2.platform.FluidHandler.class);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, path);
    }

    private static ResourceLocation standard(final String path) {
        return ResourceLocation.fromNamespaceAndPath("neoforge", path);
    }
}
