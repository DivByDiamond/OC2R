/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment;

import java.util.List;
import java.util.regex.Pattern;
import li.cil.oc2.manual.api.render.FontRenderer;
import li.cil.oc2.manual.client.document.DocumentRenderer;

public final class MonospaceSegment extends TextSegment {
    public MonospaceSegment(final DocumentRenderer document, final Segment parent, final String text) {
        super(document, parent, text);
    }

    /** Code spans are literal and must not be parsed as links, images or emphasis. */
    @Override
    public Iterable<Segment> refine(final Pattern pattern, final SegmentRefiner factory) {
        return List.of(this);
    }

    // --------------------------------------------------------------------- //

    @Override
    public String toString() {
        return String.format("`%s`", super.toString());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected boolean isIgnoringLeadingWhitespace() {
        return false;
    }

    @Override
    protected int getColor() {
        return style.getMonospaceTextColor();
    }

    @Override
    protected FontRenderer getFont() {
        return style.getMonospaceFont();
    }
}
