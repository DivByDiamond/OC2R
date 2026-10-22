/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.tab;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Simple implementation of a tab icon renderer using an item stack as its graphic.
 */
public final class ItemStackTab extends AbstractTab {
    private final ItemStack stack;

    public ItemStackTab(final String path, @Nullable final Component tooltip, final ItemStack stack) {
        super(path, tooltip);
        this.stack = stack.copy();
    }

    @Override
    public void renderIcon(final GuiGraphics graphics) {
        li.cil.oc2.manual.client.util.ManualGraphics.fakeItem(graphics, stack, 0, 0);
    }
}
