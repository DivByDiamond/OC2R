package li.cil.oc2.client.gui.screen.tablet;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.common.machine.AbstractMachineInventoryScreen;
import li.cil.oc2.common.container.tablet.TabletInventoryContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

/** Processor and flash slots of a tablet on the same dark panel as the computer inventory. */
@OnlyIn(Dist.CLIENT)
public final class TabletInventoryScreen
        extends AbstractMachineInventoryScreen<TabletInventoryContainer> {
    public TabletInventoryScreen(
            final TabletInventoryContainer container,
            final Inventory playerInventory,
            final Component title) {
        //? if >=26.1 {
        /*super(container, playerInventory, title, Sprites.TABLET_CONTAINER.width, Sprites.TABLET_CONTAINER.height);
        *///?} else {
        super(container, playerInventory, title);
        imageWidth = Sprites.TABLET_CONTAINER.width;
        imageHeight = Sprites.TABLET_CONTAINER.height;
        //?}
        inventoryLabelY = Sprites.TABLET_CONTAINER.height - 94;
    }

    @Override
    protected void renderBg(
            final GuiGraphics graphics,
            final float partialTicks,
            final int mouseX,
            final int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);

        Sprites.TABLET_CONTAINER.draw(graphics, leftPos, topPos);
    }
}
