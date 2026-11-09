package li.cil.oc2.client.input;

import li.cil.oc2.client.RenderCompat;
import li.cil.oc2.client.gui.screen.keyboard.KeyboardScreen;
import li.cil.oc2.client.gui.screen.tablet.TabletTerminalScreen;
import net.minecraft.client.Minecraft;

/** Captures guest keys before vanilla host shortcuts, without changing ordinary game input. */
public final class CaptureKeyboardHook {
    private CaptureKeyboardHook() {}

    public static boolean handle(
            final long window, final int key, final int scan, final int action, final int modifiers) {
        final Minecraft client = Minecraft.getInstance();
        //? if >=26.1 {
        /*final long currentWindow = client.getWindow().handle();
        *///?} else {
        final long currentWindow = client.getWindow().getWindow();
        //?}
        if (window != currentWindow) return false;
        if (RenderCompat.currentScreen() instanceof final KeyboardScreen screen) {
            screen.handleHostKey(key, scan, action);
            return true;
        }
        return RenderCompat.currentScreen() instanceof final TabletTerminalScreen screen
                && screen.handleHostKey(key, scan, action, modifiers);
    }
}
