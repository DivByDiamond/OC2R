/* SPDX-License-Identifier: MIT */

package li.cil.oc2.manual.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** GUI operations whose names or pose representation differ between Minecraft versions. */
public final class ManualGraphics {
    public static float approach(final float current, final float target, final float frameDelta) {
        // 26.x supplies elapsed ticks, which can exceed two on a slow frame.
        // Unbounded interpolation would overshoot and eventually hide the document.
        return net.minecraft.util.Mth.lerp(net.minecraft.util.Mth.clamp(frameDelta * 0.5f, 0, 1), current, target);
    }

    public static void enableScissor(final GuiGraphics graphics, final int width, final int height) {
        //? if >=26.1 {
        /*graphics.enableScissor(0, 0, width, height);
        *///?} else {
        // 1.21.1 expects absolute GUI coordinates; 26.x transforms local coordinates itself.
        final org.joml.Matrix4f matrix = graphics.pose().last().pose();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        final org.joml.Vector3f point = new org.joml.Vector3f();
        for (int corner = 0; corner < 4; corner++) {
            matrix.transformPosition((corner & 1) == 0 ? 0 : width,
                    (corner & 2) == 0 ? 0 : height, 0, point);
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        graphics.enableScissor((int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX), (int) Math.ceil(maxY));
        //?}
    }

    public static void componentTooltip(final GuiGraphics graphics, final Font font,
                                         final java.util.List<net.minecraft.network.chat.Component> lines,
                                         final int x, final int y) {
        //? if >=26.1 {
        /*graphics.setComponentTooltipForNextFrame(font, lines, x, y);
        *///?} else {
        graphics.renderComponentTooltip(font, lines, x, y);
        //?}
    }

    public static void tooltip(final GuiGraphics graphics, final Font font,
                                final java.util.List<net.minecraft.util.FormattedCharSequence> lines,
                                final net.minecraft.client.gui.screens.inventory.tooltip
                                        .ClientTooltipPositioner positioner,
                                final int x, final int y) {
        //? if >=26.1 {
        /*graphics.setTooltipForNextFrame(font, lines, positioner, x, y, true);
        *///?} else {
        graphics.renderTooltip(font, lines, positioner, x, y);
        //?}
    }

    public static Pose pose(final GuiGraphics graphics) {
        return new Pose(graphics);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Thin adapter intentionally borrows the active frame graphics.")
    public record Pose(GuiGraphics graphics) {
        public void pushPose() {
            //? if >=26.1 {
            /*graphics.pose().pushMatrix();
            *///?} else {
            graphics.pose().pushPose();
            //?}
        }

        public void popPose() {
            //? if >=26.1 {
            /*graphics.pose().popMatrix();
            *///?} else {
            graphics.pose().popPose();
            //?}
        }

        public void translate(final float x, final float y, final float z) {
            //? if >=26.1 {
            /*graphics.pose().translate(x, y);
            *///?} else {
            graphics.pose().translate(x, y, z);
            //?}
        }

        public void scale(final float x, final float y, final float z) {
            //? if >=26.1 {
            /*graphics.pose().scale(x, y);
            *///?} else {
            graphics.pose().scale(x, y, z);
            //?}
        }
    }

    public static void blit(final GuiGraphics graphics, final ResourceLocation texture,
                            final int x, final int y, final float u, final float v,
                            final int width, final int height, final int textureWidth, final int textureHeight) {
        blit(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight, -1);
    }

    public static void blit(final GuiGraphics graphics, final ResourceLocation texture,
                            final int x, final int y, final float u, final float v,
                            final int width, final int height, final int textureWidth,
                            final int textureHeight, final int argb) {
        //? if >=26.1 {
        /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture,
                x, y, u, v, width, height, textureWidth, textureHeight, argb);
        *///?} else {
        graphics.setColor((argb >>> 16 & 255) / 255f, (argb >>> 8 & 255) / 255f,
                (argb & 255) / 255f, (argb >>> 24) / 255f);
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        graphics.setColor(1, 1, 1, 1);
        //?}
    }

    public static void text(final GuiGraphics graphics, final Font font, final String value, final int argb) {
        //? if >=26.1 {
        /*graphics.text(font, value, 0, 0, argb);
        *///?} else {
        graphics.drawString(font, value, 0, 0, argb);
        //?}
    }

    public static void fakeItem(final GuiGraphics graphics, final ItemStack stack, final int x, final int y) {
        //? if >=26.1 {
        /*graphics.fakeItem(stack, x, y);
        *///?} else {
        graphics.renderFakeItem(stack, x, y);
        //?}
    }

    private ManualGraphics() {}
}
