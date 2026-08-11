package li.cil.oc2.client.gui.screen.keyboard;

import li.cil.oc2.client.gui.screen.misc.KeyCodeMapping;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.monitor.input.KeyboardInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class KeyboardScreen extends Screen {
    private static final int BORDER_SIZE = 4;
    private static final float ARM_SWING_RATE = 0.8f;
    private static final int BORDER_COLOR = 0xFFFFFFFF;

    private static final MutableComponent CLOSE_INFO =
            Component.translatable("gui.oc2r.keyboard.close_info");

    private final KeyboardBlockEntity keyboard;

    public static boolean hideHotbar = false;

    /**
     * Creates a new keyboard screen for the given keyboard block entity.
     *
     * @param keyboard the keyboard block entity.
     */
    public KeyboardScreen(final KeyboardBlockEntity keyboard) {
        super(Items.KEYBOARD.get().getDescription());
        this.keyboard = keyboard;
    }

    @Override
    protected void init() {
        super.init();

        // Disable hotbar since we don't need it here, and it just blocks screen space.
        hideHotbar = true;
    }

    @Override
    public void tick() {
        super.tick();

        final Vec3 keyboardCenter = Vec3.atCenterOf(keyboard.getBlockPos());
        if (!keyboard.isValid()
                || getMinecraft().player == null
                || getMinecraft().player.distanceToSqr(keyboardCenter) > 8 * 8) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(final int keycode, final int scancode, final int modifiers) {
        sendInputMessage(keycode, true);
        return true;
    }

    @Override
    public boolean keyReleased(final int keycode, final int scancode, final int modifiers) {
        sendInputMessage(keycode, false);
        return true;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
            onClose();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void renderBackground(
            final GuiGraphics guiGraphics,
            final int mouseX,
            final int mouseY,
            final float partialTick) {
        // Don't
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        renderBorderOverlay(graphics);

        graphics.drawWordWrap(
                font,
                CLOSE_INFO,
                BORDER_SIZE * 3,
                height - BORDER_SIZE * 3 - font.lineHeight,
                width - BORDER_SIZE * 6,
                0x88FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();

        hideHotbar = false;
    }

    private void renderBorderOverlay(final GuiGraphics graphics) {
        graphics.fill(BORDER_SIZE, BORDER_SIZE, width - BORDER_SIZE, BORDER_SIZE * 2, BORDER_COLOR);
        graphics.fill(
                BORDER_SIZE, BORDER_SIZE, BORDER_SIZE * 2, height - BORDER_SIZE, BORDER_COLOR);
        graphics.fill(
                BORDER_SIZE,
                height - BORDER_SIZE * 2,
                width - BORDER_SIZE,
                height - BORDER_SIZE,
                BORDER_COLOR);
        graphics.fill(
                width - BORDER_SIZE * 2,
                BORDER_SIZE,
                width - BORDER_SIZE,
                height - BORDER_SIZE,
                BORDER_COLOR);
    }

    private void sendInputMessage(final int keycode, final boolean isDown) {
        if (KeyCodeMapping.MAPPING.containsKey(keycode)) {
            swingArm();
            final int evdevCode = KeyCodeMapping.MAPPING.get(keycode);
            NetworkMessages.sendToServer(new KeyboardInputMessage(keyboard, evdevCode, isDown));
        }
    }

    private void swingArm() {
        final Minecraft minecraft = getMinecraft();
        final LocalPlayer player = minecraft.player;
        if (player != null) {
            final RandomSource random = player.getRandom();
            if (random.nextFloat() >= ARM_SWING_RATE) {
                final InteractionHand handToSwing;
                if (minecraft.options.getCameraType().isFirstPerson()) {
                    handToSwing = InteractionHand.MAIN_HAND;
                } else {
                    handToSwing =
                            random.nextBoolean()
                                    ? InteractionHand.MAIN_HAND
                                    : InteractionHand.OFF_HAND;
                }
                player.swing(handToSwing);
            }
        }
    }
}