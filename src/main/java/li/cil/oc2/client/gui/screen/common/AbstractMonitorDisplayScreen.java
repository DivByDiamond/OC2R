package li.cil.oc2.client.gui.screen.common;

import static java.util.Arrays.asList;
import static li.cil.oc2.common.util.text.TextFormatUtils.withFormat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.monitor.MonitorInputCaptureButton;
import li.cil.oc2.client.gui.screen.monitor.MonitorPowerButton;
import li.cil.oc2.client.gui.widget.misc.MonitorDisplayWidget;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.monitor.AbstractMonitorContainer;
import li.cil.oc2.common.util.text.TooltipRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMonitorDisplayScreen<T extends AbstractMonitorContainer>
        extends AbstractModContainerScreen<T> {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.MONITOR_SIDEBAR_1.height + 4;

    private final MonitorDisplayWidget monitorDisplayWidget;

    protected AbstractMonitorDisplayScreen(
            final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
        this.monitorDisplayWidget = new MonitorDisplayWidget(this);
        imageWidth = Sprites.MONITOR_SCREEN.width;
        imageHeight = Sprites.MONITOR_SCREEN.height;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        monitorDisplayWidget.tick();
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (monitorDisplayWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        final InputConstants.Key input = InputConstants.getKey(keyCode, scanCode);
        if (getMinecraft().options.keyInventory.isActiveAndMatches(input)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        if (monitorDisplayWidget.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }

        final InputConstants.Key input = InputConstants.getKey(keyCode, scanCode);
        if (getMinecraft().options.keyInventory.isActiveAndMatches(input)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void init() {
        super.init();
        monitorDisplayWidget.init();

        final EditBox focusIndicatorEditBox = new EditBox(font, 0, 0, 0, 0, Component.empty());
        focusIndicatorEditBox.setFocused(true);
        setFocusIndicatorEditBox(focusIndicatorEditBox);

        addRenderableWidget(
                new MonitorPowerButton(
                        leftPos - Sprites.MONITOR_SIDEBAR_1.width + 4,
                        topPos + CONTROLS_TOP + 4,
                        menu));
        addRenderableWidget(
                new MonitorInputCaptureButton(
                        leftPos - Sprites.MONITOR_SIDEBAR_1.width + 4,
                        topPos + CONTROLS_TOP + 4 + 14,
                        menu));
    }

    @Override
    public void onClose() {
        super.onClose();
        monitorDisplayWidget.onClose();
    }

    // We use this text box to indicate to Forge that we want all input, and event handlers should
    // not be allowed
    // to steal input from us (e.g. via custom key bindings). Since Forge is lazy and just uses
    // getDeclaredFields
    // to get private fields, which completely skips fields in base classes, we require subclasses
    // to hold the field...
    protected abstract void setFocusIndicatorEditBox(final EditBox editBox);

    @Override
    protected void renderFg(
            final GuiGraphics graphics,
            final float partialTicks,
            final int mouseX,
            final int mouseY) {
        super.renderFg(graphics, partialTicks, mouseX, mouseY);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width + 4;
            final int y = topPos + ENERGY_TOP + 4;
            Sprites.ENERGY_BAR.drawFillY(
                    graphics, x, y, menu.getEnergy() / (float) menu.getEnergyCapacity());
        }

        monitorDisplayWidget.render(
                graphics, Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY));
    }

    @Override
    protected void renderBg(
            final GuiGraphics graphics,
            final float partialTicks,
            final int mouseX,
            final int mouseY) {
        Sprites.MONITOR_SIDEBAR_1.draw(
                graphics, leftPos - Sprites.MONITOR_SIDEBAR_1.width, topPos + CONTROLS_TOP);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width;
            final int y = topPos + ENERGY_TOP;
            Sprites.SIDEBAR_2.draw(graphics, x, y);
            Sprites.ENERGY_BASE.draw(graphics, x + 4, y + 4);
        }

        monitorDisplayWidget.renderBackground(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (shouldRenderEnergyBar()) {

            if (isMouseOver(
                    mouseX,
                    mouseY,
                    -Sprites.SIDEBAR_2.width + 4,
                    ENERGY_TOP + 4,
                    Sprites.ENERGY_BAR.width,
                    Sprites.ENERGY_BAR.height)) {
                final List<? extends FormattedText> tooltip =
                        asList(
                                Component.translatable(
                                        Constants.TOOLTIP_ENERGY,
                                        withFormat(
                                                menu.getEnergy() + "/" + menu.getEnergyCapacity(),
                                                ChatFormatting.GREEN)),
                                Component.translatable(
                                        Constants.TOOLTIP_ENERGY_CONSUMPTION,
                                        withFormat(
                                                String.valueOf(Config.monitorEnergyPerTick),
                                                ChatFormatting.GREEN)));
                TooltipRenderer.drawTooltip(graphics, tooltip, mouseX, mouseY, 200);
            }
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {}

    private boolean shouldRenderEnergyBar() {
        return menu.getEnergyCapacity() > 0;
    }
}