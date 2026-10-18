/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment;

import li.cil.oc2.manual.client.document.DocumentRenderer;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.StringUtils;

public final class HeaderSegment extends TextSegment {
    private final int level;
    private final float fontScale;

    // --------------------------------------------------------------------- //

    public HeaderSegment(final DocumentRenderer document, final Segment parent, final String text, final int level) {
        super(document, parent, text);
        this.level = level;
        fontScale = Math.max(1, 1.75f - level * 0.25f);
    }

    // --------------------------------------------------------------------- //

    @Override
    public String toString() {
        return String.format("%s %s", StringUtils.repeat('#', level), super.toString());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected float getScale() {
        return fontScale * super.getScale();
    }

    @Override
    protected String getFormat() {
        return super.getFormat() + ChatFormatting.UNDERLINE;
    }

    @Override
    protected int getLineHeight() {
        return (int) (1.4f * super.getLineHeight());
    }
}
