package li.cil.oc2.common.bus.device.provider.item;

import java.util.Optional;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.GPUDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.GPUItem;
import net.minecraft.world.item.ItemStack;

public final class GPUItemDeviceProvider extends AbstractItemDeviceProvider {
    public GPUItemDeviceProvider() {
        super(GPUItem.class);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final GPUItem item = (GPUItem) stack.getItem();
        return Optional.of(new GPUDevice(stack, item.getWidth(), item.getHeight(), item.getTier()));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        final GPUItem item = (GPUItem) query.getItemStack().getItem();
        return switch (item.getTier()) {
            case 1 -> Config.gpuEnergyPerTickTier1;
            case 2 -> Config.gpuEnergyPerTickTier2;
            case 3 -> Config.gpuEnergyPerTickTier3;
            case 4 -> Config.gpuEnergyPerTickTier4;
            default -> 0;
        };
    }
}