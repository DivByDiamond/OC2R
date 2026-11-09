package li.cil.oc2.client.gui.screen.tablet;

import li.cil.oc2.client.gui.screen.common.machine.AbstractMachineTerminalScreen;
import li.cil.oc2.client.input.CaptureKeyBinding;
import li.cil.oc2.common.container.tablet.TabletTerminalContainer;
import net.minecraft.client.gui.components.EditBox;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

/** Terminal and keyboard capture of a tablet; Right Ctrl (the shared binding) releases the keyboard. */
@OnlyIn(Dist.CLIENT)
public final class TabletTerminalScreen
        extends AbstractMachineTerminalScreen<TabletTerminalContainer> {
    public TabletTerminalScreen(
            final TabletTerminalContainer container,
            final Inventory inventory,
            final Component title) {
        super(container, inventory, title);
    }

    /** Routes native keys before vanilla fullscreen and debug shortcuts consume them. */
    public boolean handleHostKey(final int key, final int scan, final int action, final int modifiers) {
        if (!getMenu().getCaptureInputState()) return false;
        if (action != org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
            //? if >=26.1 {
            /*keyPressed(new KeyEvent(key, scan, modifiers));
            *///?} else {
            keyPressed(key, scan, modifiers);
            //?}
        }
        return true;
    }

    private boolean releaseKeyboard(final int key, final int scan) {
        if (!CaptureKeyBinding.matches(key, scan)) return false;
        onClose();
        return true;
    }

    private boolean releaseMouse(final int button) {
        if (!CaptureKeyBinding.matchesMouse(button)) return false;
        onClose();
        return true;
    }

    //? if >=26.1 {
/*    @Override
    public boolean keyPressed(final KeyEvent event) {
        return releaseKeyboard(event.key(), event.scancode()) || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return releaseMouse(event.button()) || super.mouseClicked(event, doubleClick);
    }
*///?} else {
    @Override
    public boolean keyPressed(final int key, final int scan, final int modifiers) {
        return releaseKeyboard(key, scan) || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        return releaseMouse(button) || super.mouseClicked(x, y, button);
    }
    //?}

    @Override
    protected void setFocusIndicatorEditBox(final EditBox editBox) {
        // The shared screen only needs the box to exist; this screen has nothing to keep.
    }
}
