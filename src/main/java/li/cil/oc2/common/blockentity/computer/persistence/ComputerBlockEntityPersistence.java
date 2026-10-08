package li.cil.oc2.common.blockentity.computer.persistence;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public final class ComputerBlockEntityPersistence {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String ENERGY_TAG_NAME = "energy";

    public static CompoundTag getUpdateTag(
            final ComputerBlockEntity computer, final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(computer.terminalManager.terminal));
        tag.putString(
                AbstractVirtualMachine.BUS_STATE_TAG_NAME,
                computer.virtualMachine.getBusState().name());
        tag.putInt(
                AbstractVirtualMachine.BUS_OVERFLOW_TAG_NAME,
                computer.virtualMachine.getBusOverflow());
        tag.putString(
                AbstractVirtualMachine.RUN_STATE_TAG_NAME,
                computer.virtualMachine.getRunState().name());
        tag.putString(
                AbstractVirtualMachine.BOOT_ERROR_TAG_NAME,
                //? if >=26.1 {
                /*li.cil.oc2.common.util.text.ComponentJson.toJson(computer.virtualMachine.getBootError(), registries));
                *///?} else {
                Component.Serializer.toJson(computer.virtualMachine.getBootError(), registries));
                //?}
        return tag;
    }

    public static void handleUpdateTag(
            final ComputerBlockEntity computer,
            final CompoundTag tag,
            final HolderLookup.Provider registries) {
        //? if >=26.1 {
        /*NBTSerialization.deserialize(tag.getCompoundOrEmpty(TERMINAL_TAG_NAME), computer.terminalManager.terminal);
        *///?} else {
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), computer.terminalManager.terminal);
        //?}
    }

    public static void handleUpdateTagClient(
            final ComputerBlockEntity computer,
            final CompoundTag tag,
            final HolderLookup.Provider registries) {
        handleUpdateTag(computer, tag, registries);
        var level = computer.getLevel();
        if (level != null && level.isClientSide()) {
            // The whole payload above (terminal *and* bus/run state) is nested under the terminal
            // key by ComputerBlockEntity#getUpdateTag, so state is read from the same compound.
            //? if >=26.1 {
            /*var data = tag.getCompoundOrEmpty(TERMINAL_TAG_NAME);
            *///?} else {
            var data = tag.getCompound(TERMINAL_TAG_NAME);
            //?}
            computer.virtualMachine.setBusStateClient(
                    readBusState(data, AbstractVirtualMachine.BUS_STATE_TAG_NAME));
            computer.virtualMachine.setBusOverflowClient(
                    //? if >=26.1 {
                    /*data.getIntOr(AbstractVirtualMachine.BUS_OVERFLOW_TAG_NAME, 0));
                    *///?} else {
                    data.getInt(AbstractVirtualMachine.BUS_OVERFLOW_TAG_NAME));
                    //?}
            computer.virtualMachine.setRunStateClient(
                    readRunState(data, AbstractVirtualMachine.RUN_STATE_TAG_NAME));
            computer.virtualMachine.setBootErrorClient(
                    //? if >=26.1 {
                    /*li.cil.oc2.common.util.text.ComponentJson.fromJson(
                            data.getStringOr(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME, ""), registries));
                    *///?} else {
                    Component.Serializer.fromJson(
                            data.getString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME), registries));
                    //?}
        }
    }

    private static BusState readBusState(final CompoundTag tag, final String key) {
        //? if >=26.1 {
        /*if (tag.contains(key)) {
            final String name = tag.getStringOr(key, "");
        *///?} else {
        if (tag.contains(key, Tag.TAG_STRING)) {
            final String name = tag.getString(key);
        //?}
            try {
                return BusState.valueOf(name);
            } catch (final IllegalArgumentException ignored) {
                // fall back to legacy int
            }
        }
        //? if >=26.1 {
        /*if (tag.contains(key)) {
            final int ordinal = tag.getIntOr(key, 0);
        *///?} else {
        if (tag.contains(key, Tag.TAG_INT)) {
            final int ordinal = tag.getInt(key);
        //?}
            final BusState[] constants = BusState.class.getEnumConstants();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
        }
        return BusState.SCAN_PENDING;
    }

    private static VMRunState readRunState(final CompoundTag tag, final String key) {
        //? if >=26.1 {
        /*if (tag.contains(key)) {
            final String name = tag.getStringOr(key, "");
        *///?} else {
        if (tag.contains(key, Tag.TAG_STRING)) {
            final String name = tag.getString(key);
        //?}
            try {
                return VMRunState.valueOf(name);
            } catch (final IllegalArgumentException ignored) {
                // fall back to legacy int
            }
        }
        //? if >=26.1 {
        /*if (tag.contains(key)) {
            final int ordinal = tag.getIntOr(key, 0);
        *///?} else {
        if (tag.contains(key, Tag.TAG_INT)) {
            final int ordinal = tag.getInt(key);
        //?}
            final VMRunState[] constants = VMRunState.class.getEnumConstants();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
        }
        return VMRunState.STOPPED;
    }

    public static void saveAdditional(
            final ComputerBlockEntity computer,
            final CompoundTag tag,
            final HolderLookup.Provider registries) {
        if (computer.virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, computer.virtualMachine.serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(computer.terminalManager.terminal));
        }
        tag.put(ENERGY_TAG_NAME, computer.energy.serializeNBT(registries));
        tag.put(BUS_ELEMENT_TAG_NAME, computer.busElement.save(registries));
        tag.put(ITEMS_TAG_NAME, computer.deviceItems.saveItems(registries));
        tag.put(DEVICES_TAG_NAME, computer.deviceItems.saveDevices(registries));
    }

    public static void loadAdditional(
            final ComputerBlockEntity computer,
            final CompoundTag tag,
            final HolderLookup.Provider registries) {
        //? if >=26.1 {
        /*computer.energy.deserializeNBT(registries, tag.getCompoundOrEmpty(ENERGY_TAG_NAME));
        computer.busElement.loadAdditional(tag.getCompoundOrEmpty(BUS_ELEMENT_TAG_NAME), registries);
        computer.deviceItems.loadItems(registries, tag.getCompoundOrEmpty(ITEMS_TAG_NAME));
        computer.deviceItems.loadDevices(registries, tag.getCompoundOrEmpty(DEVICES_TAG_NAME));
        computer.virtualMachine.deserialize(tag.getCompoundOrEmpty(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompoundOrEmpty(TERMINAL_TAG_NAME), computer.terminalManager.terminal);
        *///?} else {
        computer.energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG_NAME));
        computer.busElement.loadAdditional(tag.getCompound(BUS_ELEMENT_TAG_NAME), registries);
        computer.deviceItems.loadItems(registries, tag.getCompound(ITEMS_TAG_NAME));
        computer.deviceItems.loadDevices(registries, tag.getCompound(DEVICES_TAG_NAME));
        computer.virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), computer.terminalManager.terminal);
        //?}
    }
}