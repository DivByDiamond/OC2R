/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment;

import javax.annotation.Nullable;

public final class NextSegmentInfo {
    @Nullable public final Segment segment;
    public int absoluteX;
    public int relativeY;

    // --------------------------------------------------------------------- //

    public NextSegmentInfo(@Nullable final Segment segment, final int absoluteX, final int relativeY) {
        this.segment = segment;
        this.absoluteX = absoluteX;
        this.relativeY = relativeY;
    }

    public NextSegmentInfo(final Segment segment) {
        this.segment = segment;
    }
}
