package li.cil.oc2.common.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.List;

public final class TooltipRenderer {
    public static void drawTooltip(
            final GuiGraphics graphics,
            final List<? extends FormattedText> tooltip,
            final int x,
            final int y) {
        drawTooltip(graphics, tooltip, x, y, 200, ItemStack.EMPTY);
    }

    public static void drawTooltip(
            final GuiGraphics graphics,
            final List<? extends FormattedText> tooltip,
            final int x,
            final int y,
            final int widthHint) {
        drawTooltip(graphics, tooltip, x, y, widthHint, ItemStack.EMPTY);
    }

    public static void drawTooltip(
            final GuiGraphics graphics,
            final List<? extends FormattedText> tooltip,
            final int x,
            final int y,
            final int widthHint,
            final ItemStack itemStack) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen screen = minecraft.screen;
        if (screen == null) {
            return;
        }

        final int availableWidth = Math.max(x, screen.width - x);
        final int targetWidth = Math.min(availableWidth, widthHint);
        final Font font = ClientHooks.getTooltipFont(itemStack, minecraft.font);

        final boolean needsWrapping =
                tooltip.stream().anyMatch(line -> font.width(line) > targetWidth);
        if (!needsWrapping) {
            graphics.renderComponentTooltip(font, tooltip, x, y, itemStack);
        } else {
            final StringSplitter splitter = font.getSplitter();
            final List<? extends FormattedText> wrappedTooltip =
                    tooltip.stream()
                            .flatMap(
                                    line ->
                                            splitter
                                                    .splitLines(line, targetWidth, Style.EMPTY)
                                                    .stream())
                            .toList();
            graphics.renderComponentTooltip(font, wrappedTooltip, x, y, itemStack);
        }
    }
}
