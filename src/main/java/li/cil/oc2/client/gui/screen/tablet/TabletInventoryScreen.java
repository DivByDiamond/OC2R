package li.cil.oc2.client.gui.screen.tablet;

import li.cil.oc2.client.gui.screen.common.machine.AbstractMachineInventoryScreen;
import li.cil.oc2.common.container.tablet.TabletInventoryContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

/** Processor and flash slots of a tablet, drawn without a texture. */
@OnlyIn(Dist.CLIENT)
public final class TabletInventoryScreen
        extends AbstractMachineInventoryScreen<TabletInventoryContainer> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 144;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int EDGE = 0xFF373737;

    public TabletInventoryScreen(
            final TabletInventoryContainer container,
            final Inventory playerInventory,
            final Component title) {
        //? if >=26.1 {
        /*super(container, playerInventory, title, WIDTH, HEIGHT);
        *///?} else {
        super(container, playerInventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        //?}
        inventoryLabelY = HEIGHT - 94;
    }

    @Override
    protected void renderBg(
            final GuiGraphics graphics,
            final float partialTicks,
            final int mouseX,
            final int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);

        graphics.fill(leftPos - 1, topPos - 1, leftPos + WIDTH + 1, topPos + HEIGHT + 1, EDGE);
        graphics.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, PANEL);
        for (final Slot slot : menu.slots) {
            final int x = leftPos + slot.x;
            final int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, EDGE);
            graphics.fill(x, y, x + 16, y + 16, SLOT);
        }
    }
}
