package li.cil.oc2.common.blockentity.computer.handler;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.util.ChunkUtils;
import li.cil.oc2.common.vm.AbstractVMItemStackHandlers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class ComputerItemStackHandlers extends AbstractVMItemStackHandlers {
    private final ComputerBlockEntity owner;

    public ComputerItemStackHandlers(final ComputerBlockEntity owner, final Supplier<HolderLookup.Provider> providerSupplier) {
        super(providerSupplier,
            new GroupDefinition(DeviceTypes.MEMORY, ComputerBlockEntity.MEMORY_SLOTS),
            new GroupDefinition(DeviceTypes.HARD_DRIVE, ComputerBlockEntity.HARD_DRIVE_SLOTS),
            new GroupDefinition(DeviceTypes.FLASH_MEMORY, ComputerBlockEntity.FLASH_MEMORY_SLOTS),
            new GroupDefinition(DeviceTypes.CARD, ComputerBlockEntity.CARD_SLOTS),
            new GroupDefinition(DeviceTypes.CPU, ComputerBlockEntity.CPU_SLOTS));
        this.owner = owner;
    }

    @Override
    protected ItemDeviceQuery makeQuery(final ItemStack stack) {
        return Devices.makeQuery(owner, stack);
    }

    @Override
    protected void onChanged() {
        super.onChanged();

        final Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            owner.virtualMachine.busController.scheduleBusScan();
            ChunkUtils.setLazyUnsaved(level, owner.getBlockPos());
        }
        owner.isNeighborUpdateScheduled = true;
    }
}
