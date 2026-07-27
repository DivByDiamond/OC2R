package li.cil.oc2.client.gui.screen.monitor;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMonitorContainer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class MonitorInputCaptureButton extends ToggleImageButton {
    private final AbstractMonitorContainer menu;

    MonitorInputCaptureButton(final int x, final int y, final AbstractMonitorContainer menu) {
        super(
                x,
                y,
                12,
                12,
                Sprites.INPUT_BUTTON_BASE,
                Sprites.INPUT_BUTTON_PRESSED,
                Sprites.INPUT_BUTTON_ACTIVE);
        this.menu = menu;
        withTooltip(
                Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_CAPTION),
                Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_DESCRIPTION));
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {}

    @Override
    public void onPress() {
        super.onPress();
        menu.toggleCaptureInputState();
    }

    @Override
    public boolean isToggled() {
        return menu.getCaptureInputState();
    }
}