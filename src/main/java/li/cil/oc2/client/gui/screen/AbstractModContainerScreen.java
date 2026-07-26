/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractModContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public AbstractModContainerScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    ///////////////////////////////////////////////////////////////////

    public boolean isMouseOver(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        final int localMouseX = mouseX - leftPos;
        final int localMouseY = mouseY - topPos;
        return localMouseX >= x &&
            localMouseX < x + width &&
            localMouseY >= y &&
            localMouseY < y + height;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        super.render(graphics, mouseX, mouseY, partialTicks);

        renderFg(graphics, partialTicks, mouseX, mouseY);

        renderTooltip(graphics, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        for (final Renderable widget : renderables) {
            if (widget instanceof AbstractWidget abstractWidget) {
                if(!abstractWidget.isHovered()) continue;
                if(abstractWidget.getTooltip() == null) continue;
                graphics.renderTooltip(Minecraft.getInstance().font, abstractWidget.getTooltip().toCharSequence(Minecraft.getInstance()), mouseX, mouseY);
            }
        }
    }

    protected void renderFg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
    }
}
