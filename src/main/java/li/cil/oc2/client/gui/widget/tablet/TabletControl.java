package li.cil.oc2.client.gui.widget.tablet;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=26.1 {
/*import net.minecraft.client.input.InputWithModifiers;
*///?}
import net.minecraft.network.chat.Component;

/** Physical tablet keys and its display selector, with no vanilla button sprites. */
public final class TabletControl extends AbstractButton {
    public enum Kind { TAB, POWER, CAPTURE, INVENTORY }

    private final Kind kind;
    private final Runnable action;
    private final BooleanSupplier selected;

    public TabletControl(final int x, final int y, final int width, final int height,
            final Component label, final Kind kind, final Runnable action, final BooleanSupplier selected) {
        super(x, y, width, height, label);
        this.kind = kind;
        this.action = action;
        this.selected = selected;
        setTooltip(Tooltip.create(label));
    }

    //? if >=26.1 {
    /*@Override public void onPress(final InputWithModifiers input) { action.run(); }
    *///?} else {
    @Override public void onPress() { action.run(); }
    //?}

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    //? if >=26.1 {
    /*@Override protected void extractContents(
    *///?} else {
    @Override public void renderWidget(
    //?}
            final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final boolean on = selected.getAsBoolean();
        final int accent = kind == Kind.POWER ? 0xFFE5B66F : 0xFF63C7BE;
        final int outline = isFocused() ? 0xFFE6E5D8 : 0xFF405866;
        final int fill = on ? 0xFF294640 : isHoveredOrFocused() ? 0xFF304753 : 0xFF1C2B35;
        rounded(graphics, getX(), getY(), width, height, outline);
        rounded(graphics, getX() + 1, getY() + 1, width - 2, height - 2, fill);
        if (kind == Kind.TAB) {
            if (on) graphics.fill(getX() + 8, getY() + height - 3, getX() + width - 8,
                    getY() + height - 1, accent);
            final var font = Minecraft.getInstance().font;
            final int color = on ? 0xFFE6E5D8 : 0xFF99AFB8;
            final int x = getX() + (width - font.width(getMessage())) / 2;
            //? if >=26.1 {
            /*graphics.text(font, getMessage(), x, getY() + (height - 8) / 2, color, false);
            *///?} else {
            graphics.drawString(font, getMessage(), x, getY() + (height - 8) / 2, color, false);
            //?}
        } else {
            icon(graphics, getX() + width / 2 - 5, getY() + height / 2 - 5, on ? accent : 0xFF99AFB8);
        }
    }

    private static void rounded(final GuiGraphics graphics, final int x, final int y,
            final int width, final int height, final int color) {
        graphics.fill(x + 2, y, x + width - 2, y + height, color);
        graphics.fill(x, y + 2, x + width, y + height - 2, color);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    private void icon(final GuiGraphics graphics, final int x, final int y, final int ink) {
        if (kind == Kind.POWER) {
            graphics.fill(x + 4, y, x + 6, y + 6, ink);
            graphics.fill(x + 1, y + 3, x + 3, y + 8, ink);
            graphics.fill(x + 7, y + 3, x + 9, y + 8, ink);
            graphics.fill(x + 2, y + 8, x + 8, y + 10, ink);
        } else if (kind == Kind.CAPTURE) {
            graphics.fill(x, y + 1, x + 10, y + 9, ink);
            graphics.fill(x + 1, y + 2, x + 9, y + 8, 0xFF1C2B35);
            for (int column = 0; column < 3; column++) {
                graphics.fill(x + 2 + column * 3, y + 3, x + 3 + column * 3, y + 4, ink);
            }
            graphics.fill(x + 2, y + 6, x + 8, y + 7, ink);
        } else {
            graphics.fill(x, y + 1, x + 10, y + 9, ink);
            graphics.fill(x + 1, y + 2, x + 9, y + 8, 0xFF1C2B35);
            graphics.fill(x + 2, y + 3, x + 4, y + 7, ink);
            graphics.fill(x + 6, y + 3, x + 8, y + 7, ink);
        }
    }
}
