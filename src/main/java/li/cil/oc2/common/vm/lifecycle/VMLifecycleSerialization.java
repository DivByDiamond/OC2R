package li.cil.oc2.common.vm.lifecycle;

import java.io.IOException;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
//? if <26.1 {
import li.cil.oc2.common.util.nbt.NBTTagIds;
//?}
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import net.minecraft.nbt.CompoundTag;

final class VMLifecycleSerialization {
    static CompoundTag serialize(final AbstractVirtualMachine vm) {
        final CompoundTag tag = new CompoundTag();

        if (vm.runner != null) {
            tag.put(VMLifecycle.RUNNER_TAG_NAME, NBTSerialization.serialize(vm.runner));
        } else {
            NBTUtils.putEnum(tag, AbstractVirtualMachine.RUN_STATE_TAG_NAME, vm.runState);
        }

        tag.put(VMLifecycle.STATE_TAG_NAME, NBTSerialization.serialize(vm.state));

        return tag;
    }

    static void deserialize(final AbstractVirtualMachine vm, final CompoundTag tag) throws IOException {
        //? if >=26.1 {
        /*if (tag.contains(VMLifecycle.RUNNER_TAG_NAME)) {
        *///?} else {
        if (tag.contains(VMLifecycle.RUNNER_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
        //?}
            vm.runner = vm.createRunner();
            //? if >=26.1 {
            /*NBTSerialization.deserialize(tag.getCompoundOrEmpty(VMLifecycle.RUNNER_TAG_NAME), vm.runner);
            *///?} else {
            NBTSerialization.deserialize(tag.getCompound(VMLifecycle.RUNNER_TAG_NAME), vm.runner);
            //?}
            vm.runState = VMRunState.LOADING_DEVICES;
        } else {
            vm.runState =
                    NBTUtils.getEnum(
                            tag, AbstractVirtualMachine.RUN_STATE_TAG_NAME, VMRunState.class);
            if (vm.runState == null) {
                vm.runState = VMRunState.STOPPED;
            } else if (vm.runState == VMRunState.RUNNING) {
                vm.runState = VMRunState.LOADING_DEVICES;
            }
        }

        //? if >=26.1 {
        /*if (tag.contains(VMLifecycle.STATE_TAG_NAME)) {
            final CompoundTag savedState = tag.getCompoundOrEmpty(VMLifecycle.STATE_TAG_NAME);
            prepareRootFilesystem(vm, savedState);
            NBTSerialization.deserialize(savedState, vm.state);
        *///?} else {
        if (tag.contains(VMLifecycle.STATE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            final CompoundTag savedState = tag.getCompound(VMLifecycle.STATE_TAG_NAME);
            prepareRootFilesystem(vm, savedState);
            NBTSerialization.deserialize(savedState, vm.state);
        //?}
        }
    }

    private static void prepareRootFilesystem(final AbstractVirtualMachine vm, final CompoundTag state) throws IOException {
        // Ceres restores sparse pages into an existing backing image. Construct that image first;
        // otherwise an Onyx overlay could be restored without its immutable packaged base.
        final var devices = state.get("builtinDevices");
        if (devices instanceof CompoundTag saved
                && saved.get("onyxRootFilesystem") instanceof CompoundTag root
                && root.contains("overlay")) {
            // The base identity selects the immutable image the sparse pages are restored onto.
            // Without the tag the identity is unknown: resolving it would silently pick the
            // legacy image and restore the pages onto the wrong base.
            if (!root.contains("baseImageId")) {
                throw new IOException("Missing OnyxOS root base identity; original snapshot retained");
            }
            if (!(root.get("baseImageId") instanceof net.minecraft.nbt.StringTag)) {
                throw new IOException("Invalid OnyxOS root base identity; original snapshot retained");
            }
            //? if >=26.1 {
            /*final String baseImageId = root.getStringOr("baseImageId", "");
            *///?} else {
            final String baseImageId = root.getString("baseImageId");
            //?}
            if (baseImageId.isEmpty()) {
                throw new IOException("Empty OnyxOS root base identity; original snapshot retained");
            }
            vm.state.builtinDevices.prepareSavedOnyxRoot(baseImageId);
        }
    }
}
