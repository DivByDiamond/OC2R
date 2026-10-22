/* SPDX-License-Identifier: MIT */

package li.cil.oc2.manual.client.document.segment;

import java.util.List;
import java.util.regex.Pattern;
import li.cil.oc2.manual.api.render.FontRenderer;
import li.cil.oc2.manual.client.document.DocumentRenderer;

/** A literal source line, including empty lines, inside a fenced code block. */
public final class CodeBlockLineSegment extends TextSegment {
    public CodeBlockLineSegment(final DocumentRenderer document, final String text) {
        super(document, null, expandTabs(text));
    }

    @Override
    public Iterable<Segment> refine(final Pattern pattern, final SegmentRefiner factory) {
        return List.of(this);
    }

    @Override
    protected boolean isIgnoringLeadingWhitespace() {
        return false;
    }

    @Override
    protected int computeWrappedIndent() {
        return 0; // A literal "- " prefix is not a Markdown list.
    }

    @Override
    protected int getColor() {
        return style.getMonospaceTextColor();
    }

    @Override
    protected FontRenderer getFont() {
        return style.getMonospaceFont();
    }

    private static String expandTabs(final String text) {
        final StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            final char ch = text.charAt(index);
            if (ch == '\t') {
                result.append(" ".repeat(4 - result.length() % 4));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
