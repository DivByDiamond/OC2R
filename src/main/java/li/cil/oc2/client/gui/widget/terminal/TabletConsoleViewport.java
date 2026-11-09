package li.cil.oc2.client.gui.widget.terminal;

//? if <26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import java.nio.ByteBuffer;
import li.cil.oc2.common.container.tablet.TabletTerminalContainer;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >=26.1 {
/*import li.cil.oc2.common.vm.terminal.render.TerminalRenderer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
*///?} else {
import org.joml.Matrix4f;
//?}

/** The regular UART terminal fitted into a tablet panel, with the original keyboard translator. */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "EI_EXPOSE_REP2", justification = "The viewport reads capture and terminal state from its owning menu.")
public final class TabletConsoleViewport {
    private final Terminal terminal;
    private final TabletTerminalContainer menu;
    private final TerminalKeyboardHandler keyboard;
    private RendererView renderer;

    public TabletConsoleViewport(final TabletTerminalContainer menu) {
        this.menu = menu;
        terminal = menu.getTerminal();
        keyboard = new TerminalKeyboardHandler(terminal);
    }

    public void render(final GuiGraphics graphics, final int x, final int y, final int width,
            final int height, final int screenWidth, final int screenHeight) {
        if (renderer == null) renderer = terminal.getRenderer();
        //? if >=26.1 {
        /*if (!(renderer instanceof final TerminalRenderer modern)) return;
        final TerminalRenderer.GuiSnapshot snapshot = modern.captureForGui();
        if (snapshot == null) return;
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose()).translate(x, y)
                .scale(width / (float) terminal.getWidth(), height / (float) terminal.getHeight());
        final ScreenRectangle area = new ScreenRectangle(x, y, width, height).transformMaxBounds(graphics.pose());
        final ScreenRectangle scissor = li.cil.oc2.client.ClientCompat.peekScissorStack(graphics);
        final ScreenRectangle bounds = scissor == null ? area : area.intersection(scissor);
        if (bounds == null) return;
        if (snapshot.backgroundVertexCount() > 0) li.cil.oc2.client.ClientCompat.submitGuiElement(graphics,
                new TerminalGuiElement.Background(snapshot, pose, bounds, bounds));
        if (snapshot.textVertexCount() > 0) li.cil.oc2.client.ClientCompat.submitGuiElement(graphics,
                new TerminalGuiElement.Text(snapshot, pose, TerminalGuiElement.fontTexture(), bounds, bounds));
        if (snapshot.cursorVisible()) li.cil.oc2.client.ClientCompat.submitGuiElement(graphics,
                new TerminalGuiElement.Cursor(snapshot, pose, bounds, bounds));
        *///?} else {
        final PoseStack pose = new PoseStack();
        pose.translate(x, y, 0);
        pose.scale(width / (float) terminal.getWidth(), height / (float) terminal.getHeight(), 1);
        renderer.render(pose, new Matrix4f().setOrtho(0, screenWidth, screenHeight, 0, -10, 10), false);
        //?}
    }

    public boolean key(final int key, final int scan, final int modifiers) {
        return keyboard.keyPressed(key, scan, modifiers, menu.getCaptureInputState(),
                Minecraft.getInstance().keyboardHandler::getClipboard);
    }

    public boolean character(final char character, final int modifiers) {
        return keyboard.charTyped(character, modifiers);
    }

    public void tick() {
        final ByteBuffer bytes = terminal.io.getInput();
        if (bytes != null) menu.sendTerminalInputToServer(bytes);
    }

    public void close() {
        if (renderer != null) {
            terminal.releaseRenderer(renderer);
            renderer = null;
        }
    }
}
