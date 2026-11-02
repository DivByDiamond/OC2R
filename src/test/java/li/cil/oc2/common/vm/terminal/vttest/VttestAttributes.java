package li.cil.oc2.common.vm.terminal.vttest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

/** Optional oracle-authored row rendition and foreground/background golden grids. */
final class VttestAttributes {
    private VttestAttributes() {
    }

    static void compare(final VttestFixtures.Fixture fixture, final Terminal terminal,
                        final List<String> mismatches) throws IOException {
        for (final String file : List.of("lineattrs.expected", "foreground.expected", "background.expected")) {
            final Path golden = fixture.dir().resolve(file);
            if (!Files.isRegularFile(golden)) continue;
            final List<String> actual = snapshot(terminal, file);
            final List<String> expected = Files.readAllLines(golden);
            if (expected.size() != actual.size()) {
                mismatches.add(file + ": expected " + expected.size() + " rows, actual " + actual.size());
            }
            for (int row = 0; row < Math.min(expected.size(), actual.size()); row++) {
                if (!expected.get(row).equals(actual.get(row))) {
                    mismatches.add(file + " row " + row + ": expected " + expected.get(row)
                            + ", actual " + actual.get(row));
                }
            }
        }
    }

    static void writeGoldens(final VttestFixtures.Fixture fixture, final Terminal terminal) throws IOException {
        final Path directory = VttestFixtures.sourceDir(fixture);
        for (final String file : List.of("lineattrs.expected", "foreground.expected", "background.expected")) {
            if (Files.isRegularFile(directory.resolve(file))) {
                Files.write(directory.resolve(file), snapshot(terminal, file));
            }
        }
    }

    private static List<String> snapshot(final Terminal terminal, final String file) {
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        final List<String> rows = new ArrayList<>();
        final ColorData[] colors = "foreground.expected".equals(file)
                ? alt ? terminal.altColors : terminal.colors
                : alt ? terminal.altColorsBackground : terminal.colorsBackground;
        final StringBuilder line = new StringBuilder();
        for (int row = 0; row < terminal.height; row++) {
            final int absoluteRow = alt ? row : row + terminal.lastRowToDisplay - terminal.height;
            if ("lineattrs.expected".equals(file)) {
                final byte[] attributes = alt ? terminal.altLineAttrs : terminal.lineAttrs;
                rows.add(Byte.toString(attributes[absoluteRow]));
                continue;
            }
            final int offset = absoluteRow * terminal.width;
            line.setLength(0);
            for (int column = 0; column < terminal.width; column++) {
                if (column != 0) line.append('|');
                line.append(color(colors[offset + column], "foreground.expected".equals(file)));
            }
            rows.add(line.toString());
        }
        return rows;
    }

    private static String color(final ColorData color, final boolean foreground) {
        final int index = foreground ? color.r : color.g;
        return switch (color.mode) {
            case DEFAULT_FOREGROUND, DEFAULT_BACKGROUND -> "default";
            case SIXTEEN_COLOR -> "index:" + index;
            case SIXTEEN_COLOR_BRIGHT -> "index:" + (index + 8);
            case TWO_FIFTY_SIX_COLOR -> "index:" + index;
            case TRUE_COLOR -> String.format("#%06X", color.toInt());
        };
    }
}
