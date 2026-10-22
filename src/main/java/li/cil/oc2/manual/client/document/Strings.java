/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document;

import li.cil.oc2.manual.api.util.Constants;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class Strings {
    public static final Component NO_SUCH_IMAGE = Component.translatable(Constants.MOD_ID + ".warning.missing.image");
    public static final Component NO_SUCH_ITEM = Component.translatable(Constants.MOD_ID + ".warning.missing.item");
    public static final Component NO_SUCH_BLOCK = Component.translatable(Constants.MOD_ID + ".warning.missing.block");
    public static final Component NO_SUCH_TAG = Component.translatable(Constants.MOD_ID + ".warning.missing.tag");

    public static Component getMissingContentText(final String url) {
        return Component.translatable(Constants.MOD_ID + ".warning.missing.content_renderer", url);
    }

    public static String getRedirectionLoopText() {
        return I18n.get(Constants.MOD_ID + ".warning.redirection_loop");
    }

    // --------------------------------------------------------------------- //

    private Strings() {
    }
}
