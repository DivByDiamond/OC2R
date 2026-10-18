package li.cil.oc2.manual;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ManualContentTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets");
    private static final Path DOCS = ASSETS.resolve("oc2r/doc");
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)]+)\\)");

    @Test
    void russianHasEveryEnglishPageAndEveryLocalLinkResolves() throws Exception {
        final Set<Path> english = pages("en_us");
        assertFalse(english.isEmpty());
        assertEquals(english, pages("ru_ru"), "Russian pages must not silently fall back to English");
        for (final String language : new String[]{"en_us", "ru_ru"}) {
            for (final Path relative : english) {
                final Path page = DOCS.resolve(language).resolve(relative);
                final String text = Files.readString(page);
                assertFalse(text.contains("TODO"), page + " is a placeholder");
                assertFalse(text.contains("НЕ ГОТОВО"), page + " is a placeholder");
                final var matcher = LINK.matcher(text);
                while (matcher.find()) {
                    final String target = matcher.group(1).split("#", 2)[0];
                    if (target.contains(":")) continue;
                    assertTrue(Files.isRegularFile(page.getParent().resolve(target).normalize()),
                            page + " has a broken local link: " + target);
                }
            }
        }
    }

    @Test
    void russianIncludesAllInterfaceAndManualWarningKeys() throws Exception {
        for (final String namespace : new String[]{"oc2r", "markdown_manual"}) {
            final Path languages = ASSETS.resolve(namespace).resolve("lang");
            final var english = JsonParser.parseString(Files.readString(languages.resolve("en_us.json"))).getAsJsonObject();
            final var russian = JsonParser.parseString(Files.readString(languages.resolve("ru_ru.json"))).getAsJsonObject();
            assertTrue(russian.keySet().containsAll(english.keySet()), namespace + " has missing Russian translations");
        }
    }

    private static Set<Path> pages(final String language) throws Exception {
        final Path directory = DOCS.resolve(language);
        try (var paths = Files.walk(directory)) {
            return paths.filter(path -> path.toString().endsWith(".md"))
                    .map(directory::relativize).collect(Collectors.toSet());
        }
    }
}
