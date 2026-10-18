/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment.render;

import li.cil.oc2.manual.api.render.InteractiveContentRenderer;
import li.cil.oc2.manual.api.util.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MissingContentRenderer extends TextureContentRenderer implements InteractiveContentRenderer {
    private static final ResourceLocation LOCATION_GUI_MANUAL_MISSING = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/missing.png");

    private final Component tooltip;

    // --------------------------------------------------------------------- //

    public MissingContentRenderer(final Component tooltip) {
        super(LOCATION_GUI_MANUAL_MISSING);
        this.tooltip = tooltip;
    }

    // --------------------------------------------------------------------- //

    @Override
    public Component getTooltip(final Component tooltip) {
        return this.tooltip;
    }
}
