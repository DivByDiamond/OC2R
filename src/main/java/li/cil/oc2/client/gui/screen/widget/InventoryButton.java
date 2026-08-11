package li.cil.oc2.client.gui.screen.widget;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.widget.misc.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.base.AbstractMachineTerminalContainer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class InventoryButton extends ImageButton {
    private final AbstractMachineTerminalContainer menu;

    public InventoryButton(final int x, final int y, final AbstractMachineTerminalContainer menu) {
        super(x, y, 12, 12, Sprites.INVENTORY_BUTTON_INACTIVE, Sprites.INVENTORY_BUTTON_ACTIVE);
        this.menu = menu;
        withTooltip(Component.translatable(Constants.MACHINE_OPEN_INVENTORY_CAPTION));
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {}

    @Override
    public void onPress() {
        menu.switchToInventory();
    }
}