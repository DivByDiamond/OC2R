package li.cil.oc2.common.entity.robot;

import static li.cil.oc2.common.Constants.*;

import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.vm.VMRunState;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class RobotSerializer {
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String BUS_ELEMENT_TAG_NAME = "bus_element";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String COMMAND_PROCESSOR_TAG_NAME = "commands";
    private static final String INVENTORY_TAG_NAME = "inventory";
    private static final String SELECTED_SLOT_TAG_NAME = "selected_slot";
    private static final String ENERGY_TAG_NAME = "energy";

    public static void save(final Robot robot, final CompoundTag tag) {
        final var provider = robot.registryAccess();
        if (robot.getVirtualMachine().getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, robot.getVirtualMachine().serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(robot.getTerminal()));
        }
        tag.put(COMMAND_PROCESSOR_TAG_NAME, robot.getMovementController().serialize());
        tag.put(BUS_ELEMENT_TAG_NAME, robot.getRobotInventory().serializeBusElement(provider));
        robot.getRobotInventory()
                .saveItems(provider, NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME));
        tag.put(DEVICES_TAG_NAME, robot.getRobotInventory().saveDevices(provider));
        tag.put(ENERGY_TAG_NAME, robot.getEnergyStorage().serializeNBT(provider));
        tag.put(
                INVENTORY_TAG_NAME,
                robot.getRobotInventory().getInventory().serializeNBT(provider));
        tag.putByte(SELECTED_SLOT_TAG_NAME, (byte) robot.getSelectedSlot());
    }

    public static void load(final Robot robot, final CompoundTag tag) {
        final var provider = robot.registryAccess();
        robot.getVirtualMachine().deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), robot.getTerminal());
        robot.getMovementController().deserialize(tag.getCompound(COMMAND_PROCESSOR_TAG_NAME));
        robot.getRobotInventory().deserializeBusElement(tag.getCompound(BUS_ELEMENT_TAG_NAME));
        robot.getRobotInventory().loadItems(provider, tag.getCompound(ITEMS_TAG_NAME));
        robot.getRobotInventory().loadDevices(provider, tag.getCompound(DEVICES_TAG_NAME));
        robot.getEnergyStorage().deserializeNBT(provider, tag.getCompound(ENERGY_TAG_NAME));
        robot.getRobotInventory()
                .getInventory()
                .deserializeNBT(provider, tag.getCompound(INVENTORY_TAG_NAME));
        robot.setSelectedSlot(tag.getByte(SELECTED_SLOT_TAG_NAME));
    }

    public static void exportToItemStack(final Robot robot, final ItemStack stack) {
        final var container = new RestrictedContainer();
        robot.getRobotInventory().saveItems(container);
        stack.set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER, container);
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
                stack.get(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER);
        final CompoundTag itemsTag = NBTUtils.getChildTag(stack, MOD_TAG_NAME, ITEMS_TAG_NAME);
        if (container != null) {
            robot.getRobotInventory().loadItems(provider, container);
        } else {
            robot.getRobotInventory().loadItems(provider, itemsTag);
            robot.getRobotInventory()
                    .getInventory()
                    .deserializeNBT(provider, itemsTag.getCompound(INVENTORY_TAG_NAME));
        }
        robot.getEnergyStorage()
                .deserializeNBT(
                        provider, NBTUtils.getChildTag(stack, MOD_TAG_NAME, ENERGY_TAG_NAME));
    }
}
