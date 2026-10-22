package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.api.prefab.Manual;
import li.cil.oc2.manual.api.prefab.tab.TextureTab;
import li.cil.oc2.manual.api.provider.DocumentProvider;
import li.cil.oc2.manual.api.util.Constants;
import li.cil.oc2.manual.client.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ManualNavigationTest {
    private static final AtomicInteger IDS = new AtomicInteger();

    @Test
    void historyRetainsPageDataAndDropsAbandonedForwardHistory() {
        final Manual manual = new Manual();
        final String start = manual.peek();
        manual.push("en_us/block/computer.md");
        manual.setUserData(42);
        manual.push("en_us/networking.md");
        assertTrue(manual.pop());
        assertEquals(42, manual.getUserData(Integer.class).orElseThrow());
        assertEquals("en_us/item/index.md", manual.resolve("../item/index.md"));
        manual.push("en_us/robotics.md");
        assertTrue(manual.pop());
        assertEquals("en_us/block/computer.md", manual.peek());
        manual.reset();
        assertEquals(start, manual.peek());
        assertFalse(manual.pop());
        assertTrue(manual.getUserData(Integer.class).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> manual.push("/absolute.md"));
    }

    @Test
    void localRegistriesIsolateTabsAndDocumentsByManualNamespaceAndResolveRedirects() {
        final String namespace = "manual_test_" + IDS.incrementAndGet();
        final FixtureManual manual = new FixtureManual();
        RegistryUtils.get(Constants.MANUAL_REGISTRY)
                .register(ResourceLocation.fromNamespaceAndPath(namespace, "manual"), () -> manual);
        final TextureTab tab = new TextureTab("en_us/index.md", null, ResourceLocation.parse("test:icon"));
        RegistryUtils.get(Constants.TAB_REGISTRY).register(
                ResourceLocation.fromNamespaceAndPath(namespace, "tab"), () -> tab);
        RegistryUtils.get(Constants.TAB_REGISTRY).register(
                ResourceLocation.fromNamespaceAndPath(namespace + "_other", "tab"),
                () -> new TextureTab("wrong.md", null, ResourceLocation.parse("test:icon")));
        final List<Object> tabs = new ArrayList<>();
        manual.getTabs().forEach(tabs::add);
        assertEquals(List.of(tab), tabs);
        final DocumentProvider provider = (path, language) -> switch (path) {
            case "en_us/index.md" -> Optional.of(new Document(new ArrayList<>(List.of("#redirect block/computer.md"))));
            case "en_us/block/computer.md" -> Optional.of(new Document(new ArrayList<>(List.of("", "Computer", ""))));
            default -> Optional.empty();
        };
        RegistryUtils.get(Constants.DOCUMENT_PROVIDER_REGISTRY).register(
                ResourceLocation.fromNamespaceAndPath(namespace, "documents"), () -> provider);
        assertEquals(List.of("Computer"), manual.read("en_us/index.md").orElseThrow().getLines());
        assertTrue(manual.read("en_us/missing.md").isEmpty());
    }

    private static final class FixtureManual extends Manual {
        Optional<Document> read(final String path) {
            return documentFor(path, "en_us", new LinkedHashSet<>());
        }
    }
}
