package li.cil.oc2.common.vm.terminal;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server-authoritative terminal screen diff.
 *
 * <p>The server is the single owner of terminal state: it parses VT100 escapes and ships
 * only the changed buffer rows to clients ({@link Snapshot}). The client applies rows into
 * its local {@link Terminal} copy and re-renders; it never parses UART bytes itself.
 *
 * <p>Row payload (per cell, little-endian): int codepoint, int R, int G, int B, int mode
 * ordinal, byte style — 17 bytes per cell. Color channels keep their palette-index meaning
 * for non-truecolor modes (the renderer resolves them against its palettes).
 */
public final class TerminalDiff {
    // int codepoint + 2 × ColorData (R,G,B,mode) + byte style.
    private static final int CELL_BYTES = 37;
    private static final ColorMode MODE_ORDINAL_FALLBACK = ColorMode.TRUE_COLOR;

    /**
     * @param rows    absolute buffer row indices (alt-buffer: screen rows 0..23)
     * @param rowData serialized cell data, one array per entry of {@code rows}
     */
    public record Snapshot(
            boolean reset,
            int width,
            boolean altBuffer,
            int[] rows,
            byte[][] rowData,
            int cursorX,
            int cursorY,
            int lastRowToDisplay,
            int lastRowToDisplayMax,
            int cursorMode,
            boolean cursorVisible,
            boolean bell,
            long inputModes) {}

    /**
     * Private-mode flags that affect client-side rendering or input handling beyond the
     * synced cell data: screen inverse (DECSCNM), mouse reporting, application cursor
     * keys, bracketed paste, focus events. The server parses the escapes, so these must
     * travel with every diff.
     */
    private static final List<Predicate<PrivateModeState>> INPUT_MODE_GETTERS = List.of(
            m -> m.DECSCNM,
            m -> m.APPLICATION_SYNC,
            m -> m.DECCKM,
            m -> m.X10MM,
            m -> m.X11MM,
            m -> m.CELL_MOTION_MOUSE,
            m -> m.ALL_MOTION_MOUSE_TRACKING,
            m -> m.UTF8_MOUSE,
            m -> m.SGR_MOUSE,
            m -> m.URXVT_MOUSE,
            m -> m.SGR_MOUSE_PIXEL,
            m -> m.FOCUS_IN_FOCUS_OUT,
            m -> m.APPLICATION_ESC_MODE,
            m -> m.SET_BRACKETED_PASTE);

    private static final List<BiConsumer<PrivateModeState, Boolean>> INPUT_MODE_SETTERS = List.of(
            (m, v) -> m.DECSCNM = v,
            (m, v) -> m.APPLICATION_SYNC = v,
            (m, v) -> m.DECCKM = v,
            (m, v) -> m.X10MM = v,
            (m, v) -> m.X11MM = v,
            (m, v) -> m.CELL_MOTION_MOUSE = v,
            (m, v) -> m.ALL_MOTION_MOUSE_TRACKING = v,
            (m, v) -> m.UTF8_MOUSE = v,
            (m, v) -> m.SGR_MOUSE = v,
            (m, v) -> m.URXVT_MOUSE = v,
            (m, v) -> m.SGR_MOUSE_PIXEL = v,
            (m, v) -> m.FOCUS_IN_FOCUS_OUT = v,
            (m, v) -> m.APPLICATION_ESC_MODE = v,
            (m, v) -> m.SET_BRACKETED_PASTE = v);

    private static long packInputModes(final PrivateModeState state) {
        long bits = 0L;
        for (int i = 0; i < INPUT_MODE_GETTERS.size(); i++) {
            if (INPUT_MODE_GETTERS.get(i).test(state)) {
                bits |= 1L << i;
            }
        }
        return bits;
    }

    private static void applyInputModes(final PrivateModeState state, final long bits) {
        for (int i = 0; i < INPUT_MODE_SETTERS.size(); i++) {
            INPUT_MODE_SETTERS.get(i).accept(state, (bits & (1L << i)) != 0);
        }
    }

    /** Builds a diff from the terminal's accumulated network-dirty rows. */
    public static Snapshot capture(final Terminal terminal) {
        final Terminal.NetworkDirty dirty = terminal.consumeNetworkDirty();
        final boolean full = dirty.fullRefresh();
        return build(terminal, full, full ? visibleWindowRows(terminal) : dirty.rows());
    }

    /** Builds a full-screen snapshot flagged as reset (used after VM restarts / RIS). */
    public static Snapshot captureFull(final Terminal terminal) {
        return build(terminal, true, visibleWindowRows(terminal));
    }

    private static Snapshot build(final Terminal terminal, final boolean reset, final int... rows) {
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        return new Snapshot(
                reset,
                terminal.width,
                alt,
                rows,
                serializeRows(terminal, alt, rows),
                terminal.x,
                terminal.y,
                terminal.lastRowToDisplay,
                terminal.lastRowToDisplayMax,
                terminal.cursorMode,
                terminal.currentPrivateModeState.DECTCEM,
                terminal.hasPendingBell,
                packInputModes(terminal.currentPrivateModeState));
    }

    private static int[] visibleWindowRows(final Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            final int[] rows = new int[Terminal.HEIGHT];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = i;
            }
            return rows;
        }
        // Main buffer: the currently displayed scrollback window.
        final int first = Math.max(0, terminal.lastRowToDisplay - Terminal.HEIGHT);
        final int count = Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT - first;
        final int[] rows = new int[Math.min(Terminal.HEIGHT, count)];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = first + i;
        }
        return rows;
    }

    private static byte[][] serializeRows(final Terminal terminal, final boolean alt, final int... rows) {
        final byte[][] data = new byte[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            data[i] = serializeRow(terminal, alt, rows[i]);
        }
        return data;
    }

    private static byte[] serializeRow(final Terminal terminal, final boolean alt, final int row) {
        final ByteBuffer buf = ByteBuffer.allocate(terminal.width * CELL_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        final int base = row * terminal.width;
        for (int x = 0; x < terminal.width; x++) {
            final int index = base + x;
            if (index >= terminal.buffer.length) break; // width race guard
            final int ch = alt ? terminal.altBuffer[index] : terminal.buffer[index];
            final ColorData fg = alt ? terminal.altColors[index] : terminal.colors[index];
            final ColorData bg = alt ? terminal.altColorsBackground[index] : terminal.colorsBackground[index];
            final byte style = alt ? terminal.altStyles[index] : terminal.styles[index];
            buf.putInt(ch);
            putColor(buf, fg);
            putColor(buf, bg);
            buf.put(style);
        }
        return buf.array();
    }

    private static void putColor(final ByteBuffer buf, final ColorData color) {
        buf.putInt(color.R);
        buf.putInt(color.G);
        buf.putInt(color.B);
        buf.putInt(color.Mode.ordinal());
    }

    /** Applies a snapshot to a local (client-side) terminal copy and marks everything dirty. */
    public static void apply(final Terminal terminal, final Snapshot s) {
        if (terminal.width != s.width()) {
            terminal.setWidth(s.width());
        }

        final boolean alt = s.altBuffer();
        if (s.reset()) {
            clearBuffers(terminal);
            terminal.scrollFirst = 0;
            terminal.scrollLast = Terminal.HEIGHT - 1;
        }
        setAltBufferEnabled(terminal, alt);

        for (int i = 0; i < s.rows().length; i++) {
            deserializeRow(terminal, alt, s.rows()[i], s.rowData()[i]);
        }

        terminal.lastRowToDisplay = s.lastRowToDisplay();
        terminal.lastRowToDisplayMax = s.lastRowToDisplayMax();
        terminal.setCursorPos(s.cursorX(), s.cursorY());
        terminal.cursorMode = s.cursorMode();
        terminal.currentPrivateModeState.DECTCEM = s.cursorVisible();
        applyInputModes(terminal.currentPrivateModeState, s.inputModes());
        if (s.bell()) {
            terminal.hasPendingBell = true;
        }
        terminal.markAllDirty();
    }

    private static void setAltBufferEnabled(final Terminal terminal, final boolean alt) {
        // Mirror the aggregate renderer-facing flag; the individual switching modes that
        // produced it on the server are irrelevant for a display-only copy.
        terminal.currentPrivateModeState.ALT_BUFFER = alt;
        terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = false;
        terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = false;
    }

    private static void clearBuffers(final Terminal terminal) {
        Arrays.fill(terminal.buffer, ' ');
        Arrays.fill(terminal.altBuffer, ' ');
        fillColors(terminal.colors, TerminalColors.DEFAULT_FOREGROUND_COLOR);
        fillColors(terminal.colorsBackground, TerminalColors.DEFAULT_BACKGROUND_COLOR);
        fillColors(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR);
        fillColors(terminal.altColorsBackground, TerminalColors.DEFAULT_BACKGROUND_COLOR);
        Arrays.fill(terminal.styles, TerminalColors.DEFAULT_STYLE);
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
    }

    private static void fillColors(final ColorData[] colors, final ColorData color) {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = color.copy();
        }
    }

    private static void deserializeRow(
            final Terminal terminal, final boolean alt, final int row, final byte[] data) {
        if (row < 0
                || (alt ? row >= Terminal.HEIGHT : row >= Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT)) {
            return;
        }
        final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        final int base = row * terminal.width;
        for (int x = 0; x < terminal.width && buf.remaining() >= CELL_BYTES; x++) {
            final int index = base + x;
            if (index >= terminal.buffer.length) break;
            deserializeCell(terminal, alt, index, buf);
        }
    }

    private static void deserializeCell(
            final Terminal terminal, final boolean alt, final int index, final ByteBuffer buf) {
        final int ch = buf.getInt();
        final ColorData fg = readColor(buf); // NOPMD: per-cell state
        final ColorData bg = readColor(buf); // NOPMD: per-cell state
        final byte style = buf.get();
        if (alt) {
            terminal.altBuffer[index] = ch;
            terminal.altColors[index] = fg;
            terminal.altColorsBackground[index] = bg;
            terminal.altStyles[index] = style;
        } else {
            terminal.buffer[index] = ch;
            terminal.colors[index] = fg;
            terminal.colorsBackground[index] = bg;
            terminal.styles[index] = style;
        }
    }

    private static ColorData readColor(final ByteBuffer buf) {
        final int r = buf.getInt();
        final int g = buf.getInt();
        final int b = buf.getInt();
        final int ordinal = buf.getInt();
        final ColorMode[] modes = ColorMode.values();
        final ColorMode mode =
                ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : MODE_ORDINAL_FALLBACK;
        return new ColorData(r, g, b, mode); // NOPMD: per-cell state
    }

    public static final StreamCodec<ByteBuf, Snapshot> STREAM_CODEC =
            StreamCodec.ofMember(TerminalDiff::writeSnapshot, TerminalDiff::readSnapshot);

    private static void writeSnapshot(final Snapshot s, final ByteBuf buf) {
        buf.writeBoolean(s.reset());
        ByteBufCodecs.VAR_INT.encode(buf, s.width());
        buf.writeBoolean(s.altBuffer());
        writeByteArray(buf, encodeInts(s.rows()));
        ByteBufCodecs.VAR_INT.encode(buf, s.rowData().length);
        for (final byte[] row : s.rowData()) {
            writeByteArray(buf, row);
        }
        ByteBufCodecs.VAR_INT.encode(buf, s.cursorX());
        ByteBufCodecs.VAR_INT.encode(buf, s.cursorY());
        ByteBufCodecs.VAR_INT.encode(buf, s.lastRowToDisplay());
        ByteBufCodecs.VAR_INT.encode(buf, s.lastRowToDisplayMax());
        ByteBufCodecs.VAR_INT.encode(buf, s.cursorMode());
        buf.writeBoolean(s.cursorVisible());
        buf.writeBoolean(s.bell());
        ByteBufCodecs.VAR_LONG.encode(buf, s.inputModes());
    }

    private static Snapshot readSnapshot(final ByteBuf buf) {
        final boolean reset = buf.readBoolean();
        final int width = ByteBufCodecs.VAR_INT.decode(buf);
        final boolean altBuffer = buf.readBoolean();
        final int[] rows = decodeInts(readByteArray(buf));
        final int rowCount = ByteBufCodecs.VAR_INT.decode(buf);
        final byte[][] rowData = new byte[rowCount][];
        for (int i = 0; i < rowCount; i++) {
            rowData[i] = readByteArray(buf);
        }
        return new Snapshot(
                reset,
                width,
                altBuffer,
                rows,
                rowData,
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                buf.readBoolean(),
                buf.readBoolean(),
                ByteBufCodecs.VAR_LONG.decode(buf));
    }

    private static void writeByteArray(final ByteBuf buf, final byte[] data) {
        ByteBufCodecs.BYTE_ARRAY.encode(buf, data);
    }

    private static byte[] readByteArray(final ByteBuf buf) {
        return ByteBufCodecs.BYTE_ARRAY.decode(buf);
    }

    private static byte[] encodeInts(final int... values) {
        final ByteBuffer buf = ByteBuffer.allocate(values.length * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (final int value : values) {
            buf.putInt(value);
        }
        return buf.array();
    }

    private static int[] decodeInts(final byte[] data) {
        final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        final int[] values = new int[data.length / Integer.BYTES];
        for (int i = 0; i < values.length; i++) {
            values[i] = buf.getInt();
        }
        return values;
    }

    private TerminalDiff() {}
}
