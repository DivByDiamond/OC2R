package li.cil.oc2.client.input;

import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
/*import li.cil.oc2.api.API;
*///?}
import net.minecraft.client.KeyMapping;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.ResourceLocation;
*///?}
import org.lwjgl.glfw.GLFW;

/** The host shortcut that releases input captured by an emulated computer. */
public final class CaptureKeyBinding {
    public static final KeyMapping RELEASE = new KeyMapping(
            "key.oc2r.release_input",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_CONTROL,
            //? if >=26.1 {
            /*KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "input")));
            *///?} else {
            "key.categories.oc2r");
            //?}

    private CaptureKeyBinding() {}

    public static boolean matches(final int keyCode, final int scanCode) {
        //? if >=26.1 {
        /*return !RELEASE.isUnbound() && RELEASE.matches(new KeyEvent(keyCode, scanCode, 0));
        *///?} else {
        return !RELEASE.isUnbound() && RELEASE.matches(keyCode, scanCode);
        //?}
    }

    public static boolean matchesMouse(final int button) {
        //? if >=26.1 {
        /*return RELEASE.matchesMouse(new MouseButtonEvent(0, 0, new MouseButtonInfo(button, 0)));
        *///?} else {
        return RELEASE.matchesMouse(button);
        //?}
    }
}
