/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client;

import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualScreenStyle;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.provider.RendererProvider;
import li.cil.oc2.manual.api.util.Constants;
import li.cil.oc2.manual.api.util.ShowManualScreenEvent;
import li.cil.oc2.manual.client.gui.ManualScreen;
import li.cil.oc2.manual.client.provider.BlockRendererProvider;
import li.cil.oc2.manual.client.provider.ItemRendererProvider;
import li.cil.oc2.manual.client.provider.TagRendererProvider;
import li.cil.oc2.manual.client.provider.TextureRendererProvider;
import li.cil.oc2.manual.client.util.ManualRegistry;
import li.cil.oc2.manual.client.util.RegistryUtils;

public final class ClientSetup {
    public static void initialize() {
        final ManualRegistry<RendererProvider> rendererProviders = RegistryUtils.get(Constants.RENDERER_PROVIDER_REGISTRY);

        rendererProviders.register("texture", TextureRendererProvider::new);
        rendererProviders.register("item", ItemRendererProvider::new);
        rendererProviders.register("block", BlockRendererProvider::new);
        rendererProviders.register("tag", TagRendererProvider::new);

        ShowManualScreenEvent.SHOW_MANUAL_SCREEN_EVENT.register(ClientSetup::handleShowManualScreen);
    }

    // --------------------------------------------------------------------- //

    private static void handleShowManualScreen(final ShowManualScreenEvent event) {
        final ManualModel model = event.getManualModel();
        final ManualStyle manualStyle = event.getManualStyle().orElse(ManualStyle.DEFAULT);
        final ManualScreenStyle screenStyle = event.getScreenStyle().orElse(ManualScreenStyle.DEFAULT);

        final ManualScreen screen = new ManualScreen(model, manualStyle, screenStyle);
        li.cil.oc2.client.RenderCompat.setScreen(screen);
    }
}
