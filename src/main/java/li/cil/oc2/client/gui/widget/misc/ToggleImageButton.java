package li.cil.oc2.client.gui.widget.misc;

import li.cil.oc2.client.gui.widget.Sprite;
import net.minecraft.client.gui.GuiGraphics;

public abstract class ToggleImageButton extends ImageButton {
    private final Sprite activeImage;
    private boolean toggled;

    public ToggleImageButton(
            final int x,
            final int y,
            final int width,
            final int height,
            final Sprite baseImage,
            final Sprite pressedImage,
            final Sprite activeImage) {
        super(x, y, width, height, baseImage, pressedImage);
        this.activeImage = activeImage;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(final boolean value) {
        toggled = value;
    }

    @Override
    protected void renderBackground(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTicks) {
        super.renderBackground(graphics, mouseX, mouseY, partialTicks);
        if (isToggled()) {
            activeImage.draw(graphics, getX(), getY());
        }
    }
}