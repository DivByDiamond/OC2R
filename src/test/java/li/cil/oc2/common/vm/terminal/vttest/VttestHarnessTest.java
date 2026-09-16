package li.cil.oc2.common.vm.terminal.vttest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.RendererModel;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * vttest replay harness: feeds a fixture's recorded byte stream through the server-side
 * {@link Terminal} (the same {@code io.putOutput} path the VM firmware uses) and compares the
 * resulting visible screen grid against the committed goldens.
 *
 * <p>Fixtures and their format are documented in {@code src/test/resources/vttest/README.md}.
 * The {@code xfail} status ratchets both ways: an xfail fixture passes only while it still
 * mismatches, and matching its golden forces the status to be promoted, so implementing a
 * feature cannot leave a stale xfail behind and regressions cannot flip a pass silently.
 *
 * <p>{@code -Dvttest.regen=true} turns the replay into a golden authoring tool: it writes the
 * actual grid over {@code screen.expected} (and {@code styles.expected}, only if it already
 * exists) in the source tree. Regen output is a draft — for humans and AIs alike — and must be
 * reviewed before committing.
 */
public class VttestHarnessTest {
    private static final boolean REGEN = Boolean.getBoolean("vttest.regen");
    private static final int MAX_REPORTED_MISMATCHES = 20;

    static final class FixtureArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(
                ParameterDeclarations declarations, ExtensionContext context) {
            return VttestFixtures.discover().stream().map(Arguments::of);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ArgumentsSource(FixtureArguments.class)
    void replayFixture(final VttestFixtures.Fixture fixture) throws IOException {
        final Terminal terminal = new Terminal();
        if (terminal.height != fixture.height()) {
            terminal.resizeHeight(fixture.height());
        }
        if (terminal.width != fixture.width()) {
            terminal.setWidth(fixture.width());
        }

        // The renderer is not strictly needed for putOutput, but keeping one matches SGRTest and
        // the terminal's renderer set is weak — the local must stay strongly referenced.
        final RendererModel renderer = new DummyRenderer();
        terminal.renderers.add(renderer);

        final byte[] stream = Files.readAllBytes(fixture.dir().resolve(VttestFixtures.STREAM_FILE));
        terminal.io.putOutput(ByteBuffer.wrap(stream)); // one whole-file ByteBuffer drains fully

        final int[][] cells = visibleCells(terminal);
        final byte[][] styles = visibleStyles(terminal);

        if (REGEN) {
            if (fixture.xfail()) {
                System.out.println("[vttest] skipping regen for xfail fixture " + fixture.id() // NOPMD SystemPrintln: xfail diagnostics go to stdout, captured verbatim by the JUnit report
                        + ": its golden encodes the not-yet-correct output, and regenerating would"
                        + " bake today's wrong grid into the reference and flip the ratchet to a"
                        + " spurious 'now passes'. Promote the status to pass first.");
                return;
            }
            writeGoldens(fixture, cells, styles);
            return;
        }

        final List<String> mismatches = compare(fixture, cells, styles);
        if (fixture.xfail()) {
            if (mismatches.isEmpty()) {
                fail("fixture " + fixture.id() + " now passes — promote status to pass");
            }
            System.out.println("[vttest] xfail " + fixture.id() + ": " + mismatches.size() // NOPMD SystemPrintln: xfail diagnostics go to stdout, captured verbatim by the JUnit report
                    + " mismatch(es) as expected"
                    + (fixture.reason().isEmpty() ? "" : " (" + fixture.reason() + ")")
                    + "\n  " + summarize(mismatches));
        } else if (!mismatches.isEmpty()) {
            fail("fixture " + fixture.id() + ": " + mismatches.size()
                    + " mismatch(es):\n  " + summarize(mismatches));
        }
    }

    /**
     * Snapshot of the VISIBLE screen following the renderer's window formula
     * ({@code TerminalRenderer} and {@code Terminal.recordNetworkDirtyScreenRows}): main-buffer
     * screen row {@code y} lives at absolute buffer row {@code y + lastRowToDisplay - height};
     * with an alt-buffer mode active (1049h/47h/1047h) the separate {@code altBuffer} rows are
     * addressed directly. Fresh cells hold {@code ' '} (buffers are filled with spaces on
     * allocation), but codepoint 0 is normalized to {@code ' '} defensively.
     */
    private static int[][] visibleCells(final Terminal terminal) {
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        final int[] source = alt ? terminal.altBuffer : terminal.buffer;
        final int[][] cells = new int[terminal.height][terminal.width];
        for (int y = 0; y < terminal.height; y++) {
            final int row = alt ? y : y + terminal.lastRowToDisplay - terminal.height;
            System.arraycopy(source, row * terminal.width, cells[y], 0, terminal.width);
            for (int x = 0; x < terminal.width; x++) {
                if (cells[y][x] == 0) {
                    cells[y][x] = ' ';
                }
            }
        }
        return cells;
    }

    private static byte[][] visibleStyles(final Terminal terminal) {
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        final byte[] source = alt ? terminal.altStyles : terminal.styles;
        final byte[][] styles = new byte[terminal.height][terminal.width];
        for (int y = 0; y < terminal.height; y++) {
            final int row = alt ? y : y + terminal.lastRowToDisplay - terminal.height;
            System.arraycopy(source, row * terminal.width, styles[y], 0, terminal.width);
        }
        return styles;
    }

    private static List<String> compare(
            final VttestFixtures.Fixture fixture, final int[][] cells, final byte[][] styles) throws IOException {
        final List<String> mismatches = new ArrayList<>();
        compareCells(fixture, cells, mismatches);
        compareStyles(fixture, styles, mismatches);
        return mismatches;
    }

    private static void compareCells(
            final VttestFixtures.Fixture fixture, final int[][] cells, final List<String> mismatches)
            throws IOException {
        final Path screenGolden = fixture.dir().resolve(VttestFixtures.SCREEN_FILE);
        if (!Files.isRegularFile(screenGolden)) {
            fail("fixture " + fixture.id() + " has no " + VttestFixtures.SCREEN_FILE);
        }
        // Golden rows are UTF-8 text, one codepoint per cell. Missing trailing lines count as
        // all-blank, and so do cells past the end of a line (trailing spaces may be trimmed).
        final List<String> expectedLines = Files.readAllLines(screenGolden, StandardCharsets.UTF_8);
        for (int y = 0; y < cells.length; y++) {
            final int[] expected = y < expectedLines.size()
                    ? expectedLines.get(y).codePoints().toArray()
                    : new int[0]; // NOPMD AvoidInstantiatingObjectsInLoops: per-row codepoint decode, ~24 small arrays per fixture on a cold test path
            for (int x = 0; x < cells[y].length; x++) {
                final int want = x < expected.length ? expected[x] : ' ';
                final int got = cells[y][x];
                if (want != got) {
                    mismatches.add("row " + y + " col " + x + ": char " + formatCell(want)
                            + ", got " + formatCell(got));
                }
            }
        }
    }

    private static void compareStyles( // NOPMD CognitiveComplexity+CyclomaticComplexity: per-cell style compare with a malformed-token fallback; splitting further would scatter one comparison across more methods than it clarifies
            final VttestFixtures.Fixture fixture, final byte[][] styles, final List<String> mismatches)
            throws IOException {
        final Path stylesGolden = fixture.dir().resolve(VttestFixtures.STYLES_FILE);
        if (!Files.isRegularFile(stylesGolden)) {
            return;
        }
        // Styles are only compared when the golden exists; a missing line or trailing cells
        // count as style byte 0x00 (trailing all-00 cells may be trimmed). A malformed token
        // is reported as a cell mismatch instead of aborting the whole comparison.
        final List<String> styleLines = Files.readAllLines(stylesGolden, StandardCharsets.UTF_8);
        for (int y = 0; y < styles.length; y++) {
            final String[] tokens = styleLines.size() > y && !styleLines.get(y).isEmpty()
                    ? styleLines.get(y).split("\\|")
                    : new String[0]; // NOPMD AvoidInstantiatingObjectsInLoops: per-row token split, ~24 small arrays per fixture on a cold test path
            for (int x = 0; x < styles[y].length; x++) {
                final int got = styles[y][x] & 0xFF;
                if (x >= tokens.length) {
                    if (got != 0) {
                        mismatches.add("row " + y + " col " + x + ": style 00, got " + hex2(got));
                    }
                    continue;
                }
                final String token = tokens[x].trim();
                final int want;
                try {
                    want = Integer.parseInt(token, 16);
                } catch (NumberFormatException e) {
                    mismatches.add("row " + y + " col " + x + ": bad style token '" + token + "'");
                    continue;
                }
                if (want != got) {
                    mismatches.add("row " + y + " col " + x + ": style "
                            + hex2(want) + ", got " + hex2(got));
                }
            }
        }
    }

    private static void writeGoldens(
            final VttestFixtures.Fixture fixture, final int[][] cells, final byte[][] styles) throws IOException {
        final Path dir = VttestFixtures.sourceDir(fixture);
        Files.createDirectories(dir);

        final List<String> screenLines = new ArrayList<>();
        for (final int[] row : cells) {
            final StringBuilder line = new StringBuilder(); // NOPMD AvoidInstantiatingObjectsInLoops: regen is a cold authoring path; a builder per screen row is the point
            for (final int codepoint : row) {
                line.appendCodePoint(codepoint);
            }
            screenLines.add(trimTrailing(line, ' '));
        }
        final Path screenGolden = dir.resolve(VttestFixtures.SCREEN_FILE);
        writeLinesGolden(screenGolden, screenLines);
        System.out.println("[vttest] regenerated " + screenGolden); // NOPMD SystemPrintln: regen is a cold authoring path; the stdout notice is the point

        // Styles are opt-in: only refresh an existing golden, never create one.
        final Path stylesGolden = dir.resolve(VttestFixtures.STYLES_FILE);
        if (Files.isRegularFile(stylesGolden)) {
            final List<String> styleLines = new ArrayList<>();
            for (final byte[] row : styles) {
                final StringBuilder line = new StringBuilder(); // NOPMD AvoidInstantiatingObjectsInLoops: regen is a cold authoring path; a builder per screen row is the point
                for (int x = 0; x < row.length; x++) {
                    if (x > 0) {
                        line.append('|');
                    }
                    line.append(hex2(row[x] & 0xFF));
                }
                trimTrailingTokens(line, hex2(0));
                styleLines.add(line.toString());
            }
            writeLinesGolden(stylesGolden, styleLines);
            System.out.println("[vttest] regenerated " + stylesGolden); // NOPMD SystemPrintln: regen is a cold authoring path; the stdout notice is the point
        }
    }

    private static void writeLinesGolden(final Path target, final List<String> lines) throws IOException {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        final StringBuilder text = new StringBuilder();
        for (final String line : lines) {
            text.append(line).append('\n');
        }
        Files.writeString(target, text.toString(), StandardCharsets.UTF_8);
    }

    private static String trimTrailing(final StringBuilder line, final char blank) {
        int end = line.length();
        while (end > 0 && line.charAt(end - 1) == blank) {
            end--;
        }
        return line.substring(0, end);
    }

    private static void trimTrailingTokens(final StringBuilder line, final String blank) {
        while (line.length() >= blank.length()
                && line.substring(line.length() - blank.length()).equals(blank)) {
            line.setLength(line.length() - blank.length());
            if (line.length() > 0 && line.charAt(line.length() - 1) == '|') {
                line.setLength(line.length() - 1);
            }
        }
    }

    private static String summarize(final List<String> mismatches) {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_REPORTED_MISMATCHES, mismatches.size()); i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append(mismatches.get(i));
        }
        if (mismatches.size() > MAX_REPORTED_MISMATCHES) {
            text.append("\n(+").append(mismatches.size() - MAX_REPORTED_MISMATCHES).append(" more)");
        }
        return text.toString();
    }

    private static String formatCell(final int codepoint) {
        final String glyph = Character.isISOControl(codepoint) ? "?" : String.valueOf(Character.toChars(codepoint));
        return "'U+" + String.format("%04X", codepoint) + "' '" + glyph + "'";
    }

    private static String hex2(final int value) {
        return String.format("%02X", value);
    }

    private static final class DummyRenderer implements RendererModel {
        private final AtomicLong dirtyMask = new AtomicLong();

        @Override
        public AtomicLong getDirtyMask() {
            return dirtyMask;
        }

        @Override
        public void close() {
            dirtyMask.set(0L);
        }
    }
}
