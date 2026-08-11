package li.cil.oc2.common.vm.lifecycle;

import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.oc2.common.util.nbt.NBTTagIds;
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

    static void deserialize(final AbstractVirtualMachine vm, final CompoundTag tag) {
        if (tag.contains(VMLifecycle.RUNNER_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            vm.runner = vm.createRunner();
            NBTSerialization.deserialize(tag.getCompound(VMLifecycle.RUNNER_TAG_NAME), vm.runner);
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

        if (tag.contains(VMLifecycle.STATE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(tag.getCompound(VMLifecycle.STATE_TAG_NAME), vm.state);
        }
    }
}