/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * A single page of a manual, holding the raw lines it is rendered from.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Document filters deliberately edit the shared line list.")
public final class Document {
    private final List<String> lines;
    @Nullable private final ResourceLocation location;

    /**
     * Creates a new document.
     *
     * @param lines    the lines making up the document.
     * @param location the location the document was loaded from, or {@code null} if it has none.
     */
    public Document(final List<String> lines, @Nullable final ResourceLocation location) {
        this.location = location;
        this.lines = lines;
    }

    /**
     * Creates a new document without a location.
     *
     * @param lines the lines making up the document.
     */
    public Document(final List<String> lines) {
        this(lines, null);
    }

    /**
     * The lines making up the document.
     *
     * @return the lines of the document.
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * The location the document was loaded from.
     *
     * @return the location of the document, or {@code null} if it has none.
     */
    @Nullable
    public ResourceLocation getLocation() {
        return location;
    }
}
