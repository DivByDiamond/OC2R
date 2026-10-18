/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.Tab;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.api.render.ContentRenderer;
import li.cil.oc2.manual.api.util.Constants;
import li.cil.oc2.manual.api.util.MarkdownManualRegistryEntry;
import li.cil.oc2.manual.api.util.PathUtils;
import li.cil.oc2.manual.client.document.Strings;
import li.cil.oc2.manual.client.util.ManualRegistry;
import li.cil.oc2.manual.client.util.RegistryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple implementation of the {@link ManualModel} interface which should cover most use-cases.
 */
public class Manual implements ManualModel {
    /**
     * The magic first characters indicating a redirect in a document, with the target path following.
     */
    private static final String REDIRECT_PRAGMA = "#redirect ";

    // ----------------------------------------------------------------------- //

    /**
     * The current navigation history for this manual.
     */
    protected final List<History> history = new ArrayList<>();

    /**
     * The current index in the navigation history. We keep pages we came back from
     * at the back of the list to allow navigating back forwards, retaining the stored
     * scroll offset of these pages.
     */
    protected int historyIndex;

    // ----------------------------------------------------------------------- //

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // Start-page/reset hooks are part of the upstream extension API.
    public Manual() {
        reset();
    }

    // ----------------------------------------------------------------------- //

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> pathFor(final ItemStack stack) {
        return find(Constants.PATH_PROVIDER_REGISTRY, provider -> provider.pathFor(stack));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> pathFor(final Level world, final BlockPos pos, final Direction face) {
        return find(Constants.PATH_PROVIDER_REGISTRY, provider -> provider.pathFor(world, pos, face));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Document> documentFor(final String path) {
        final String language = Minecraft.getInstance().getLanguageManager().getSelected();
        final Optional<Document> document = documentFor(path.replace(LANGUAGE_KEY, language), language, new LinkedHashSet<>());
        return document.isPresent() ? document : documentFor(path.replace(LANGUAGE_KEY, FALLBACK_LANGUAGE), FALLBACK_LANGUAGE, new LinkedHashSet<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ContentRenderer> imageFor(final String path) {
        return find(Constants.RENDERER_PROVIDER_REGISTRY, provider -> provider.getRenderer(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Tab> getTabs() {
        final ManualRegistry<Tab> registry = RegistryUtils.get(Constants.TAB_REGISTRY);
        return StreamSupport.stream(registry.spliterator(), false)
            .filter(tab -> matches(registry, tab))
            .sorted()
            .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------- //

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        history.clear();
        history.add(createHistoryEntry(StringUtils.stripStart(getStartPage(), "/")));
        historyIndex = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void push(final String path) {
        if (path.startsWith("/")) throw new IllegalArgumentException("Path must not start with a slash.");
        if (Objects.equals(history.get(historyIndex).path, path)) {
            return;
        }

        // Try to re-use "future" entry.
        if (history.size() > historyIndex + 1 && Objects.equals(history.get(historyIndex + 1).path, path)) {
            historyIndex++;
            return;
        }

        // Remove "future" entries we kept to navigate back forwards.
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }

        history.add(createHistoryEntry(path));
        historyIndex++;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public boolean pop() {
        if (historyIndex <= 0) {
            return false;
        }

        historyIndex--;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String peek() {
        return history.get(historyIndex).path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(final String path) {
        return PathUtils.resolve(peek(), path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getUserData(final Class<T> type) {
        return Optional.ofNullable((T) history.get(historyIndex).userData.get(type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void setUserData(final T value) {
        history.get(historyIndex).userData.put(value.getClass(), value);
    }

    // ----------------------------------------------------------------------- //

    protected String getStartPage() {
        return LANGUAGE_KEY + "/index.md";
    }

    protected History createHistoryEntry(final String path) {
        return new History(path);
    }

    protected <P extends MarkdownManualRegistryEntry, R> Optional<R> find(final ResourceKey<Registry<P>> key, final Function<P, Optional<R>> lookup) {
        final ManualRegistry<P> registry = RegistryUtils.get(key);
        return registry.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(provider -> matches(registry, provider))
            .sorted().map(lookup).filter(Optional::isPresent).findFirst().flatMap(x -> x);
    }

    protected <T extends MarkdownManualRegistryEntry> boolean matches(final ManualRegistry<T> registry, final T entry) {
        return switch (entry.matches(this)) {
            case PASS -> {
                final Optional<ResourceKey<T>> entryId = registry.getKey(entry);
                final Optional<ResourceKey<ManualModel>> manualId = RegistryUtils.get(Constants.MANUAL_REGISTRY).getKey(this);
                yield entryId.isPresent() && manualId.isPresent() && Objects.equals(RegistryUtils.id(entryId.get()).getNamespace(), RegistryUtils.id(manualId.get()).getNamespace());
            }
            case MATCH -> true;
            case MISMATCH -> false;
        };
    }

    /**
     * Loads the document from the specified path, in the specified language.
     * <p>
     * This method may perform additional processing on the loaded documents. The default implementation
     * will resolve redirects and trim leading and trailing blank lines.
     *
     * @param path     the path of the document to load.
     * @param language the language of the document to load.
     * @param seen     a set of already seen documents in the loading process, used to break cycles in redirects.
     * @return the loaded document.
     */
    protected Optional<Document> documentFor(final String path, final String language, final Set<String> seen) {
        if (!seen.add(path)) {
            final List<String> message = new ArrayList<>();
            message.add(Strings.getRedirectionLoopText());
            message.addAll(seen);
            message.add(path);
            return Optional.of(new Document(message));
        }

        return find(Constants.DOCUMENT_PROVIDER_REGISTRY, provider -> provider.getDocument(path, language)).flatMap(document -> {
            // Read first line only to check for redirect.
            final List<String> lines = document.getLines();
            if (!lines.isEmpty() && lines.get(0).toLowerCase(Locale.ROOT).startsWith(REDIRECT_PRAGMA)) {
                final String redirectPath = lines.get(0).substring(REDIRECT_PRAGMA.length()).trim();
                return documentFor(PathUtils.resolve(path, redirectPath), language, seen);
            }

            // Trim leading and trailing blank lines. Mostly for the trailing ones, to avoid scrolling being weird.
            while (!lines.isEmpty() && StringUtils.isWhitespace(lines.get(0))) {
                lines.remove(0);
            }
            while (!lines.isEmpty() && StringUtils.isWhitespace(lines.get(lines.size() - 1))) {
                lines.remove(lines.size() - 1);
            }
            return Optional.of(document);
        });
    }

    // --------------------------------------------------------------------- //

    protected static class History {
        public final String path;
        @SuppressWarnings("PMD.UseConcurrentHashMap") // Client thread only.
        public final Map<Class<?>, Object> userData = new HashMap<>();

        public History(final String path) {
            this.path = path;
        }
    }
}
