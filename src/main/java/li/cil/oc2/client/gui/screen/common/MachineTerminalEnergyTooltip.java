package li.cil.oc2.client.gui.screen.common;

import static java.util.Arrays.asList;
import static li.cil.oc2.common.util.text.TextFormatUtils.withFormat;

import java.util.List;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.text.TooltipRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

final class MachineTerminalEnergyTooltip {

    static void renderEnergyTooltip(
            final AbstractMachineTerminalScreen<?> screen,
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY) {
        if (screen.getMenu().getEnergyCapacity() <= 0) return;

        if (isMouseOverEnergyArea(screen, mouseX, mouseY)) {
            final List<? extends FormattedText> tooltip =
                    asList(
                            Component.translatable(
                                    Constants.TOOLTIP_ENERGY,
                                    withFormat(
                                            screen.getMenu().getEnergy() + "/" + screen.getMenu().getEnergyCapacity(),
                                            ChatFormatting.GREEN)),
                            Component.translatable(
                                    Constants.TOOLTIP_ENERGY_CONSUMPTION,
                                    withFormat(
                                            String.valueOf(screen.getMenu().getEnergyConsumption()),
                                            ChatFormatting.GREEN)));
            TooltipRenderer.drawTooltip(graphics, tooltip, mouseX, mouseY, 200);
        }
    }

    static boolean isMouseOverEnergyArea(
            final AbstractMachineTerminalScreen<?> screen,
            final int mouseX, final int mouseY) {
        final int leftPos = screen.getGuiLeft();
        final int topPos = screen.getGuiTop();
        final int CONTROLS_TOP = 8;
        final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_3.height + 4;
        return screen.isMouseOver(
                mouseX,
                mouseY,
                -Sprites.SIDEBAR_2.width + 4,
                ENERGY_TOP + 4,
                Sprites.ENERGY_BAR.width,
                Sprites.ENERGY_BAR.height);
    }
}
