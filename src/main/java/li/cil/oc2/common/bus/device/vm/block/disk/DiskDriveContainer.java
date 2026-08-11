package li.cil.oc2.common.bus.device.vm.block.disk;

import net.minecraft.world.item.ItemStack;

public interface DiskDriveContainer {
    ItemStack getDiskItemStack();

    void handleDataAccess();
}