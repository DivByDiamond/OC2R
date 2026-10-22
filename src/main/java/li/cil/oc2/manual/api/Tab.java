/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api;

import java.util.List;
import li.cil.oc2.manual.api.util.MarkdownManualRegistryEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Represents a tab displayed next to the manual.
 * <p>
 * These are intended to link to index pages, and for the time being there
 * a relatively low number of tabs that can be displayed, so I'd ask you to
 * only register as many tabs as actually, technically <em>needed</em>. Which
 * will usually be one, for your main index page.
 */
public interface Tab extends MarkdownManualRegistryEntry {
    /**
     * Called when icon of a tab should be rendered.
     * <p>
     * This should render something in a 16x16 area. The OpenGL state has been
     * adjusted so that drawing starts at (0,0,0), and should go to (16,16,0).
     *
     * @param graphics the current graphics context.
     */
    void renderIcon(GuiGraphics graphics);

    /**
     * The (ideally localized) tooltip for the tab.
     *
     * @param tooltip the list to add the tooltip to.
     */
    void getTooltip(List<Component> tooltip);

    /**
     * The path to the page to open when the tab is clicked.
     *
     * @return the linked page path.
     */
    String getPath();
}
