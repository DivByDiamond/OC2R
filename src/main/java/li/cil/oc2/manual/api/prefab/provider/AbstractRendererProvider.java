/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.provider;

import java.util.Optional;
import li.cil.oc2.manual.api.provider.RendererProvider;
import li.cil.oc2.manual.api.render.ContentRenderer;

/**
 * Utility base class for {@link RendererProvider}s, taking care of the prefix check.
 */
public abstract class AbstractRendererProvider implements RendererProvider {
    private final String prefix;

    public AbstractRendererProvider(final String prefix) {
        this.prefix = prefix + ":";
    }

    @Override
    public Optional<ContentRenderer> getRenderer(final String path) {
        if (path.startsWith(prefix)) {
            return doGetRenderer(path.substring(prefix.length()));
        } else {
            return Optional.empty();
        }
    }

    protected abstract Optional<ContentRenderer> doGetRenderer(final String data);
}
