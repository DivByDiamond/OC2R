package li.cil.oc2.client;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.2 {
/*import net.minecraft.util.LightCoordsUtil;
*///?} else {
import net.minecraft.client.renderer.LevelRenderer;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Access to the current screen, the main camera and block light coordinates, which moved off {@link
 * Minecraft}, the game renderer and the level renderer in 26.2.
 */
@OnlyIn(Dist.CLIENT)
public final class RenderCompat {
    private RenderCompat() {}

    @Nullable
    public static Screen currentScreen() {
        //? if >=26.2 {
/*        return Minecraft.getInstance().gui.screen();
*///?} else {
        return Minecraft.getInstance().screen;
        //?}
    }

    public static void setScreen(@Nullable final Screen screen) {
        //? if >=26.2 {
/*        Minecraft.getInstance().gui.setScreen(screen);
*///?} else {
        Minecraft.getInstance().setScreen(screen);
        //?}
    }

    public static Camera mainCamera() {
        //? if >=26.2 {
/*        return Minecraft.getInstance().gameRenderer.mainCamera();
*///?} else {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
        //?}
    }

    public static int lightCoords(final Level level, final BlockPos pos) {
        //? if >=26.2 {
/*        return LightCoordsUtil.getLightCoords(level, pos);
*///?} else if >=26.1 {
/*        return LevelRenderer.getLightCoords(level, pos);
*///?} else {
        return LevelRenderer.getLightColor(level, pos);
        //?}
    }
}
