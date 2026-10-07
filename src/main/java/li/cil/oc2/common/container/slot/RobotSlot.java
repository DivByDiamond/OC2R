package li.cil.oc2.common.container.slot;

import li.cil.oc2.platform.ItemHandler;
import li.cil.oc2.platform.ItemHandlerSlot;
import li.cil.oc2.platform.Platform;
import net.minecraft.world.item.ItemStack;

public final class RobotSlot extends ItemHandlerSlot {
    public RobotSlot(
            final ItemHandler itemHandler,
            final int index,
            final int xPosition,
            final int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return super.mayPlace(stack) && Platform.hooks().canFitInsideContainerItems(stack);
    }
}