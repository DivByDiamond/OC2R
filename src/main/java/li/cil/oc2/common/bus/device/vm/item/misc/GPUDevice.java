package li.cil.oc2.common.bus.device.vm.item.misc;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class GPUDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice {
    private final int width;
    private final int height;
    private final int tier;

    public GPUDevice(final ItemStack identity, final int width, final int height, final int tier) {
        super(identity);
        this.width = width;
        this.height = height;
        this.tier = tier;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {}

    @Override
    public void dispose() {}

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag tag) {}
}