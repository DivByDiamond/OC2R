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
import net.minecraft.world.item.ItemStack;

public final class TagRendererProvider extends AbstractRendererProvider {
    public TagRendererProvider() {
        super("tag");
    }

    // --------------------------------------------------------------------- //

    @Override
    public MatchResult matches(final ManualModel manual) {
        return MatchResult.MATCH;
    }

    @Override
    protected Optional<ContentRenderer> doGetRenderer(final String data) {
        final ResourceLocation location = ResourceLocation.parse(data);
        final var tag = BuiltInRegistries.ITEM.getTagOrEmpty(
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, location));
        final ItemStack[] stacks = java.util.stream.StreamSupport.stream(tag.spliterator(), false)
                .map(ItemStack::new).toArray(ItemStack[]::new);
        return Optional.of(stacks.length == 0
                ? new MissingContentRenderer(Strings.NO_SUCH_TAG)
                : new ItemStackContentRenderer(stacks));
    }
}
