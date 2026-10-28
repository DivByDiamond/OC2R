package li.cil.oc2.client.gui.screen.keyboard;

import li.cil.oc2.client.input.CaptureKeyBinding;
import li.cil.oc2.client.input.KeyboardCapture;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Transparent host input capture; only the configured host shortcut leaves the guest. */
public final class KeyboardScreen extends Screen {
    private final KeyboardBlockEntity keyboard;
    private final KeyboardCapture capture;
    public static boolean hideHotbar = false;

    public KeyboardScreen(final KeyboardBlockEntity keyboard) {
        super(Component.translatable(Items.KEYBOARD.get().getDescriptionId()));
        this.keyboard = keyboard;
        capture = new KeyboardCapture(keyboard);
    }

    @Override
    protected void init() {
        super.init();
        hideHotbar = true;
        capture.start();
    }

    @Override
    public void tick() {
        super.tick();
        final Minecraft client = Minecraft.getInstance();
        if (!keyboard.isValid() || client.player == null || !client.isWindowActive()
                || client.player.distanceToSqr(Vec3.atCenterOf(keyboard.getBlockPos())) > 64) {
            onClose();
            return;
        }
        capture.tick();
    }

    /** Handles native keyboard callbacks before vanilla fullscreen/debug shortcuts consume them. */
    public void handleHostKey(final int key, final int scan, final int action) {
        if (action == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
            if (!CaptureKeyBinding.matches(key, scan)) capture.key(key, false);
        } else {
            press(key, scan);
        }
    }

    private boolean press(final int key, final int scan) {
        if (CaptureKeyBinding.matches(key, scan)) onClose();
        else capture.key(key, true);
        return true;
    }

    private boolean click(final int button, final boolean down) {
        if (CaptureKeyBinding.matchesMouse(button)) {
            if (down) onClose();
        } else capture.button(button, down);
        return true;
    }

    //? if >=26.1 {
/*    @Override
    public boolean keyPressed(final KeyEvent event) { return press(event.key(), event.scancode()); }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        if (!CaptureKeyBinding.matches(event.key(), event.scancode())) capture.key(event.key(), false);
        return true;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return click(event.button(), true);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) { return click(event.button(), false); }

    @Override
    public void extractBackground(final GuiGraphics graphics, final int mouseX, final int mouseY,
            final float partialTick) {}

    @Override
    public void extractRenderState(final GuiGraphics graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.textWithWordWrap(font, hint(), 12, height - 24, width - 24, 0xCCFFFFFF);
    }
*///?} else {
    @Override
    public boolean keyPressed(final int key, final int scan, final int modifiers) { return press(key, scan); }

    @Override
    public boolean keyReleased(final int key, final int scan, final int modifiers) {
        if (!CaptureKeyBinding.matches(key, scan)) capture.key(key, false);
        return true;
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) { return click(button, true); }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        return click(button, false);
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY,
            final float partialTick) {}

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawWordWrap(font, hint(), 12, height - 24, width - 24, 0xCCFFFFFF);
    }
    //?}

    @Override
    public void mouseMoved(final double x, final double y) { capture.move(x, y); }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double horizontal, final double vertical) {
        capture.scroll(vertical);
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        capture.close();
        hideHotbar = false;
        super.removed();
    }

    private Component hint() {
        return Component.translatable(
                "gui.oc2r.keyboard.close_info", CaptureKeyBinding.RELEASE.getTranslatedKeyMessage());
    }
}
