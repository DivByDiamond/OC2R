/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment;

import java.util.regex.Matcher;
import li.cil.oc2.manual.client.document.DocumentRenderer;

@FunctionalInterface
public interface SegmentRefiner {
    Segment refine(final DocumentRenderer document, final Segment segment, final Matcher matcher);
}
