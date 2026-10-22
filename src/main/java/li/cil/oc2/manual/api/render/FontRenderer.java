/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Base interface for font renderers.
 */
public interface FontRenderer {
    /**
     * Draws a string through the current GUI context.
     *
     * @param graphics the current graphics context.
     * @param value    the string to render.
     * @param argb     the color to render the string with.
     */
    void draw(GuiGraphics graphics, CharSequence value, int argb);

    /**
     * Computes the rendered width of the provided character sequence.
     *
     * @param value the value to get the render width for.
     * @return the render width of the specified value.
     */
    int width(final CharSequence value);

    /**
     * Computes the rendered width of the provided text component.
     *
     * @param value the value to get the render width for.
     * @return the render width of the specified value.
     */
    int width(final Component value);

    /**
     * Get the height of the characters drawn with the font renderer, in pixels.
     *
     * @return the height of the drawn characters.
     */
    int lineHeight();
}
