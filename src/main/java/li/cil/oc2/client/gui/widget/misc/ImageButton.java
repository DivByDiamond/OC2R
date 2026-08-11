package li.cil.oc2.client.gui.widget.misc;

import static java.util.Collections.emptyList;
import static li.cil.oc2.common.util.text.TextFormatUtils.withFormat;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import li.cil.oc2.client.gui.widget.Sprite;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class ImageButton extends AbstractButton {
    private static final long PRESS_DURATION = 200;
    private final Sprite baseImage;
    private final Sprite pressedImage;
    private List<Component> tooltip = emptyList();
    private long lastPressedAt;

    protected ImageButton(
            final int x,
            final int y,
            final int width,
            final int height,
            final Sprite baseImage,
            final Sprite pressedImage) {
        super(x, y, width, height, Component.empty());
        this.baseImage = baseImage;
        this.pressedImage = pressedImage;
    }

    public ImageButton withMessage(final Component component) {
        setMessage(component);
        return this;
    }

    public final ImageButton withTooltip(final Component... components) {
        tooltip = Arrays.asList(components);
        for (int i = 1; i < tooltip.size(); i++) {
            final Component component = tooltip.get(i);
            tooltip.set(i, withFormat(component, ChatFormatting.GRAY));
        }
        return this;
    }

    @Override
    public void onPress() {
        lastPressedAt = System.currentTimeMillis();
    }

    @Override
    public void renderWidget(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
    }

    protected void renderBackground(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTicks) {
        RenderSystem.enableDepthTest();

        Sprite background = baseImage;
        if ((System.currentTimeMillis() - lastPressedAt) < PRESS_DURATION) {
            background = pressedImage;
        }

        background.draw(graphics, getX(), getY());

        if (!Objects.equals(getMessage(), Component.empty())) {
            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    getX() + width / 2,
                    getY() + (height - 8) / 2,
                    getFGColor() | Mth.ceil(alpha * 255) << 24);
        }
    }

    @Override
    @Nullable
    public Tooltip getTooltip() {
        if (tooltip.stream().findFirst().isEmpty()) return null;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tooltip.size(); i++) {
            builder.append(tooltip.get(i).getString()).append(i == tooltip.size() - 1 ? "" : "\n");
        }
        Component component = Component.literal(builder.toString());
        return Tooltip.create(component);
    }
}