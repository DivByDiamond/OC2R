/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.AbstractMonitorDisplayScreen;
import li.cil.oc2.client.gui.screen.KeyCodeMapping;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.container.AbstractMonitorContainer;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.MonitorInputMessage;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public final class MonitorDisplayWidget {
    private static final int TERMINAL_WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    private static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    private static final int MARGIN_SIZE = 8;
    private static final int TERMINAL_X = MARGIN_SIZE;
    private static final int TERMINAL_Y = MARGIN_SIZE;

    public static final int WIDTH = Sprites.MONITOR_SCREEN.width;
    public static final int HEIGHT = Sprites.MONITOR_SCREEN.height;

    ///////////////////////////////////////////////////////////////////

    private final AbstractMonitorDisplayScreen<?> parent;
    private final AbstractMonitorContainer container;
    private int leftPos, topPos;
    private boolean isMouseOverTerminal;
    private MonitorGUIRenderer.RendererView rendererView;

    ///////////////////////////////////////////////////////////////////

    public MonitorDisplayWidget(final AbstractMonitorDisplayScreen<?> parent) {
        this.parent = parent;
        this.container = this.parent.getMenu();
    }

    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        Sprites.MONITOR_SCREEN.draw(graphics, leftPos, topPos);

        if (shouldCaptureInput()) {
            Sprites.MONITOR_FOCUSED.draw(graphics, leftPos, topPos);
        }
    }

    public void render(final GuiGraphics graphics, @Nullable final Component error) {
        if (container.getPowerState() && container.isMounted() && container.hasPower()) {
            final PoseStack terminalStack = new PoseStack();
            terminalStack.translate(leftPos + TERMINAL_X, topPos + TERMINAL_Y, 0);
            terminalStack.scale((Sprites.MONITOR_SCREEN.width - 16f) / MonitorDevice.WIDTH, (Sprites.MONITOR_SCREEN.height - 16f) / MonitorDevice.HEIGHT, 1f);

            if (rendererView == null) {
                rendererView = container.getMonitor().getMonitor().getRenderer(container.getMonitor());
            }

            final Matrix4f projectionMatrix = (new Matrix4f()).setOrtho(0, parent.width, parent.height, 0, -10f, 10f);
            rendererView.render(terminalStack, projectionMatrix, MonitorDevice.WIDTH, MonitorDevice.HEIGHT, false);
        } else if (container.getPowerState()) {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (Sprites.MONITOR_SCREEN.width - textWidth) / 2;
                final int textOffsetY = (Sprites.MONITOR_SCREEN.height - font.lineHeight) / 2;
                drawShadow(
                    font,
                    graphics,
                    error,
                    leftPos + textOffsetX,
                    topPos + textOffsetY,
                    0xEE3322
                );
            }
        }
    }

    private void drawShadow(Font font, GuiGraphics graphics, Component text, float x, float y, int color) {
        var batch = graphics.bufferSource();
        font.drawInBatch(text, x, y, color, true, graphics.pose().last().pose(), batch, Font.DisplayMode.NORMAL, 0, 15728880);
        batch.endBatch();
    }

    public void tick() {

    }

    public void init() {
        this.leftPos = (parent.width - WIDTH) / 2;
        this.topPos = (parent.height - HEIGHT) / 2;
    }

    public void onClose() {
        if (rendererView != null) {
            rendererView = null;
        }
    }

    public boolean keyPressed(final int keycode, final int scancode, final int modifiers) {
        if (keycode == GLFW.GLFW_KEY_ESCAPE && !shouldCaptureInput())
        {
            return false;
        }
        sendInputMessage(keycode, true);
        return true;
    }

    public boolean keyReleased(final int keycode, final int scancode, final int modifiers) {
        if (keycode == GLFW.GLFW_KEY_ESCAPE && !shouldCaptureInput())
        {
            return false;
        }
        sendInputMessage(keycode, false);
        return true;
    }

    private void sendInputMessage(final int keycode, final boolean isDown) {
        if (KeyCodeMapping.MAPPING.containsKey(keycode)) {
            final int evdevCode = KeyCodeMapping.MAPPING.get(keycode);
            Network.sendToServer(new MonitorInputMessage(container.getMonitor(), evdevCode, isDown));
        }
    }

    ///////////////////////////////////////////////////////////////////

    private Minecraft getClient() {
        return parent.getMinecraft();
    }

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal && container.getCaptureInputState();
    }

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        return parent.isMouseOver(mouseX, mouseY,
            MonitorDisplayWidget.TERMINAL_X, MonitorDisplayWidget.TERMINAL_Y,
            MonitorDisplayWidget.TERMINAL_WIDTH, MonitorDisplayWidget.TERMINAL_HEIGHT);
    }
}
