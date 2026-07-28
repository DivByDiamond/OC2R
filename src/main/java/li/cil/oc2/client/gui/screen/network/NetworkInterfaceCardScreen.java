package li.cil.oc2.client.gui.screen.network;

import static li.cil.oc2.common.util.text.TranslationUtils.text;

import javax.annotation.Nullable;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.screen.computer.ComputerBlockItemRenderer;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.network.NetworkInterfaceCardConfigurationMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

public final class NetworkInterfaceCardScreen extends Screen {
    private static final Component INFO_TEXT = text("gui.{mod}.network_interface_card.info");

    public static final int UI_WIDTH = Sprites.NETWORK_INTERFACE_CARD_SCREEN.width;
    public static final int UI_HEIGHT = Sprites.NETWORK_INTERFACE_CARD_SCREEN.height;
    public static final int BLOCK_LEFT = UI_WIDTH / 2;
    public static final int BLOCK_TOP = 53;
    public static final int INFO_TEXT_LEFT = 8;
    public static final int INFO_TEXT_TOP = 104;
    public static final int INFO_TEXT_WIDTH = UI_WIDTH - 16;
    public static final int MAX_BLOCK_PITCH = 30;

    private final Player player;
    private final InteractionHand hand;

    private final ComputerBlockItemRenderer computerBlockItemRenderer =
            new ComputerBlockItemRenderer();

    private Vector3f blockRotation = new Vector3f(0, 0, 0);
    private int left;
    private int top;
    @Nullable private Direction focusedSide;
    private boolean isDraggingBlock;
    private boolean hasDraggedBlock;
    private double dragStartX;
    private double dragStartY;

    public NetworkInterfaceCardScreen(final Player player, final InteractionHand hand) {
        super(Items.NETWORK_INTERFACE_CARD.get().getDescription());
        this.player = player;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        left = (width - UI_WIDTH) / 2;
        top = (height - UI_HEIGHT) / 2;
    }

    @Override
    public void tick() {
        super.tick();

        final ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.is(Items.NETWORK_INTERFACE_CARD.get())) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final boolean result = super.mouseClicked(mouseX, mouseY, button);

        if (!result && isMouseInBlockArea(mouseX, mouseY) && button == 0) {
            isDraggingBlock = true;
            hasDraggedBlock = false;
            dragStartX = mouseX;
            dragStartY = mouseY;
        }

        return result;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (isDraggingBlock && button == 0) {
            isDraggingBlock = false;
            if (!hasDraggedBlock && focusedSide != null) {
                final NetworkInterfaceCardConfigurationMessage message =
                        new NetworkInterfaceCardConfigurationMessage(
                                hand, focusedSide, !getConfiguration(focusedSide));
                NetworkMessages.sendToServer(message);
                Minecraft.getInstance()
                        .getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            final double mouseX,
            final double mouseY,
            final int activeButton,
            final double deltaX,
            final double deltaY) {
        if (isDraggingBlock && activeButton == 0) {
            if (!hasDraggedBlock) {
                final double dx = mouseX - dragStartX;
                final double dy = mouseY - dragStartY;
                final double delta = Math.sqrt(dx * dx + dy * dy);
                hasDraggedBlock = delta > 3;
            }
            if (hasDraggedBlock) {
                blockRotation =
                        new Vector3f(
                                Mth.clamp(
                                        blockRotation.x() - (float) deltaY * 0.2f,
                                        -MAX_BLOCK_PITCH,
                                        MAX_BLOCK_PITCH),
                                Mth.wrapDegrees(blockRotation.y() + (float) deltaX * 0.2f),
                                blockRotation.z());
            }
        }

        return true;
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        Sprites.NETWORK_INTERFACE_CARD_SCREEN.draw(graphics, left, top);

        super.render(graphics, mouseX, mouseY, partialTicks);

        final int blockX = left + BLOCK_LEFT;
        final int blockY = top + BLOCK_TOP;
        focusedSide =
                computerBlockItemRenderer.getFocusedSide(
                        blockX - mouseX, blockY - mouseY, blockRotation);
        computerBlockItemRenderer.render(blockX, blockY, blockRotation, focusedSide, this);

        // renderTooltip was here, left as comment for future reference

        graphics.drawWordWrap(
                font,
                INFO_TEXT,
                left + INFO_TEXT_LEFT,
                top + INFO_TEXT_TOP,
                INFO_TEXT_WIDTH,
                0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isMouseInBlockArea(final double mouseX, final double mouseY) {
        return mouseX >= left + 37
                && mouseX <= left + (37 + 102)
                && mouseY >= top + 10
                && mouseY <= top + (10 + 102);
    }

    public boolean getConfiguration(@Nullable final Direction side) {
        return side != null
                && NetworkInterfaceCardItem.getSideConfiguration(player.getItemInHand(hand), side);
    }
}