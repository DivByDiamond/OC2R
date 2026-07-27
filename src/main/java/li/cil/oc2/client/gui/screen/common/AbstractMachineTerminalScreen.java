package li.cil.oc2.client.gui.screen.common;

import static java.util.Arrays.asList;
import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.widget.MachineTerminalWidget;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.util.text.TooltipRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import li.cil.oc2.client.gui.screen.widget.InputCaptureButton;
import li.cil.oc2.client.gui.screen.widget.InventoryButton;
import li.cil.oc2.client.gui.screen.widget.PowerButton;
import li.cil.oc2.client.gui.screen.widget.SidebarAreas;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMachineTerminalScreen<T extends AbstractMachineTerminalContainer>
        extends AbstractModContainerScreen<T> {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_3.height + 4;

    private boolean wasMouseClicked;

    private final MachineTerminalWidget terminalWidget;

    protected AbstractMachineTerminalScreen(
            final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
        this.terminalWidget = new MachineTerminalWidget(this);
        imageWidth = Sprites.TERMINAL_SCREEN.width;
        imageHeight = Sprites.TERMINAL_SCREEN.height;
    }

    public List<Rect2i> getExtraAreas() {
        return SidebarAreas.getExtraAreas(leftPos, topPos, shouldRenderEnergyBar());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        terminalWidget.tick();
    }

    @Override
    public boolean charTyped(final char ch, final int modifiers) {
        return terminalWidget.charTyped(ch, modifiers) || super.charTyped(ch, modifiers);
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        wasMouseClicked = true;
        if (!terminalWidget.mouseClicked(x, y, button)) {
            return super.mouseClicked(x, y, button);
        }
        return true;
    }

    @Override
    public void mouseMoved(final double x, final double y) {
        terminalWidget.mouseMoved(x, y);
    }

    @Override
    public boolean mouseScrolled(
            final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        return terminalWidget.mouseScrolled(scrollY);
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        if (!wasMouseClicked) return super.mouseReleased(x, y, button);
        if (!terminalWidget.mouseReleased(x, y, button)) {
            return super.mouseReleased(x, y, button);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (terminalWidget.keyPressed(keyCode, scanCode, modifiers)) {
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
        terminalWidget.init();

        final EditBox focusIndicatorEditBox = new EditBox(font, 0, 0, 0, 0, Component.empty());
        focusIndicatorEditBox.setFocused(true);
        setFocusIndicatorEditBox(focusIndicatorEditBox);

        addRenderableWidget(
                new PowerButton(
                        leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4, menu));
        addRenderableWidget(
                new InputCaptureButton(
                        leftPos - Sprites.SIDEBAR_3.width + 4,
                        topPos + CONTROLS_TOP + 4 + 14,
                        menu));
        addRenderableWidget(
                new InventoryButton(
                        leftPos - Sprites.SIDEBAR_3.width + 4,
                        topPos + CONTROLS_TOP + 4 + 14 + 14,
                        menu));
    }

    @Override
    public void onClose() {
        super.onClose();
        terminalWidget.onClose();
    }

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

        terminalWidget.render(graphics, menu.getVirtualMachine().getError());
    }

    @Override
    protected void renderBg(
            final GuiGraphics graphics,
            final float partialTicks,
            final int mouseX,
            final int mouseY) {
        Sprites.SIDEBAR_3.draw(graphics, leftPos - Sprites.SIDEBAR_3.width, topPos + CONTROLS_TOP);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width;
            final int y = topPos + ENERGY_TOP;
            Sprites.SIDEBAR_2.draw(graphics, x, y);
            Sprites.ENERGY_BASE.draw(graphics, x + 4, y + 4);
        }

        terminalWidget.renderBackground(graphics, mouseX, mouseY);
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
                                                String.valueOf(menu.getEnergyConsumption()),
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