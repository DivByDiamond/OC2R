package li.cil.oc2.client.gui.widget.terminal;

import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.common.machine.AbstractMachineTerminalScreen;
import li.cil.oc2.common.container.base.AbstractMachineTerminalContainer;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import li.cil.oc2.common.vm.terminal.render.TerminalRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class MachineTerminalWidget {
    private static final int MARGIN_SIZE = 8;
    static final int TERMINAL_X = MARGIN_SIZE;
    static final int TERMINAL_Y = MARGIN_SIZE;
    static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2; // base height; dynamic height scales within the fixed widget

    public static final int WIDTH = Sprites.TERMINAL_SCREEN.width;
    public static final int HEIGHT = Sprites.TERMINAL_SCREEN.height;

    // The terminal's inner pixel width inside the GUI sprite (sprite width minus both side
    // margins). Pixel width, not columns — distinct from Terminal.getWidth()/getTerminalWidth().
    static int getInnerWidth() {
        return Sprites.TERMINAL_SCREEN.width - 2 * MARGIN_SIZE;
    }

    private final AbstractMachineTerminalScreen<?> parent;
    private final AbstractMachineTerminalContainer container;
    private final Terminal terminal;
    private final TerminalMouseHandler mouseHandler;
    private final TerminalKeyboardHandler keyboardHandler;
    private int leftPos;
    private int topPos;
    private boolean mouseOverTerminal;
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
        mouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        Sprites.TERMINAL_SCREEN.draw(graphics, leftPos, topPos);

        if (shouldCaptureInput()) {
            Sprites.TERMINAL_FOCUSED.draw(graphics, leftPos, topPos);
        }
    }

    public void render(final GuiGraphics graphics, @Nullable final Component error) {
        if (container.getVirtualMachine().isRunning()) {
            //? if >=26.1 {
            /*if (rendererView == null) {
                rendererView = terminal.getRenderer();
            }
            if (rendererView instanceof final TerminalRenderer renderer) {
                final TerminalRenderer.GuiSnapshot snapshot = renderer.captureForGui();
                if (snapshot != null) {
                    // Same fit as the 1.21 path: scale the terminal pixels to the sprite's inner area.
                    final Matrix3x2f pose = new Matrix3x2f(graphics.pose())
                            .translate(leftPos + TERMINAL_X, topPos + TERMINAL_Y)
                            .scale(
                                    getInnerWidth() / (float) terminal.getWidth(),
                                    TERMINAL_HEIGHT / (float) terminal.getHeight());
                    final ScreenRectangle scissor = graphics.peekScissorStack();
                    final ScreenRectangle area = new ScreenRectangle(
                                    leftPos + TERMINAL_X,
                                    topPos + TERMINAL_Y,
                                    getInnerWidth(),
                                    TERMINAL_HEIGHT)
                            .transformMaxBounds(graphics.pose());
                    final ScreenRectangle bounds = scissor == null ? area : area.intersection(scissor);

                    if (snapshot.vertexCount() > 0) {
                        graphics.submitGuiElementRenderState(new TerminalGuiElement.Text(
                                snapshot, pose, TerminalGuiElement.fontTexture(), bounds, scissor));
                    }
                    if (snapshot.cursorVisible()) {
                        graphics.submitGuiElementRenderState(
                                new TerminalGuiElement.Cursor(terminal, snapshot, pose, bounds, scissor));
                    }
                }
            }
*///?} else {
            final PoseStack terminalStack = new PoseStack();
            terminalStack.translate(leftPos + TERMINAL_X, topPos + TERMINAL_Y, 0);
            // Scale terminal rendering to fit inside the sprite's inner area. Terminal.getWidth()
            // returns the terminal's pixel width (columns * CHAR_WIDTH); getInnerWidth() is the
            // sprite's inner pixel area. Scale = inner / terminal so the terminal fills the area.
            terminalStack.scale(
                    getInnerWidth() / (float) terminal.getWidth(),
                    TERMINAL_HEIGHT / (float) terminal.getHeight(),
                    1f);

            if (rendererView == null) {
                rendererView = terminal.getRenderer();
            }

            final Matrix4f projectionMatrix =
                    new Matrix4f().setOrtho(0, parent.width, parent.height, 0, -10f, 10f);
            rendererView.render(terminalStack, projectionMatrix, false);
            //?}
        } else {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (getInnerWidth() - textWidth) / 2;
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
        //? if >=26.1 {
/*        graphics.text(font, text, (int) x, (int) y, 0xFF000000 | 15610658, true);
*///?} else {
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
        //?}
    }

    public void tick() {
        final ByteBuffer input = terminal.io.getInput();
        if (input != null) {
            container.sendTerminalInputToServer(input);
        }
    }

    public boolean mouseScrolled(double dir) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) return false;
        if (dir < 0) {
            terminal.bufferManager.incrementLastLineToDisplay(true);
        } else {
            terminal.bufferManager.decrementLastLineToDisplay();
        }
        return true;
    }

    public void mouseMoved(double x, double y) {
        if (isMouseOverTerminal((int) x, (int) y)) {
            if (!isOver && terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT) {
                isOver = true;
                terminal.io.putInput("\033[I");
            }
        } else {
            if (isOver && terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT) {
                terminal.io.putInput("\033[O");
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
                getClient().keyboardHandler::getClipboard);
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
        return Minecraft.getInstance();
    }

    private boolean shouldCaptureInput() {
        return mouseOverTerminal
                && container.getCaptureInputState()
                && container.getVirtualMachine().isRunning();
    }

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        return parent.isMouseOver(
                mouseX, mouseY, TERMINAL_X, TERMINAL_Y, getInnerWidth(), TERMINAL_HEIGHT);
    }
}