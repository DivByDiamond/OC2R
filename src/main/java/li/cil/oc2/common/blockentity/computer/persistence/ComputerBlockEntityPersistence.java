package li.cil.oc2.common.blockentity.computer.persistence;

import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

public final class ComputerBlockEntityPersistence {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String ENERGY_TAG_NAME = "energy";

    public static CompoundTag getUpdateTag(final ComputerBlockEntity computer, final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(computer.terminal));
        tag.putInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME, computer.virtualMachine.getBusState().ordinal());
        tag.putInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME, computer.virtualMachine.getRunState().ordinal());
        tag.putString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME, Component.Serializer.toJson(computer.virtualMachine.getBootError(), registries));
        return tag;
    }

    public static void handleUpdateTag(final ComputerBlockEntity computer, final CompoundTag tag, final HolderLookup.Provider registries) {
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), computer.terminal);
    }

    public static void handleUpdateTagClient(final ComputerBlockEntity computer, final CompoundTag tag, final HolderLookup.Provider registries) {
        handleUpdateTag(computer, tag, registries);
        var level = computer.getLevel();
        if (level != null && level.isClientSide()) {
            computer.virtualMachine.setBusStateClient(li.cil.oc2.common.bus.controller.BusState.values()[tag.getInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME)]);
            computer.virtualMachine.setRunStateClient(VMRunState.values()[tag.getInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME)]);
            computer.virtualMachine.setBootErrorClient(Component.Serializer.fromJson(tag.getString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME), registries));
        }
    }

    public static void saveAdditional(final ComputerBlockEntity computer, final CompoundTag tag, final HolderLookup.Provider registries) {
        if (computer.virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, computer.virtualMachine.serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(computer.terminal));
        }
        tag.put(ENERGY_TAG_NAME, computer.energy.serializeNBT(registries));
        tag.put(BUS_ELEMENT_TAG_NAME, computer.busElement.save(registries));
        tag.put(ITEMS_TAG_NAME, computer.deviceItems.saveItems(registries));
        tag.put(DEVICES_TAG_NAME, computer.deviceItems.saveDevices(registries));
    }

    public static void loadAdditional(final ComputerBlockEntity computer, final CompoundTag tag, final HolderLookup.Provider registries) {
        computer.energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG_NAME));
        computer.busElement.loadAdditional(tag.getCompound(BUS_ELEMENT_TAG_NAME), registries);
        computer.deviceItems.loadItems(registries, tag.getCompound(ITEMS_TAG_NAME));
        computer.deviceItems.loadDevices(registries, tag.getCompound(DEVICES_TAG_NAME));
        computer.virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), computer.terminal);
    }
}
