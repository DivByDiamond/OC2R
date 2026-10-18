/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Document filters deliberately edit the shared line list.")
public final class Document {
    private final List<String> lines;
    @Nullable private final ResourceLocation location;

    public Document(final List<String> lines, @Nullable final ResourceLocation location) {
        this.location = location;
        this.lines = lines;
    }

    public Document(final List<String> lines) {
        this(lines, null);
    }

    public List<String> getLines() {
        return lines;
    }

    @Nullable
    public ResourceLocation getLocation() {
        return location;
    }
}
