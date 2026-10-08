package li.cil.oc2.common.container.slot;

import com.mojang.datafixers.util.Pair;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.platform.ItemHandler;
import li.cil.oc2.platform.ItemHandlerSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class DeviceTypeSlotItemHandler extends ItemHandlerSlot {
    private final DeviceType deviceType;

    public DeviceTypeSlotItemHandler(
            final ItemHandler itemHandler,
            final DeviceType deviceType,
            final int index,
            final int xPosition,
            final int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.deviceType = deviceType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    //? if >=26.1 {
    /*@Nullable
    @Override
    public ResourceLocation getNoItemIcon() {
        if (hasItem()) {
            return super.getNoItemIcon();
        } else {
            return deviceType.getBackgroundIcon();
        }
    }
    *///?} else {
    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        if (hasItem()) {
            return super.getNoItemIcon();
        } else {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, deviceType.getBackgroundIcon());
        }
    }
    //?}
}