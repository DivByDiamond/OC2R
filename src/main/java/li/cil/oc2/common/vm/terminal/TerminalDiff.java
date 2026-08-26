package li.cil.oc2.common.vm.terminal;

import io.netty.buffer.ByteBuf;
import java.nio.BufferUnderflowException;
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
 * <p>Row payload is a sequence of runs of identical cells. Each run is an unsigned varint
 * run length followed by one cell: varint codepoint (masked to 21 bits, full Unicode),
 * attribute byte, then optional fields selected by attribute bits. Colors are packed into
 * 27 bits (3-bit mode ordinal low, then 8-bit R/G/B — matching the palette-index meaning
 * for non-truecolor modes that {@link ColorData#toInt()} resolves against) and emitted as
 * varints only when they differ from the defaults; a zero style is implied. A typical
 * single-character echo therefore costs a handful of bytes per changed row instead of the
 * former fixed 37 bytes per cell.
 */
public final class TerminalDiff {
    // Attribute byte: which non-default fields follow the codepoint.
    private static final int ATTR_FG_EXPLICIT = 1;
    private static final int ATTR_BG_EXPLICIT = 1 << 1;
    private static final int ATTR_STYLE_EXPLICIT = 1 << 2;

    // Codepoints are Unicode (21 bits); higher bits cannot occur and are masked out.
    private static final int CODEPOINT_MASK = 0x1FFFFF;

    // 3-byte codepoint varint + attribute byte + two 4-byte color varints + style byte.
    private static final int MAX_CELL_BYTES = 13;
    // Varint run length for the widest supported row.
    private static final int MAX_RUN_HEADER_BYTES = 5;

    private static final int DEFAULT_FOREGROUND_PACKED = packColor(TerminalColors.DEFAULT_FOREGROUND_COLOR);
    private static final int DEFAULT_BACKGROUND_PACKED = packColor(TerminalColors.DEFAULT_BACKGROUND_COLOR);

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
        // Consume the bell flag: it must fire exactly once per emitted diff, otherwise
        // every subsequent diff would replay the bell until the next one arrives.
        final boolean bell = terminal.hasPendingBell;
        terminal.hasPendingBell = false;
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
                bell,
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
        final ByteBuffer buf = ByteBuffer.allocate(
                        terminal.width * (MAX_CELL_BYTES + MAX_RUN_HEADER_BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        final int base = row * terminal.width;
        final int cells = Math.max(0, Math.min(terminal.width, terminal.buffer.length - base));
        int x = 0;
        while (x < cells) {
            final int index = base + x;
            final Cell cell =
                    readCell(terminal, alt, index); // NOPMD: per-cell state
            int run = 1;
            while (x + run < cells && cell.equals(readCell(terminal, alt, base + x + run))) {
                run++;
            }
            putVarInt(buf, run);
            writeCell(buf, cell);
            x += run;
        }
        return Arrays.copyOf(buf.array(), buf.position());
    }

    private static Cell readCell(final Terminal terminal, final boolean alt, final int index) {
        if (alt) {
            return new Cell(
                    terminal.altBuffer[index],
                    terminal.altColors[index],
                    terminal.altColorsBackground[index],
                    terminal.altStyles[index]);
        }
        return new Cell(
                terminal.buffer[index], terminal.colors[index], terminal.colorsBackground[index], terminal.styles[index]);
    }

    private static void writeCell(final ByteBuffer buf, final Cell cell) {
        putVarInt(buf, cell.codepoint() & CODEPOINT_MASK);
        final int fgPacked = packColor(cell.foreground());
        final int bgPacked = packColor(cell.background());
        int attr = 0;
        if (fgPacked != DEFAULT_FOREGROUND_PACKED) attr |= ATTR_FG_EXPLICIT;
        if (bgPacked != DEFAULT_BACKGROUND_PACKED) attr |= ATTR_BG_EXPLICIT;
        if (cell.style() != TerminalColors.DEFAULT_STYLE) attr |= ATTR_STYLE_EXPLICIT;
        buf.put((byte) attr);
        if ((attr & ATTR_FG_EXPLICIT) != 0) putVarInt(buf, fgPacked);
        if ((attr & ATTR_BG_EXPLICIT) != 0) putVarInt(buf, bgPacked);
        if ((attr & ATTR_STYLE_EXPLICIT) != 0) buf.put(cell.style());
    }

    /** Packs mode ordinal (3 bits) plus 8-bit R/G/B into a single varint-friendly value. */
    private static int packColor(final ColorData color) {
        return (color.Mode.ordinal() & 0x7)
                | (color.R & 0xFF) << 3
                | (color.G & 0xFF) << 11
                | (color.B & 0xFF) << 19;
    }

    private static ColorData unpackColor(final int packed) {
        final ColorMode[] modes = ColorMode.values();
        final int ordinal = packed & 0x7;
        final ColorMode mode = ordinal < modes.length ? modes[ordinal] : MODE_ORDINAL_FALLBACK;
        return new ColorData((packed >>> 3) & 0xFF, (packed >>> 11) & 0xFF, (packed >>> 19) & 0xFF, mode);
    }

    private static void putVarInt(final ByteBuffer buf, final int value) {
        int v = value;
        while ((v & ~0x7F) != 0) {
            buf.put((byte) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        buf.put((byte) v);
    }

    private static int getVarInt(final ByteBuffer buf) {
        int value = 0;
        for (int shift = 0; shift <= 28; shift += 7) {
            final byte b = buf.get();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
        }
        throw new BufferUnderflowException();
    }

    /** Immutable snapshot of one screen cell used for run detection and serialization. */
    private record Cell(int codepoint, ColorData foreground, ColorData background, byte style) {

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof final Cell other)) {
                return false;
            }
            return codepoint == other.codepoint
                    && style == other.style
                    && packColor(foreground) == packColor(other.foreground)
                    && packColor(background) == packColor(other.background);
        }

        @Override
        public int hashCode() {
            return codepoint ^ (packColor(foreground) * 31) ^ (packColor(background) * 961) ^ style;
        }
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
        try {
            deserializeRowCells(terminal, alt, base, buf);
        } catch (final BufferUnderflowException ignored) {
            // Truncated row payload: keep whatever cells were decoded.
        }
    }

    private static void deserializeRowCells(
            final Terminal terminal, final boolean alt, final int base, final ByteBuffer buf) {
        int x = 0;
        while (x < terminal.width && buf.hasRemaining()) {
            final int run = getVarInt(buf);
            if (run < 1) {
                return; // malformed run length: stop rather than desync
            }
            final DecodedCell cell = readCell(buf);
            x = fillRun(terminal, alt, base, x, run, cell);
            if (x < 0) {
                return; // width race guard: abort decoding this snapshot entirely
            }
        }
    }

    /** Reads one decoded cell from the snapshot stream, expanding explicit-attribute bits. */
    private static DecodedCell readCell(final ByteBuffer buf) {
        final int ch = getVarInt(buf);
        final int attr = buf.get() & 0xFF;
        final ColorData fg =
                (attr & ATTR_FG_EXPLICIT) != 0 ? unpackColor(getVarInt(buf)) : TerminalColors.DEFAULT_FOREGROUND_COLOR.copy();
        final ColorData bg =
                (attr & ATTR_BG_EXPLICIT) != 0 ? unpackColor(getVarInt(buf)) : TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();
        final byte style =
                (attr & ATTR_STYLE_EXPLICIT) != 0 ? buf.get() : TerminalColors.DEFAULT_STYLE;
        return new DecodedCell(ch, fg, bg, style);
    }

    private record DecodedCell(int ch, ColorData fg, ColorData bg, byte style) {}

    /**
     * Writes a cell run starting at column {@code x}. Returns the new column, or -1
     * when the width race guard tripped (snapshot must not be decoded any further).
     */
    private static int fillRun(
            final Terminal terminal,
            final boolean alt,
            final int base,
            final int startX,
            final int runLength,
            final DecodedCell cell) {
        int x = startX;
        for (int i = 0; i < runLength && x < terminal.width; i++, x++) {
            final int index = base + x;
            if (index >= terminal.buffer.length) {
                return -1;
            }
            storeCell(terminal, alt, index, cell.ch(), cell.fg(), cell.bg(), cell.style());
        }
        return x;
    }

    private static void storeCell(
            final Terminal terminal,
            final boolean alt,
            final int index,
            final int ch,
            final ColorData fg,
            final ColorData bg,
            final byte style) {
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
