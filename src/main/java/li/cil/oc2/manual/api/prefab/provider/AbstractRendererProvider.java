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

    /**
     * Creates a new provider for renderers below the given prefix.
     *
     * @param prefix the prefix of the paths this provider is responsible for.
     */
    public AbstractRendererProvider(final String prefix) {
        this.prefix = prefix + ":";
    }

    /**
     * Gets the renderer for the given path, if the path is prefixed with this provider's prefix.
     *
     * @param path the path to the requested image content.
     * @return the image renderer for the data, or {@link Optional#empty()} if the specified path is not supported.
     */
    @Override
    public Optional<ContentRenderer> getRenderer(final String path) {
        if (path.startsWith(prefix)) {
            return doGetRenderer(path.substring(prefix.length()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the renderer for the given path, with this provider's prefix already stripped.
     *
     * @param data the path to the requested image content, without the prefix.
     * @return the image renderer for the data, or {@link Optional#empty()} if the specified path is not supported.
     */
    protected abstract Optional<ContentRenderer> doGetRenderer(final String data);
}
