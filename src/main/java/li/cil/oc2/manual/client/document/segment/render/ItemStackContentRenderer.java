/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment.render;

import li.cil.oc2.manual.api.render.ContentRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ItemStackContentRenderer implements ContentRenderer {
    /**
     * How long to show individual stacks, in milliseconds, before switching to the next.
     */
    private static final int CYCLE_SPEED = 1000;

    // --------------------------------------------------------------------- //

    private final ItemStack[] stacks;

    // --------------------------------------------------------------------- //

    public ItemStackContentRenderer(final ItemStack... stacks) {
        this.stacks = stacks.clone();
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getWidth() {
        return 32;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final int index = (int) (System.currentTimeMillis() % (CYCLE_SPEED * (long) stacks.length)) / CYCLE_SPEED;
        final ItemStack stack = stacks[index];

        final float scaleX = getWidth() / 16f;
        final float scaleY = getHeight() / 16f;

        final var pose = li.cil.oc2.manual.client.util.ManualGraphics.pose(graphics);

        pose.pushPose();
        pose.scale(scaleX, scaleY, 1);

        li.cil.oc2.manual.client.util.ManualGraphics.fakeItem(graphics, stack, 0, 0);

        pose.popPose();
    }
}
