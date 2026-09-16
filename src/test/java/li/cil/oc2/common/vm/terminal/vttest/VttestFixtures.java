package li.cil.oc2.common.vm.terminal.vttest;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Discovery and metadata for the vttest replay fixtures.
 *
 * <p>A fixture is a directory {@code vttest/<id>/} under the test resources containing
 * {@code page.properties}, {@code stream.bin}, {@code screen.expected} and optionally
 * {@code styles.expected}; the format spec lives in {@code src/test/resources/vttest/README.md}.
 *
 * <p>Discovery goes through the class loader instead of the source tree: gradle's {@code test}
 * copies {@code src/test/resources} into {@code build/resources/test}, so the classpath resource
 * {@code vttest} resolves to a real directory there and can simply be walked. (Packaged/jar
 * classpaths are not supported — tests always run from the exploded directory.)
 */
public final class VttestFixtures {
    static final String RESOURCE_ROOT = "vttest";
    static final String PROPERTIES_FILE = "page.properties";
    static final String STREAM_FILE = "stream.bin";
    static final String SCREEN_FILE = "screen.expected";
    static final String STYLES_FILE = "styles.expected";
    private static final String DEFAULT_GEOMETRY = "80x24";

    private VttestFixtures() {
    }

    /**
     * One fixture. {@code dir} points at the classpath copy (under {@code build/resources/test}),
     * which is what the replay reads; golden regeneration targets the source tree instead
     * (see {@link #sourceDir}).
     */
    public record Fixture(String id, Path dir, int width, int height, boolean xfail, String reason) {
        @Override
        public String toString() {
            return xfail ? id + " [xfail]" : id;
        }
    }

    static List<Fixture> discover() {
        Path root = fixturesRoot();
        List<Fixture> fixtures = new ArrayList<>();
        try (var dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve(PROPERTIES_FILE)))
                    .sorted()
                    .forEach(dir -> fixtures.add(load(dir)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan fixture root " + root, e);
        }
        return List.copyOf(fixtures);
    }

    private static Fixture load(Path dir) {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(dir.resolve(PROPERTIES_FILE), StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + dir.resolve(PROPERTIES_FILE), e);
        }
        String geometry = props.getProperty("geometry", DEFAULT_GEOMETRY);
        int sep = geometry.indexOf('x');
        if (sep <= 0 || sep == geometry.length() - 1) {
            throw new IllegalStateException("bad geometry '" + geometry + "' in " + dir);
        }
        final boolean xfail;
        String status = props.getProperty("status", "pass");
        switch (status) {
            case "pass" -> xfail = false;
            case "xfail" -> xfail = true;
            default -> throw new IllegalStateException("unknown status '" + status + "' in " + dir);
        }
        String dirName = fileNameOf(dir);
        requireFixtureFile(dir, STREAM_FILE, dirName);
        requireFixtureFile(dir, SCREEN_FILE, dirName);
        return new Fixture(
                props.getProperty("id", dirName),
                dir,
                Integer.parseInt(geometry.substring(0, sep).trim()),
                Integer.parseInt(geometry.substring(sep + 1).trim()),
                xfail,
                props.getProperty("reason", ""));
    }

    /**
     * Fails fast with a fixture-and-file-specific message when a required fixture file is
     * missing, instead of letting it surface later as an opaque {@code IOException} or a
     * generic {@code fail(...)} deep in the replay test body.
     */
    private static void requireFixtureFile(Path dir, String fileName, String dirName) {
        if (!Files.isRegularFile(dir.resolve(fileName))) {
            throw new IllegalStateException("fixture '" + dirName + "' missing " + fileName);
        }
    }

    private static String fileNameOf(Path dir) {
        Path fileName = dir.getFileName();
        if (fileName == null) {
            throw new IllegalStateException("fixture directory has no name: " + dir);
        }
        return fileName.toString();
    }

    static Path fixturesRoot() {
        URL url = VttestFixtures.class.getResource("/" + RESOURCE_ROOT);
        if (url == null) {
            throw new IllegalStateException("No '" + RESOURCE_ROOT + "' directory on the test classpath");
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unusable vttest resource URL " + url, e);
        }
    }

    /**
     * Source-tree directory a fixture's regenerated goldens are written to
     * ({@code -Dvttest.regen=true}); never the classpath copy, so regeneration stays visible
     * to git for review. Gradle runs test JVMs with the working directory set to the project
     * root, so {@code user.dir} is the authoritative anchor; the classpath-root walk-up is a
     * null-tolerant fallback for other launchers.
     */
    static Path sourceDir(Fixture fixture) {
        Path dirName = fixture.dir().getFileName();
        Path fromWorkDir = Paths.get(System.getProperty("user.dir"), "src", "test", "resources")
                .resolve(RESOURCE_ROOT).resolve(dirName);
        if (Files.isDirectory(fromWorkDir)) {
            return fromWorkDir;
        }
        // build/resources/test/vttest -> walk up until a src/main sibling is found; each
        // getParent() may be null, which only ends the walk (the work-dir path already
        // covered every gradle-launched case).
        Path candidate = fixturesRoot().getParent();
        while (candidate != null) {
            Path src = candidate.resolve("src").resolve("main");
            if (Files.isDirectory(src)) {
                return candidate.resolve("src").resolve("test").resolve("resources")
                        .resolve(RESOURCE_ROOT).resolve(dirName);
            }
            candidate = candidate.getParent();
        }
        return fromWorkDir;
    }
}
