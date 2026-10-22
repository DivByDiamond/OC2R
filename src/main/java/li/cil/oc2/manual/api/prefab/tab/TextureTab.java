/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.tab;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Simple implementation of a tab icon renderer using a full texture as its graphic.
 */
public final class TextureTab extends AbstractTab {
    private final ResourceLocation location;

    public TextureTab(final String path, @Nullable final Component tooltip, final ResourceLocation location) {
        super(path, tooltip);
        this.location = location;
    }

    @Override
    public void renderIcon(final GuiGraphics graphics) {
        li.cil.oc2.manual.client.util.ManualGraphics.blit(graphics, location, 0, 0, 0, 0, 16, 16, 16, 16);
    }
}
