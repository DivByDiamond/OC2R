/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.renderer;

import li.cil.oc2.manual.api.render.FontRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Implementation of the {@link FontRenderer} interface using the default Minecraft font renderer.
 */
public final class MinecraftFontRenderer implements FontRenderer {
    private final net.minecraft.client.gui.Font font;

    public MinecraftFontRenderer(final net.minecraft.client.gui.Font font) {
        this.font = font;
    }

    @Override
    public void draw(final GuiGraphics graphics, final CharSequence value, final int argb) {
        li.cil.oc2.manual.client.util.ManualGraphics.text(graphics, font, value.toString(), argb);
    }

    @Override
    public int width(final CharSequence value) {
        return font.width(value.toString());
    }

    @Override
    public int width(final Component value) {
        return font.width(value);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
