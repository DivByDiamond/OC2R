/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.util;

import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.Tab;
import li.cil.oc2.manual.api.provider.DocumentProvider;
import li.cil.oc2.manual.api.provider.PathProvider;
import li.cil.oc2.manual.api.provider.RendererProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class Constants {
    public static final String MOD_ID = "markdown_manual";

    // ----------------------------------------------------------------------- //

    public static final ResourceKey<Registry<PathProvider>> PATH_PROVIDER_REGISTRY = key("path_provider");
    public static final ResourceKey<Registry<DocumentProvider>> DOCUMENT_PROVIDER_REGISTRY = key("document_provider");
    public static final ResourceKey<Registry<RendererProvider>> RENDERER_PROVIDER_REGISTRY = key("renderer_provider");
    public static final ResourceKey<Registry<Tab>> TAB_REGISTRY = key("tab");
    public static final ResourceKey<Registry<ManualModel>> MANUAL_REGISTRY = key("manual");

    // ----------------------------------------------------------------------- //

    private static <T> ResourceKey<Registry<T>> key(final String name) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
    }

    // ----------------------------------------------------------------------- //

    private Constants() {
    }
}
