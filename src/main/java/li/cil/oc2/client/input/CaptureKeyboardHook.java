package li.cil.oc2.client.input;

import li.cil.oc2.client.RenderCompat;
import li.cil.oc2.client.gui.screen.keyboard.KeyboardScreen;
import net.minecraft.client.Minecraft;

/** Captures guest keys before vanilla host shortcuts, without changing ordinary game input. */
public final class CaptureKeyboardHook {
    private CaptureKeyboardHook() {}

    public static boolean handle(final long window, final int key, final int scan, final int action) {
        final Minecraft client = Minecraft.getInstance();
        //? if >=26.1 {
        /*final long currentWindow = client.getWindow().handle();
        *///?} else {
        final long currentWindow = client.getWindow().getWindow();
        //?}
        if (window != currentWindow || !(RenderCompat.currentScreen() instanceof KeyboardScreen screen)) return false;
        screen.handleHostKey(key, scan, action);
        return true;
    }
}
