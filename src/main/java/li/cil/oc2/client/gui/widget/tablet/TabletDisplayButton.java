package li.cil.oc2.client.gui.widget.tablet;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.tablet.TabletTerminalScreen;
import li.cil.oc2.client.gui.widget.misc.ToggleImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Sidebar switch between the guest's graphical display (lit) and its text console. */
public final class TabletDisplayButton extends ToggleImageButton {
    private final TabletTerminalScreen screen;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The button belongs to that screen and switches its view.")
    public TabletDisplayButton(final int x, final int y, final TabletTerminalScreen screen) {
        super(x, y, 12, 12,
                Sprites.DISPLAY_BUTTON_BASE, Sprites.DISPLAY_BUTTON_PRESSED, Sprites.DISPLAY_BUTTON_ACTIVE);
        this.screen = screen;
        withTooltip(Component.translatable("gui.oc2r.tablet.display.capt"),
                Component.translatable("gui.oc2r.tablet.display.desc"));
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}

    @Override
    public void onPress() {
        super.onPress();
        screen.showGraphics(!screen.isGraphical());
    }

    @Override
    public boolean isToggled() {
        return screen.isGraphical();
    }
}
