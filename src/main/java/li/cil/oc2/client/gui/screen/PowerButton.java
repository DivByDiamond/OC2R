
package li.cil.oc2.client.gui.screen;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class PowerButton extends ToggleImageButton {
    private final AbstractMachineTerminalContainer menu;

    PowerButton(final int x, final int y, final AbstractMachineTerminalContainer menu) {
        super(x, y, 12, 12, Sprites.POWER_BUTTON_BASE, Sprites.POWER_BUTTON_PRESSED, Sprites.POWER_BUTTON_ACTIVE);
        this.menu = menu;
        withTooltip(
            Component.translatable(Constants.COMPUTER_SCREEN_POWER_CAPTION),
            Component.translatable(Constants.COMPUTER_SCREEN_POWER_DESCRIPTION)
        );
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void onPress() {
        super.onPress();
        menu.sendPowerStateToServer(!menu.getVirtualMachine().isRunning());
    }

    @Override
    public boolean isToggled() {
        return menu.getVirtualMachine().isRunning();
    }
}
