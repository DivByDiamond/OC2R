package li.cil.oc2.client.gui.screen.tablet;

import java.util.BitSet;
import li.cil.oc2.client.gui.screen.common.AbstractModContainerScreen;
import li.cil.oc2.client.gui.screen.misc.KeyCodeMapping;
import li.cil.oc2.client.gui.screen.widget.InputCaptureButton;
import li.cil.oc2.client.gui.screen.widget.InventoryButton;
import li.cil.oc2.client.gui.screen.widget.PowerButton;
import li.cil.oc2.client.gui.widget.tablet.TabletFramebufferRenderer;
import li.cil.oc2.client.gui.widget.tablet.TabletViewport;
import li.cil.oc2.client.gui.widget.terminal.TabletConsoleViewport;
import li.cil.oc2.client.input.CaptureKeyBinding;
import li.cil.oc2.common.container.tablet.TabletTerminalContainer;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.tablet.TabletPointerMessage;
import li.cil.oc2.common.tablet.TabletClientView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
//? if >=26.1 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** One compact tablet with independent guest graphics and UART views of the same running machine. */
public final class TabletTerminalScreen extends AbstractModContainerScreen<TabletTerminalContainer> {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 224;
    private static boolean preferGraphics = true;
    private final TabletConsoleViewport console;
    private final BitSet heldKeys = new BitSet();
    private TabletFramebufferRenderer display;
    private Button graphicsButton;
    private Button consoleButton;
    private int buttons;
    private boolean graphical;
    private TabletViewport.Pixel pendingPointer;
    private EditBox focusIndicatorEditBox;

    public TabletTerminalScreen(final TabletTerminalContainer menu, final Inventory inventory, final Component title) {
        //? if >=26.1 {
        /*super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        *///?} else {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
        //?}
        console = new TabletConsoleViewport(menu);
        graphical = preferGraphics;
    }

    @Override public void init() {
        super.init();
        graphicsButton = addRenderableWidget(Button.builder(Component.translatable("gui.oc2r.tablet.screen"),
                button -> showGraphics(true)).bounds(leftPos + 94, topPos + 7, 78, 20).build());
        consoleButton = addRenderableWidget(Button.builder(Component.translatable("gui.oc2r.tablet.console"),
                button -> showGraphics(false)).bounds(leftPos + 174, topPos + 7, 94, 20).build());
        addRenderableWidget(new PowerButton(leftPos + 13, topPos + 204, menu));
        addRenderableWidget(new InputCaptureButton(leftPos + 33, topPos + 204, menu));
        addRenderableWidget(new InventoryButton(leftPos + 53, topPos + 204, menu));
        focusIndicatorEditBox = new EditBox(font, 0, 0, 0, 0, Component.empty());
        focusIndicatorEditBox.setFocused(true);
        updateTabs();
    }

    public boolean isGraphical() { return graphical; }

    public TabletViewport viewport() { return new TabletViewport(leftPos + 12, topPos + 34, 256, 160); }

    /** Changes only the client viewport; no guest power, process or terminal state is touched. */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "The last selected display mode is a client-wide UI preference.")
    public void showGraphics(final boolean value) {
        releaseInput();
        graphical = value;
        preferGraphics = value;
        updateTabs();
    }

    private void updateTabs() {
        if (graphicsButton != null) graphicsButton.active = !graphical;
        if (consoleButton != null) consoleButton.active = graphical;
    }

    @Override public void containerTick() {
        super.containerTick();
        console.tick();
        if (pendingPointer != null && graphical && menu.getCaptureInputState()) {
            send(TabletPointerMessage.POINTER, pendingPointer.x(), pendingPointer.y(), buttons);
            pendingPointer = null;
        }
        if ((!menu.getCaptureInputState() || !net.minecraft.client.Minecraft.getInstance().isWindowActive())
                && (!heldKeys.isEmpty() || buttons != 0)) releaseInput();
    }

    @Override protected void renderBg(final GuiGraphics graphics,
            final float partialTick, final int mouseX, final int mouseY) {
        graphics.fill(leftPos - 2, topPos - 2, leftPos + PANEL_WIDTH + 2, topPos + PANEL_HEIGHT + 2, 0xFF435A66);
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, 0xFF202D36);
        graphics.fill(leftPos + 11, topPos + 33, leftPos + 269, topPos + 195, 0xFF071216);
        graphics.fill(leftPos + 12, topPos + 194, leftPos + 268, topPos + 195, 0xFF56CECF);
        text(graphics, title, leftPos + 12, topPos + 13, 0xFFE4EEF3);
    }

    @Override protected void renderFg(final GuiGraphics graphics,
            final float partialTick, final int mouseX, final int mouseY) {
        final TabletViewport area = viewport();
        if (graphical && menu.getVirtualMachine().isRunning()) {
            if (display == null) display = new TabletFramebufferRenderer();
            TabletClientView.find(menu.tabletId()).ifPresent(view -> display.render(graphics, view, area));
        } else if (!graphical && menu.getVirtualMachine().isRunning()) {
            console.render(graphics, area.x(), area.y(), area.width(), area.height(), width, height);
        }
        final Component error = menu.getVirtualMachine().getError();
        if (error != null && !error.getString().isEmpty())
            text(graphics, error, area.x() + 6, area.y() + 6, 0xFFFFB58A);
        else if (!menu.getVirtualMachine().isRunning()) text(graphics, Component.translatable("gui.oc2r.tablet.off"),
                area.x() + 6, area.y() + 6, 0xFFB6CBD2);
        text(graphics, Component.translatable("gui.oc2r.tablet.release",
                CaptureKeyBinding.RELEASE.getTranslatedKeyMessage()),
                leftPos + 76, topPos + 207, 0xFFB6CBD2);
    }

    private void text(final GuiGraphics graphics, final Component text, final int x, final int y, final int color) {
        //? if >=26.1 {
        /*graphics.text(font, text, x, y, color, false);
        *///?} else {
        graphics.drawString(font, text, x, y, color, false);
        //?}
    }

    public boolean handleHostKey(final int key, final int scan, final int action, final int modifiers) {
        if (!menu.getCaptureInputState()) return false;
        if (CaptureKeyBinding.matches(key, scan)) {
            if (action != GLFW.GLFW_RELEASE) onClose();
            return true;
        }
        return guestKey(key, scan, action, modifiers);
    }

    private boolean guestKey(final int key, final int scan, final int action, final int modifiers) {
        if (!menu.getCaptureInputState()) return false;
        if (!graphical) {
            if (action != GLFW.GLFW_RELEASE) console.key(key, scan, modifiers);
            return true;
        }
        final int code = KeyCodeMapping.MAPPING.getOrDefault(key, -1);
        if (code >= 0 && action != GLFW.GLFW_REPEAT) {
            final boolean down = action != GLFW.GLFW_RELEASE;
            heldKeys.set(code, down);
            send(TabletPointerMessage.KEY, code, 0, down ? 1 : 0);
        }
        return true;
    }

    private boolean click(final double x, final double y, final int button, final boolean down) {
        if (CaptureKeyBinding.matchesMouse(button)) {
            if (down) onClose();
            return true;
        }
        if (!graphical || !menu.getCaptureInputState() || button < 0 || button > 2) return false;
        if (down && viewport().pixel(x, y).isEmpty()) return false;
        if (!down && (buttons & (1 << button)) == 0) return false;
        pendingPointer = null;
        buttons = down ? buttons | (1 << button) : buttons & ~(1 << button);
        final TabletViewport.Pixel position = viewport().clamp(x, y);
        send(TabletPointerMessage.POINTER, position.x(), position.y(), buttons);
        return true;
    }

    @Override public void mouseMoved(final double x, final double y) {
        super.mouseMoved(x, y);
        if (graphical && menu.getCaptureInputState()) pendingPointer = viewport().pixel(x, y).orElse(null);
    }

    private void send(final int operation, final int first, final int second, final int value) {
        NetworkMessages.sendToServer(new TabletPointerMessage(menu.tabletId(), operation, first, second, value));
    }

    private void releaseInput() {
        heldKeys.clear();
        buttons = 0;
        pendingPointer = null;
        send(TabletPointerMessage.RELEASE, 0, 0, 0);
    }

    //? if >=26.1 {
    /*@Override public boolean keyPressed(final KeyEvent event) {
        if (CaptureKeyBinding.matches(event.key(), event.scancode())) {
            onClose();
            return true;
        }
        return guestKey(event.key(), event.scancode(), GLFW.GLFW_PRESS, event.modifiers()) || super.keyPressed(event);
    }

    @Override public boolean keyReleased(final KeyEvent event) {
        return guestKey(event.key(), event.scancode(), GLFW.GLFW_RELEASE, event.modifiers())
                || super.keyReleased(event);
    }

    @Override public boolean charTyped(final CharacterEvent event) {
        return (!graphical && menu.getCaptureInputState() && Character.isBmpCodePoint(event.codepoint()))
                ? console.character((char) event.codepoint(), 0)
                : graphical || super.charTyped(event);
    }

    @Override public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return click(event.x(), event.y(), event.button(), true) || super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseReleased(final MouseButtonEvent event) {
        return click(event.x(), event.y(), event.button(), false) || super.mouseReleased(event);
    }
    *///?} else {

    @Override public boolean keyPressed(final int key, final int scan, final int modifiers) {
        if (CaptureKeyBinding.matches(key, scan)) {
            onClose();
            return true;
        }
        return guestKey(key, scan, GLFW.GLFW_PRESS, modifiers) || super.keyPressed(key, scan, modifiers);
    }

    @Override public boolean keyReleased(final int key, final int scan, final int modifiers) {
        return guestKey(key, scan, GLFW.GLFW_RELEASE, modifiers) || super.keyReleased(key, scan, modifiers);
    }

    @Override public boolean charTyped(final char ch, final int modifiers) {
        return (!graphical && menu.getCaptureInputState()) ? console.character(ch, modifiers)
                : graphical || super.charTyped(ch, modifiers);
    }

    @Override public boolean mouseClicked(final double x, final double y, final int button) {
        return click(x, y, button, true) || super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseReleased(final double x, final double y, final int button) {
        return click(x, y, button, false) || super.mouseReleased(x, y, button);
    }
    //?}

    @Override protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {}

    @Override public void removed() {
        releaseInput();
        console.close();
        if (display != null) {
            display.close();
            display = null;
        }
        super.removed();
    }
}
