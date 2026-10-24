package li.cil.oc2.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
//? if <26.1 {
import com.mojang.blaze3d.platform.InputConstants;
//?}
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Client API that NeoForge patches into vanilla classes and that the shared client code needs. The
 * Fabric module has a class with the same name and API (the NeoForge one is excluded from the Fabric
 * compile), built on vanilla fields and access wideners.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCompat {
    private ClientCompat() {}

    //? if >=26.1 {
    /*// The getGuiLeft/getGuiTop/getSlotUnderMouse shims are deprecated for removal on 26.x; the
    // patch that deprecated them points at these vanilla replacements.
    public static int guiLeft(final AbstractContainerScreen<?> screen) {
        return screen.getLeftPos();
    }

    public static int guiTop(final AbstractContainerScreen<?> screen) {
        return screen.getTopPos();
    }

    @Nullable
    public static Slot slotUnderMouse(final AbstractContainerScreen<?> screen) {
        return screen.getHoveredSlot();
    }
    *///?} else {
    public static int guiLeft(final AbstractContainerScreen<?> screen) {
        return screen.getGuiLeft();
    }

    public static int guiTop(final AbstractContainerScreen<?> screen) {
        return screen.getGuiTop();
    }

    @Nullable
    public static Slot slotUnderMouse(final AbstractContainerScreen<?> screen) {
        return screen.getSlotUnderMouse();
    }
    //?}

    public static Font tooltipFont(final ItemStack stack, final Font font) {
        return ClientHooks.getTooltipFont(stack, font);
    }

    public static void renderTooltip(
            final GuiGraphics graphics,
            final Font font,
            final List<? extends FormattedText> tooltip,
            final int x,
            final int y,
            final ItemStack stack) {
        //? if >=26.1 {
/*        graphics.setComponentTooltipForNextFrame(font, tooltip, x, y, stack);
*///?} else {
        graphics.renderComponentTooltip(font, tooltip, x, y, stack);
        //?}
    }

    //? if >=26.1 {
/*    @Nullable
    public static net.minecraft.client.gui.navigation.ScreenRectangle peekScissorStack(
            final GuiGraphics graphics) {
        return graphics.peekScissorStack();
    }
*///?}

    //? if >=26.1 {
/*    public static void submitGuiElement(final GuiGraphics graphics,
            final net.minecraft.client.renderer.state.gui.GuiElementRenderState state) {
        graphics.submitGuiElementRenderState(state);
    }

    public static void submitPictureInPicture(final GuiGraphics graphics,
            final net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState state) {
        graphics.submitPictureInPictureRenderState(state);
    }

    public static <E extends net.minecraft.world.level.block.entity.BlockEntity,
            S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
            net.minecraft.world.phys.AABB blockEntityRenderBounds(
            final net.minecraft.client.renderer.blockentity.BlockEntityRenderer<E, S> renderer, final E entity) {
        return renderer.getRenderBoundingBox(entity);
    }
*///?}

    public static boolean isActiveAndMatches(
            final KeyMapping mapping, final int keyCode, final int scanCode) {
        //? if >=26.1 {
/*        return mapping.matches(new KeyEvent(keyCode, scanCode, 0));
*///?} else {
        return mapping.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
        //?}
    }

    public static boolean isStencilEnabled(final RenderTarget target) {
        //? if >=26.1 {
/*        // 26.x render targets carry no stencil toggle.
        return false;
*///?} else {
        return target.isStencilEnabled();
        //?}
    }

    public static void enableStencil(final RenderTarget target) {
        //? if >=26.1 {
/*        // 26.x render targets carry no stencil toggle.
*///?} else {
        target.enableStencil();
        //?}
    }
}
