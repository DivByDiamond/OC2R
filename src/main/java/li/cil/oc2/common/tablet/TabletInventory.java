package li.cil.oc2.common.tablet;

import java.util.function.Supplier;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.handler.AbstractVMItemStackHandlers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/**
 * Device slots of a tablet: a fixed memory module, one processor and one flash. Memory is part of
 * the craft and has no player facing slot; processor and flash are the only parts players swap.
 */
public final class TabletInventory extends AbstractVMItemStackHandlers {
    private final boolean clientSide;
    private Runnable onDeviceChanged = () -> {};

    /** @param clientSide a client mirror only holds items for the screen and never creates devices */
    public TabletInventory(final Supplier<HolderLookup.Provider> provider, final boolean clientSide) {
        super(
                provider,
                new GroupDefinition(DeviceTypes.MEMORY, 1),
                new GroupDefinition(DeviceTypes.CPU, 1),
                new GroupDefinition(DeviceTypes.FLASH_MEMORY, 1));
        this.clientSide = clientSide;
    }

    public void setOnDeviceChanged(final Runnable listener) {
        onDeviceChanged = listener;
    }

    /** Puts the basic 16 MiB module back if the slot is empty, e.g. after loading old data. */
    public void ensureFixedMemory() {
        getItemHandler(DeviceTypes.MEMORY)
                .ifPresent(
                        memory -> {
                            if (memory.getStackInSlot(0).isEmpty()) {
                                memory.setStackInSlot(0, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
                            }
                        });
    }

    /** Puts the OnyxOS flash into a freshly crafted tablet. */
    public void installDefaultFlash() {
        getItemHandler(DeviceTypes.FLASH_MEMORY)
                .ifPresent(flash -> flash.setStackInSlot(0, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get())));
    }

    @Override
    protected ItemDeviceQuery makeQuery(final ItemStack stack) {
        return Devices.makeQuery(stack);
    }

    @Override
    protected boolean isClientSide() {
        return clientSide;
    }

    @Override
    protected void onChanged() {
        super.onChanged();
        onDeviceChanged.run();
    }
}
