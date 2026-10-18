/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.provider;

import java.util.Optional;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.prefab.provider.AbstractRendererProvider;
import li.cil.oc2.manual.api.render.ContentRenderer;
import li.cil.oc2.manual.api.util.MatchResult;
import li.cil.oc2.manual.client.document.Strings;
import li.cil.oc2.manual.client.document.segment.render.ItemStackContentRenderer;
import li.cil.oc2.manual.client.document.segment.render.MissingContentRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemRendererProvider extends AbstractRendererProvider {
    public ItemRendererProvider() {
        super("item");
    }

    // --------------------------------------------------------------------- //

    @Override
    public MatchResult matches(final ManualModel manual) {
        return MatchResult.MATCH;
    }

    @Override
    protected Optional<ContentRenderer> doGetRenderer(final String data) {
        final Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(data)).orElse(Items.AIR);
        if (item != Items.AIR) {
            return Optional.of(new ItemStackContentRenderer(new ItemStack(item)));
        } else {
            return Optional.of(new MissingContentRenderer(Strings.NO_SUCH_ITEM));
        }
    }
}
