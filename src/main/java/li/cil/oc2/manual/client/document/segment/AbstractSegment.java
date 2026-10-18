/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.client.document.DocumentRenderer;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Segments form a mutable linked document graph.")
abstract class AbstractSegment implements Segment {
    protected final DocumentRenderer document;
    protected final ManualModel model;
    protected final ManualStyle style;
    @Nullable private final Segment parent;
    protected Segment next;

    // --------------------------------------------------------------------- //

    protected AbstractSegment(final DocumentRenderer document, @Nullable final Segment parent) {
        this.document = document;
        this.model = document.getModel();
        this.style = document.getStyle();
        this.parent = parent;
    }

    // --------------------------------------------------------------------- //

    @Override
    public Segment getLineRoot() {
        return parent != null ? parent.getLineRoot() : this;
    }

    @Override
    public Optional<Segment> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Iterable<Segment> refine(final Pattern pattern, final SegmentRefiner refiner) {
        return Collections.singletonList(this);
    }

    @Override
    public void setNext(@Nullable final Segment segment) {
        next = segment;
    }
}
