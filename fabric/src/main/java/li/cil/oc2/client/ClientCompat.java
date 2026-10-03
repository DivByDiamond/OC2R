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
        graphics.renderTooltip(
                font, Language.getInstance().getVisualOrder(new ArrayList<FormattedText>(tooltip)), x, y);
    }

    public static boolean isActiveAndMatches(
            final KeyMapping mapping, final int keyCode, final int scanCode) {
        return mapping.matches(keyCode, scanCode);
    }

    // Vanilla render targets have no stencil buffer.
    public static boolean isStencilEnabled(final RenderTarget target) {
        return false;
    }

    public static void enableStencil(final RenderTarget target) {
        // Nothing to enable.
    }
}
