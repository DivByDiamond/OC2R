package li.cil.oc2.common.vm.terminal.render.overlay;

import com.mojang.blaze3d.vertex.BufferBuilder;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalBackgroundRenderer {
    public static void renderBackground(
            final Terminal terminal,
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final int row) {
        float backgroundStartX = -1;
        int backgroundColor = 0;
        float tx = 0f;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        int index =
                useAltBuffer
                        ? row * Terminal.WIDTH
                        : (row + (terminal.lastRowToDisplay - Terminal.HEIGHT)) * Terminal.WIDTH;
        for (int col = 0; col < Terminal.WIDTH; col++, index++) {
            final byte style = useAltBuffer ? terminal.altStyles[index] : terminal.styles[index];
            final boolean invertBackground = (style & Terminal.STYLE_INVERT_MASK) != 0;
            final ColorData color =
                    !invertBackground
                            ? useAltBuffer
                                    ? terminal.altColorsBackground[index]
                                    : terminal.colorsBackground[index]
                            : useAltBuffer ? terminal.altColors[index] : terminal.colors[index];

            if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

            final int[] palette =
                    (style & Terminal.STYLE_DIM_MASK) != 0
                            ? TerminalColors.DIM_COLORS
                            : TerminalColors.COLORS;
            int background =
                    switch (color.Mode) {
                        case SIXTEEN_COLOR -> palette[!invertBackground ? color.G : color.R];
                        case TWO_FIFTY_SIX_COLOR ->
                                TerminalColors.COLORS_256[!invertBackground ? color.G : color.R];
                        case TRUE_COLOR -> color.ToInt();
                        case SIXTEEN_COLOR_BRIGHT ->
                                TerminalColors.BRIGHT_COLORS[!invertBackground ? color.G : color.R];
                        case DEFAULT_BACKGROUND -> 0x000000;
                        default -> throw new AssertionError(color.Mode);
                    };

            final boolean hadBackground = backgroundStartX >= 0;
            final boolean hasBackground = background != 0x000000;
            if (!hadBackground && hasBackground) {
                backgroundStartX = tx;
                backgroundColor = background;
            } else if (hadBackground && (!hasBackground || backgroundColor != background)) {
                renderBackgroundRect(matrix, buffer, backgroundStartX, tx, backgroundColor);
                if (hasBackground) {
                    backgroundStartX = tx;
                    backgroundColor = background;
                } else {
                    backgroundStartX = -1;
                }
            }
            tx += Terminal.CHAR_WIDTH;
        }

        if (backgroundStartX >= 0) {
            renderBackgroundRect(matrix, buffer, backgroundStartX, tx, backgroundColor);
        }
    }

    static void renderBackgroundRect(
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final float x0,
            final float x1,
            final int color) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        buffer.addVertex(matrix, x0, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, 0, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x0, 0, 0).setColor(r, g, b, 1).setUv(0, 0);
    }
}