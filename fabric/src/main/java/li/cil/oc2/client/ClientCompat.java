package li.cil.oc2.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric side of {@code ClientCompat}: the NeoForge module has a class with the same name and API
 * that uses the APIs NeoForge patches into vanilla; here they are built on vanilla fields (opened up
 * through the access widener).
 */
@Environment(EnvType.CLIENT)
public final class ClientCompat {
    private ClientCompat() {}

    public static int guiLeft(final AbstractContainerScreen<?> screen) {
        return screen.leftPos;
    }

    public static int guiTop(final AbstractContainerScreen<?> screen) {
        return screen.topPos;
    }

    @Nullable
    public static Slot slotUnderMouse(final AbstractContainerScreen<?> screen) {
        return screen.hoveredSlot;
    }

    public static Font tooltipFont(final ItemStack stack, final Font font) {
        return font;
    }

    public static void renderTooltip(
            final GuiGraphics graphics,
            final Font font,
            final List<? extends FormattedText> tooltip,
            final int x,
            final int y,
            final ItemStack stack) {
        //? if >=26.1 {
/*        graphics.setTooltipForNextFrame(
                font, Language.getInstance().getVisualOrder(new ArrayList<>(tooltip)), x, y);
*///?} else {
        graphics.renderTooltip(
                font, Language.getInstance().getVisualOrder(new ArrayList<>(tooltip)), x, y);
        //?}
    }

    //? if >=26.1 {
/*    @Nullable
    public static net.minecraft.client.gui.navigation.ScreenRectangle peekScissorStack(
            final GuiGraphics graphics) {
        return graphics.scissorStack.peek();
    }
*///?}

    //? if >=26.1 {
/*    public static void submitGuiElement(final GuiGraphics graphics,
            final net.minecraft.client.renderer.state.gui.GuiElementRenderState state) {
        graphics.guiRenderState.addGuiElement(state);
    }

    public static void submitPictureInPicture(final GuiGraphics graphics,
            final net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState state) {
        graphics.guiRenderState.addPicturesInPictureState(state);
    }

    public static <E extends net.minecraft.world.level.block.entity.BlockEntity,
            S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
            net.minecraft.world.phys.AABB blockEntityRenderBounds(
            final net.minecraft.client.renderer.blockentity.BlockEntityRenderer<E, S> renderer, final E entity) {
        if (entity instanceof final li.cil.oc2.common.blockentity.energy.ChargerBlockEntity charger) {
            return charger.getRenderBoundingBox();
        }
        if (entity instanceof final li.cil.oc2.common.blockentity.misc.gateway.InternetGateWayBlockEntity gateway) {
            return gateway.getRenderBoundingBox();
        }
        // Vanilla has no renderer bounds hook; a conservative block margin also covers chest lids and signs.
        return new net.minecraft.world.phys.AABB(entity.getBlockPos()).inflate(1);
    }
*///?}

    public static boolean isActiveAndMatches(
            final KeyMapping mapping, final int keyCode, final int scanCode) {
        //? if >=26.1 {
/*        return mapping.matches(new KeyEvent(keyCode, scanCode, 0));
*///?} else {
        return mapping.matches(keyCode, scanCode);
        //?}
    }

    // Vanilla render targets have no stencil buffer.
    public static boolean isStencilEnabled(final RenderTarget target) {
        return false;
    }

    public static void enableStencil(final RenderTarget target) {
        // Nothing to enable.
    }
}
