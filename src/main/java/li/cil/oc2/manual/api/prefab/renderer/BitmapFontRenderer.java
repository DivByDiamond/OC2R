/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.renderer;

import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;
import java.util.Optional;
import li.cil.oc2.manual.api.render.FontRenderer;
import li.cil.oc2.manual.client.util.ManualGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Base implementation for texture based monospace font rendering.
 */
public abstract class BitmapFontRenderer implements FontRenderer {
    private final Char2IntMap CHAR_MAP;

    private final int COLUMNS = getResolution() / (charWidth() + getGapU());
    private final float U_STEP = (charWidth() + getGapU()) / (float) getResolution();
    private final float V_STEP = (lineHeight() + getGapV()) / (float) getResolution();

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // Font subclasses provide constant atlas dimensions/characters.
    protected BitmapFontRenderer() {
        CHAR_MAP = new Char2IntOpenHashMap();
        final CharSequence chars = getCharacters();
        for (int index = 0; index < chars.length(); index++) {
            CHAR_MAP.put(chars.charAt(index), index);
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(final GuiGraphics graphics, final CharSequence value, final int argb) {
        for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || Character.isISOControl(ch)) continue;
            final int index = getCharIndex(ch);
            final float u = index % COLUMNS * U_STEP * getResolution();
            final int row = index / COLUMNS;
            final float v = row * V_STEP * getResolution();
            ManualGraphics.blit(graphics, getTextureLocation(), i * charWidth(), 0,
                    u, v, charWidth(), lineHeight(), getResolution(), getResolution(), argb);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int width(final CharSequence value) {
        return value.length() * charWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int width(final Component value) {
        final MutableInteger count = new MutableInteger();
        value.visit(s -> {
            count.value += s.length() * charWidth();
            return Optional.empty();
        });
        return count.value;
    }

    // --------------------------------------------------------------------- //

    /**
     * The list of characters available in the backing texture, in left-to right, then top-to bottom order.
     * <p>
     * For example, for the character sequence {@code abcd}, the texture would look like this:
     * <pre>
     * ab
     * cd
     * </pre>
     *
     * @return the list of characters.
     */
    protected abstract CharSequence getCharacters();

    /**
     * The location of the texture to use for rendering characters.
     *
     * @return the location of the font texture.
     */
    protected abstract ResourceLocation getTextureLocation();

    /**
     * The actual resolution of the texture.
     * <p>
     * Note that the texture is expected to be squared.
     *
     * @return the resolution of the texture.
     */
    protected abstract int getResolution();

    /**
     * The horizontal pixel gap between characters in the font texture.
     *
     * @return the horizontal character gap.
     */
    protected abstract int getGapU();

    /**
     * The vertical pixel gap between characters in the font texture.
     *
     * @return the vertical character gap.
     */
    protected abstract int getGapV();

    /**
     * The width of a single character, in pixels.
     *
     * @return the width of a character.
     */
    protected abstract int charWidth();

    // --------------------------------------------------------------------- //

    private int getCharIndex(final char ch) {
        if (!CHAR_MAP.containsKey(ch)) {
            return CHAR_MAP.get('?');
        }
        return CHAR_MAP.get(ch);
    }

    // --------------------------------------------------------------------- //

    private static final class MutableInteger {
        public int value;
    }

}
