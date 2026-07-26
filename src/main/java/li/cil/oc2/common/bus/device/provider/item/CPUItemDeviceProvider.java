package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.item.CPUItemDevice;
import li.cil.oc2.common.item.CPUItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class CPUItemDeviceProvider extends AbstractItemDeviceProvider {
    public CPUItemDeviceProvider() {
        super(CPUItem.class);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return Optional.of(new CPUItemDevice(query.getItemStack()));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final CPUItem item = (CPUItem) stack.getItem();
        final int freq = Math.max(item.getFrequency(), 0);
        return Math.max(1, (int) Math.round(freq * Config.cpuEnergyPerMegahertzPerTick / 1_000_000));
    }
}
