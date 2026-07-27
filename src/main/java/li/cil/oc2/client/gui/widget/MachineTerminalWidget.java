package li.cil.oc2.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.AbstractMachineTerminalScreen;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.vm.terminal.RendererView;
import li.cil.oc2.common.vm.terminal.Terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public final class MachineTerminalWidget {
    private static final int MARGIN_SIZE = 8;
    static final int TERMINAL_X = MARGIN_SIZE;
    static final int TERMINAL_Y = MARGIN_SIZE;
    static final int TERMINAL_WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    public static final int WIDTH = Sprites.TERMINAL_SCREEN.width;
    public static final int HEIGHT = Sprites.TERMINAL_SCREEN.height;

    private final AbstractMachineTerminalScreen<?> parent;
    private final AbstractMachineTerminalContainer container;
    private final Terminal terminal;
    private final TerminalMouseHandler mouseHandler;
    private final TerminalKeyboardHandler keyboardHandler;
    private int leftPos, topPos;
    private boolean isMouseOverTerminal;
    private RendererView rendererView;
    private boolean isOver;

    public MachineTerminalWidget(final AbstractMachineTerminalScreen<?> parent) {
        this.parent = parent;
        this.container = this.parent.getMenu();
        this.terminal = this.container.getTerminal();
        this.mouseHandler = new TerminalMouseHandler(terminal);
        this.keyboardHandler = new TerminalKeyboardHandler(terminal);
    }

    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        Sprites.TERMINAL_SCREEN.draw(graphics, leftPos, topPos);

        if (shouldCaptureInput()) {
            Sprites.TERMINAL_FOCUSED.draw(graphics, leftPos, topPos);
        }
    }

    public void render(final GuiGraphics graphics, @Nullable final Component error) {
        if (container.getVirtualMachine().isRunning()) {
            final PoseStack terminalStack = new PoseStack();
            terminalStack.translate(leftPos + TERMINAL_X, topPos + TERMINAL_Y, 0);
            terminalStack.scale(
                    TERMINAL_WIDTH / (float) terminal.getWidth(),
                    TERMINAL_HEIGHT / (float) terminal.getHeight(),
                    1f);

            if (rendererView == null) {
                rendererView = terminal.getRenderer();
            }

            final Matrix4f projectionMatrix =
                    (new Matrix4f()).setOrtho(0, parent.width, parent.height, 0, -10f, 10f);
            rendererView.render(terminalStack, projectionMatrix, false);
        } else {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (TERMINAL_WIDTH - textWidth) / 2;
                final int textOffsetY = (TERMINAL_HEIGHT - font.lineHeight) / 2;
                drawShadow(
                        font,
                        graphics,
                        error,
                        leftPos + TERMINAL_X + textOffsetX,
                        topPos + TERMINAL_Y + textOffsetY);
            }
        }
    }

    private void drawShadow(Font font, GuiGraphics graphics, Component text, float x, float y) {
        var batch = graphics.bufferSource();
        font.drawInBatch(
                text,
                x,
                y,
                15610658,
                true,
                graphics.pose().last().pose(),
                batch,
                Font.DisplayMode.NORMAL,
                0,
                15728880);
        batch.endBatch();
    }

    public void tick() {
        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            container.sendTerminalInputToServer(input);
        }
    }

    public boolean mouseScrolled(double dir) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) return false;
        if (dir < 0) {
            terminal.incrementLastLineToDisplay(true);
        } else {
            terminal.decrementLastLineToDisplay();
        }
        return true;
    }

    public void mouseMoved(double x, double y) {
        if (isMouseOverTerminal((int) x, (int) y)) {
            if (!isOver && terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT) {
                isOver = true;
                terminal.putInput("\033[I");
            }
        } else {
            if (isOver && terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT) {
                terminal.putInput("\033[O");
            }
        }
    }

    public boolean mouseClicked(double x, double y, int button) {
        return mouseHandler.mouseClicked(
                x,
                y,
                button,
                isMouseOverTerminal((int) x, (int) y),
                shouldCaptureInput(),
                leftPos,
                topPos);
    }

    public boolean mouseReleased(double x, double y, int button) {
        return mouseHandler.mouseReleased(
                x,
                y,
                button,
                isMouseOverTerminal((int) x, (int) y),
                shouldCaptureInput(),
                leftPos,
                topPos);
    }

    public boolean charTyped(final char ch, final int modifier) {
        return keyboardHandler.charTyped(ch, modifier);
    }

    @SuppressWarnings("unused")
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        return keyboardHandler.keyPressed(
                keyCode,
                scanCode,
                modifiers,
                shouldCaptureInput(),
                () -> getClient().keyboardHandler.getClipboard());
    }

    public void init() {
        this.leftPos = (parent.width - WIDTH) / 2;
        this.topPos = (parent.height - HEIGHT) / 2;
    }

    public void onClose() {
        if (rendererView != null) {
            terminal.releaseRenderer(rendererView);
            rendererView = null;
        }
    }

    private Minecraft getClient() {
        return parent.getMinecraft();
    }

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal
                && container.getCaptureInputState()
                && container.getVirtualMachine().isRunning();
    }

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        return parent.isMouseOver(
                mouseX, mouseY, TERMINAL_X, TERMINAL_Y, TERMINAL_WIDTH, TERMINAL_HEIGHT);
    }
}
