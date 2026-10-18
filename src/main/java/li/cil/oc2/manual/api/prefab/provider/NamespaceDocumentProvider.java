/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.api.provider.DocumentProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Basic implementation of a content provider based on Minecraft's resource
 * loading framework.
 * <p>
 * Beware that the manual is unaware of resource domains. In other words, two
 * paths that are identical except for their resource domain will be the same,
 * as seen from the manual.
 */
public class NamespaceDocumentProvider implements DocumentProvider {
    private final String namespace;
    private final String basePath;

    public NamespaceDocumentProvider(final String namespace, final String basePath) {
        this.namespace = namespace;
        this.basePath = basePath.endsWith("/") ? basePath : (basePath + "/");
    }

    public NamespaceDocumentProvider(final String namespace) {
        this(namespace, "");
    }

    @Override
    public Optional<Document> getDocument(final String path, final String language) {
        final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        final ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, basePath + path);
        return resourceManager.getResource(location).flatMap(resource -> {
            try (InputStream stream = resource.open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                final ArrayList<String> lines = new ArrayList<>();
                while (true) {
                    final String line = reader.readLine();
                    if (line == null) break;
                    lines.add(line);
                }
                return Optional.of(new Document(lines, location));
            } catch (final java.io.IOException ignored) {
                return Optional.empty();
            }
        });
    }
}
