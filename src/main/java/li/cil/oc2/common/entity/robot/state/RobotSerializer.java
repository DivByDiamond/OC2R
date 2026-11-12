package li.cil.oc2.common.entity.robot.state;

import static li.cil.oc2.common.Constants.*;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.oc2.common.tablet.TabletStore;
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RobotSerializer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String BUS_ELEMENT_TAG_NAME = "bus_element";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String COMMAND_PROCESSOR_TAG_NAME = "commands";
    private static final String INVENTORY_TAG_NAME = "inventory";
    private static final String SELECTED_SLOT_TAG_NAME = "selected_slot";
    private static final String ENERGY_TAG_NAME = "energy";
    private static final String ORIGIN_TAG_NAME = "origin";

    public static void save(final Robot robot, final CompoundTag tag) {
        final var provider = robot.registryAccess();
        robot.getRadioEndpoint().save(tag);
        saveMachine(robot, tag);
        tag.put(COMMAND_PROCESSOR_TAG_NAME, robot.getMovementController().serialize());
        tag.put(ENERGY_TAG_NAME, robot.getEnergyStorage().serializeNBT(provider));
        tag.put(
                INVENTORY_TAG_NAME,
                robot.getRobotInventory().getInventory().serializeNBT(provider));
        tag.putByte(SELECTED_SLOT_TAG_NAME, (byte) robot.getSelectedSlot());
        if (robot.getOrigin() != null) {
            GlobalPos.CODEC
                    .encodeStart(NbtOps.INSTANCE, robot.getOrigin())
                    .result()
                    .ifPresent(originTag -> tag.put(ORIGIN_TAG_NAME, originTag));
        }
    }

    public static void load(final Robot robot, final CompoundTag tag) {
        robot.getRadioEndpoint().load(tag);
        final var provider = robot.registryAccess();
        loadMachine(robot, tag);
        //? if >=26.1 {
        /*robot.getMovementController().deserialize(tag.getCompoundOrEmpty(COMMAND_PROCESSOR_TAG_NAME));
        robot.getEnergyStorage().deserializeNBT(provider, tag.getCompoundOrEmpty(ENERGY_TAG_NAME));
        *///?} else {
        robot.getMovementController().deserialize(tag.getCompound(COMMAND_PROCESSOR_TAG_NAME));
        robot.getEnergyStorage().deserializeNBT(provider, tag.getCompound(ENERGY_TAG_NAME));
        //?}
        robot.getRobotInventory()
                .getInventory()
                //? if >=26.1 {
                /*.deserializeNBT(provider, tag.getCompoundOrEmpty(INVENTORY_TAG_NAME));
                robot.setSelectedSlot(tag.getByteOr(SELECTED_SLOT_TAG_NAME, (byte) 0));
                        *///?} else {
                .deserializeNBT(provider, tag.getCompound(INVENTORY_TAG_NAME));
        robot.setSelectedSlot(tag.getByte(SELECTED_SLOT_TAG_NAME));
        //?}
        robot.setOrigin(
                GlobalPos.CODEC
                        .parse(NbtOps.INSTANCE, NBTUtils.getChildTag(tag, ORIGIN_TAG_NAME))
                        .result()
                        .orElse(null));
    }

    /** The virtual machine with its private root disk, the terminal and the device slots with their state. */
    private static void saveMachine(final Robot robot, final CompoundTag tag) {
        final var provider = robot.registryAccess();
        // The VM state owns the private root disk even when its CPU is powered off.
        tag.put(STATE_TAG_NAME, robot.getVirtualMachine().serialize());
        if (robot.getVirtualMachine().getRunState() != VMRunState.STOPPED) {
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(robot.getTerminal()));
        }
        tag.put(BUS_ELEMENT_TAG_NAME, robot.getRobotInventory().serializeBusElement(provider));
        robot.getRobotInventory()
                .saveItems(provider, NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME));
        tag.put(DEVICES_TAG_NAME, robot.getRobotInventory().saveDevices(provider));
    }

    private static void loadMachine(final Robot robot, final CompoundTag tag) {
        final var provider = robot.registryAccess();
        robot.getVirtualMachine().deserialize(NBTUtils.getChildTag(tag, STATE_TAG_NAME));
        NBTSerialization.deserialize(NBTUtils.getChildTag(tag, TERMINAL_TAG_NAME), robot.getTerminal());
        robot.getRobotInventory().deserializeBusElement(NBTUtils.getChildTag(tag, BUS_ELEMENT_TAG_NAME));
        robot.getRobotInventory().resetDevices();
        robot.getRobotInventory().loadItems(provider, NBTUtils.getChildTag(tag, ITEMS_TAG_NAME));
        robot.getRobotInventory().loadDevices(provider, NBTUtils.getChildTag(tag, DEVICES_TAG_NAME));
    }

    /** Machine states of robots carried as items; the item only names its file. */
    public static TabletStore machineStore(final MinecraftServer server) {
        return new TabletStore(server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("oc2r_robots"));
    }

    /**
     * Memory, private root disk and terminal are far too large for an item, so they go to the world
     * folder and the item carries their id. Without it the robot comes back powered off with a fresh disk.
     */
    private static void exportMachine(final Robot robot, final ItemStack stack) {
        final MinecraftServer server = robot.level().getServer();
        if (server == null) return;
        final UUID id = UUID.randomUUID();
        final CompoundTag machine = new CompoundTag();
        try {
            saveMachine(robot, machine);
            machineStore(server).save(id, machine);
            stack.set(li.cil.oc2.common.components.DataComponents.ROBOT_STATE_ID.get(), id);
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Cannot keep the machine state of a picked up robot; it will start fresh", e);
        }
    }

    /** Takes the stored state once: a copied item finds nothing and starts fresh. */
    private static void importMachine(final Robot robot, final ItemStack stack) {
        final MinecraftServer server = robot.level().getServer();
        final UUID id = stack.get(li.cil.oc2.common.components.DataComponents.ROBOT_STATE_ID.get());
        if (server == null || id == null) return;
        final TabletStore store = machineStore(server);
        try {
            final Optional<CompoundTag> machine = store.load(id);
            if (machine.isEmpty()) return;
            loadMachine(robot, machine.get());
            store.delete(id);
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Cannot restore the machine state {} of a placed robot; its file is kept", id, e);
            robot.getVirtualMachine().stop();
        }
    }

    public static void exportToItemStack(final Robot robot, final ItemStack stack) {
        exportMachine(robot, stack);
        final var container = new RestrictedContainer();
        robot.getRobotInventory().saveItems(container);
        stack.set(
                li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER.get(), container);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                (nbt) -> {
                    final var tag = NBTUtils.getOrCreateChildTag(nbt, MOD_TAG_NAME);
                    tag.put(
                            ENERGY_TAG_NAME,
                            robot.getEnergyStorage().serializeNBT(robot.registryAccess()));
                });
    }

    public static void importFromItemStack(final Robot robot, final ItemStack stack) {
        final var provider = robot.registryAccess();
        final var container =
                stack.get(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER.get());
        final CompoundTag itemsTag = NBTUtils.getChildTag(stack, MOD_TAG_NAME, ITEMS_TAG_NAME);
        if (container != null) {
            robot.getRobotInventory().loadItems(provider, container);
        } else {
            robot.getRobotInventory().loadItems(provider, itemsTag);
            robot.getRobotInventory()
                    .getInventory()
                    //? if >=26.1 {
                    /*.deserializeNBT(provider, itemsTag.getCompoundOrEmpty(INVENTORY_TAG_NAME));
                     *///?} else {
                    .deserializeNBT(provider, itemsTag.getCompound(INVENTORY_TAG_NAME));
            //?}
        }
        robot.getEnergyStorage()
                .deserializeNBT(
                        provider, NBTUtils.getChildTag(stack, MOD_TAG_NAME, ENERGY_TAG_NAME));
        importMachine(robot, stack);
    }
}
