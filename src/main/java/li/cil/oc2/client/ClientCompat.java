package li.cil.oc2.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Client API that NeoForge patches into vanilla classes and that the shared client code needs. The
 * Fabric module has a class with the same name and API (the NeoForge one is excluded from the Fabric
 * compile), built on vanilla fields and access wideners.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCompat {
    private ClientCompat() {}

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
        graphics.renderComponentTooltip(font, tooltip, x, y, stack);
    }

    public static boolean isActiveAndMatches(
            final KeyMapping mapping, final int keyCode, final int scanCode) {
        return mapping.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }

    public static boolean isStencilEnabled(final RenderTarget target) {
        return target.isStencilEnabled();
    }

    public static void enableStencil(final RenderTarget target) {
        target.enableStencil();
    }
}
