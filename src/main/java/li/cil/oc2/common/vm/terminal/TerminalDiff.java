package li.cil.oc2.common.vm.terminal;

import io.netty.buffer.ByteBuf;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
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
@SuppressWarnings("PMD.CyclomaticComplexity") // class-aggregate complexity is high because the diff touches every facet of terminal state (rows, cursor, modes, bell, palette); each is a small focused method
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
    private static final int PALETTE_SIZE = 256;
    // Shift ops ride the wire as flat quintuples of resolved memmove geometry.
    private static final int SHIFT_OP_FIELDS = 5;

    /**
     * Terminal snapshot transferred from server to client.
     *
     * @param rows     absolute buffer row indices (alt-buffer: screen rows 0..23)
     * @param rowData  serialized cell data, one array per entry of {@code rows}
     * @param shiftOps resolved main-buffer scrollback shift operations since the last diff,
     *                 quintuples (copySrcRow, copyDstRow, copyRows, blankStartRow, blankRows).
     *                 A shift moves content at absolute indices that need not be screen-visible;
     *                 the client replays the identical memmove so its scrollback copy (above the
     *                 visible window) stays exact.
     */
    public record Snapshot(
            boolean reset,
            int width,
            int height,
            boolean altBuffer,
            int[] rows,
            byte[][] rowData,
            int[] shiftOps,
            int cursorX,
            int cursorY,
            int lastRowToDisplay,
            int lastRowToDisplayMax,
            int cursorMode,
            boolean cursorVisible,
            boolean bell,
            long inputModes,
            int[] palette) {
        public Snapshot {
            // Wire payload arrays are owned copies: the codec and apply treat snapshots as
            // immutable, and defensive copies keep the record clean without extending the
            // SpotBugs baseline. rowData's outer array is copied (the per-row payloads are
            // never mutated in place by either side). Trivial cost at diff rates.
            rows = rows.clone();
            rowData = rowData.clone();
            shiftOps = shiftOps.clone();
            if (palette != null) {
                palette = palette.clone();
            }
        }

        @Override
        public int[] shiftOps() {
            return shiftOps.clone();
        }
    }

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
        final int[] rows = full ? fullRefreshRows(terminal, dirty.rows()) : dirty.rows();
        return build(terminal, full, false, dirty.shiftOps(), rows);
    }

    /**
     * §46 (chunk 1/3 sweep tails): a full refresh always re-ships the visible window, but rows
     * explicitly marked dirty OUTSIDE it — erase-scrollback ({@code ED 3 J}, which marks every
     * buffer row via {@link Terminal#markAllBufferRowsDirty}) or a shift-op backlog overflow
     * (see {@link Terminal#recordNetworkShift}) — must ride along too. Previously {@code capture}
     * discarded {@code dirty.rows()} outright on a full refresh in favor of just the visible
     * window, so the client's off-screen scrollback copy silently diverged from server state and
     * never self-healed (documented as accepted/bounded degradation in todo.md §46 — this closes
     * it: the extra rows now ship with the same full-refresh diff instead of being dropped).
     */
    private static int[] fullRefreshRows(final Terminal terminal, final int... extraRows) {
        final int[] visible = visibleWindowRows(terminal);
        if (extraRows.length == 0) {
            return visible;
        }
        final BitSet union = new BitSet();
        for (final int row : visible) {
            union.set(row);
        }
        for (final int row : extraRows) {
            union.set(row);
        }
        return union.stream().toArray();
    }

    /** Builds a full-screen snapshot flagged as reset (used after VM restarts / RIS). */
    public static Snapshot captureFull(final Terminal terminal) {
        return build(terminal, true, true, new int[0], visibleWindowRows(terminal));
    }

    private static Snapshot build(
            final Terminal terminal, final boolean reset, final boolean forcePalette, final int[] shiftOps,
            final int... rows) {
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        // Consume the bell flag: it must fire exactly once per emitted diff, otherwise
        // every subsequent diff would replay the bell until the next one arrives.
        final boolean bell = terminal.hasPendingBell;
        terminal.hasPendingBell = false;
        // Ship the palette when it changed since the last diff, or always on a reset snapshot —
        // captureFull after RIS, but ALSO any full-refresh capture (reset == full): a client
        // that missed the original OSC 4 change (opened/tracked the computer later) rebuilds
        // its screen from that snapshot and must not render a stale default palette.
        final int[] palette = terminal.consumePaletteDirty(forcePalette || reset);
        return new Snapshot(
                reset,
                terminal.width,
                terminal.height,
                alt,
                rows,
                serializeRows(terminal, alt, rows),
                shiftOps,
                terminal.x,
                terminal.y,
                terminal.lastRowToDisplay,
                terminal.lastRowToDisplayMax,
                terminal.cursorMode,
                terminal.currentPrivateModeState.DECTCEM,
                bell,
                packInputModes(terminal.currentPrivateModeState),
                palette);
    }

    private static int[] visibleWindowRows(final Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            final int[] rows = new int[terminal.height];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = i;
            }
            return rows;
        }
        // Main buffer: the currently displayed scrollback window.
        final int first = Math.max(0, terminal.lastRowToDisplay - terminal.height);
        final int count = terminal.height * Terminal.SCROLL_BACK_COUNT - first;
        final int[] rows = new int[Math.min(terminal.height, count)];
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

    /** Packs stable mode id (3 bits) plus 8-bit R/G/B into a single varint-friendly value. */
    private static int packColor(final ColorData color) {
        return (modeToId(color.mode) & 0x7)
                | (color.r & 0xFF) << 3
                | (color.g & 0xFF) << 11
                | (color.b & 0xFF) << 19;
    }

    private static ColorData unpackColor(final int packed) {
        final int id = packed & 0x7;
        final ColorMode mode = idToMode(id);
        return new ColorData((packed >>> 3) & 0xFF, (packed >>> 11) & 0xFF, (packed >>> 19) & 0xFF, mode);
    }

    private static int modeToId(final ColorMode mode) {
        return switch (mode) {
            case SIXTEEN_COLOR -> 0;
            case TWO_FIFTY_SIX_COLOR -> 1;
            case TRUE_COLOR -> 2;
            case SIXTEEN_COLOR_BRIGHT -> 3;
            case DEFAULT_BACKGROUND -> 4;
            case DEFAULT_FOREGROUND -> 5;
        };
    }

    private static ColorMode idToMode(final int id) {
        return switch (id) {
            case 0 -> ColorMode.SIXTEEN_COLOR;
            case 1 -> ColorMode.TWO_FIFTY_SIX_COLOR;
            case 2 -> ColorMode.TRUE_COLOR;
            case 3 -> ColorMode.SIXTEEN_COLOR_BRIGHT;
            case 4 -> ColorMode.DEFAULT_BACKGROUND;
            case 5 -> ColorMode.DEFAULT_FOREGROUND;
            default -> MODE_ORDINAL_FALLBACK;
        };
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

    /**
     * Applies a snapshot to a local (client-side) terminal copy and marks everything dirty.
     *
     * <p>Order matters and mirrors the server's chronology: shift operations first (they
     * reference PRE-resize absolute rows), then geometry, then row payloads (post-resize
     * absolute rows). Known bounded divergence: the client re-runs resizeHeight's relayout
     * with its OWN (previous snapshot's) cursor, and the shrink anchor is cursor-relative.
     * A remote shrink can therefore anchor the off-screen scrollback rows differently than
     * the server. The visible window always re-ships at absolute row indices and the cursor
     * is set authoritatively below, so display state is exact after this call; the mismatch
     * lives only in scrollback above the window and self-heals as rows scroll into view.
     * (Setting the SHIPPED cursor before the resize would be worse: that's the POST-resize
     * row, and the anchor formula needs the pre-resize one, which isn't on the wire.)
     *
     * <p>View ownership: the scroll position while a user is scrolled back through scrollback
     * is a PER-VIEWER preference the server cannot own. The shipped {@code lastRowToDisplay}
     * (the server's write-window view, always glued to the bottom) therefore only drives the
     * client's view when the client is glued too; a scrolled-back client keeps its absolute
     * content position — window slides don't move content, so the view stays put, and shift
     * operations move the content, so the view follows them (the same anchoring semantics as
     * xterm/tmux scrollback). Otherwise every incoming diff would yank a scrolled-back view
     * back to the bottom, making scrollback review impossible while output streams.
     */
    public static void apply(final Terminal terminal, final Snapshot s) { // NOPMD: NPath — ops replay + view-anchoring branches over the snapshot fields, same shape as Terminal.resizeHeight
        // The user's scrollback position BEFORE this snapshot, for the anchoring rule below.
        final int preLrd = terminal.lastRowToDisplay;
        final int preLrdMax = terminal.lastRowToDisplayMax;

        int anchorLrd = preLrd;
        // Ops replay ONLY in incremental windows — this guard is load-bearing, not an
        // optimization. A server-side resize arms a full refresh, so ops recorded after that
        // resize ride in the full window and reference POST-resize geometry the client has
        // not applied yet (it resizes below, in this same call). Replaying them against the
        // pre-resize client buffer would move above-window scrollback wrongly; skipping is
        // always safe because the full window repaints the visible rows, and ops in later
        // incremental windows replay against matching post-resize geometry on both sides.
        if (!s.reset() && s.shiftOps().length > 0) {
            anchorLrd = applyShiftOps(terminal, s.shiftOps(), preLrd);
        }

        // Geometry first, rows after — the deserialize guards evaluate against post-resize
        // dims. Known bounded divergence: the client re-runs resizeHeight's relayout with its
        // OWN (previous snapshot's) cursor, and the shrink anchor is cursor-relative. A remote
        // shrink can therefore anchor the off-screen scrollback rows differently than the
        // server. The visible window always re-ships at absolute row indices and the cursor is
        // set authoritatively below, so display state is exact after this call; the mismatch
        // lives only in scrollback above the window and self-heals as rows scroll into view.
        // (Setting the SHIPPED cursor before the resize would be worse: that's the POST-resize
        // row, and the anchor formula needs the pre-resize one, which isn't on the wire.)
        if (terminal.width != s.width()) {
            terminal.resizeWidth(s.width());
        }
        if (terminal.height != s.height()) {
            terminal.resizeHeight(s.height());
        }

        final boolean alt = s.altBuffer();
        if (s.reset()) {
            clearBuffers(terminal);
            terminal.scrollFirst = 0;
            terminal.scrollLast = terminal.height - 1;
        }
        setAltBufferEnabled(terminal, alt);

        // Guard the rows/rowData pairing: a malformed snapshot with mismatched lengths must not
        // AIOOBE on the client network thread (see §37). The codec already clamps rowCount to a
        // non-negative bounded value; this handles the residual mismatch (e.g. rows.length !=
        // rowData.length) by decoding only the overlap — missing rows stay as they were, extra
        // rowData is ignored (stream integrity was already preserved by reading it).
        final int rowPairs = Math.min(s.rows().length, s.rowData().length);
        for (int i = 0; i < rowPairs; i++) {
            deserializeRow(terminal, alt, s.rows()[i], s.rowData()[i]);
        }

        // Clamp the scroll-window indices into the (already-resized) geometry, mirroring the
        // palette guard below: they're raw wire values. A malformed snapshot with
        // lastRowToDisplayMax < height would leave the client with a broken window invariant —
        // its own later resizeHeight would compute a relayout span below newHeight and
        // Math.clamp would throw on the client network thread. Max clamps first so the view
        // bottom never ends up above the view top.
        final int newLrdMax = Math.clamp(
                s.lastRowToDisplayMax(), terminal.height, terminal.height * Terminal.SCROLL_BACK_COUNT);
        terminal.lastRowToDisplayMax = newLrdMax;
        if (alt || s.reset() || preLrd >= preLrdMax) {
            // Glued (or alt, or a full reset): the view follows the shipped window bottom.
            terminal.lastRowToDisplay = Math.clamp(
                    s.lastRowToDisplay(), terminal.height, newLrdMax);
        } else {
            // User is scrolled back: anchor the view to its absolute content rows. Window
            // slides grow lrdMax without moving content — the view stays put and new rows
            // appear below it; shift ops moved the content, and applyShiftOps advanced the
            // anchor by the same memmoves, so this preserves what the user was reading.
            terminal.lastRowToDisplay = Math.clamp(anchorLrd, terminal.height, newLrdMax);
        }
        terminal.setCursorPos(s.cursorX(), s.cursorY());
        terminal.cursorMode = s.cursorMode();
        terminal.currentPrivateModeState.DECTCEM = s.cursorVisible();
        applyInputModes(terminal.currentPrivateModeState, s.inputModes());
        if (s.bell()) {
            terminal.hasPendingBell = true;
        }
        // Apply a synced palette (clone so the client's array stays independent of the server's,
        // matching the per-instance discipline). Null = unchanged this diff. Guarded to the
        // canonical 256-entry xterm palette to avoid AIOOBE on malformed payloads.
        if (s.palette() != null && s.palette().length == PALETTE_SIZE) {
            terminal.palette256 = s.palette().clone();
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

    /**
     * Replays the snapshot's resolved shift operations against the local main buffer (the
     * identical memmoves the server performed), tracking where the user's view anchor — the
     * content row at {@code viewBottomLrd - 1} — moves with them. Blanked rows do NOT move
     * the anchor: content went blank under a stationary position. Returns the content-anchored
     * view bottom for the caller's clamp.
     *
     * <p>The ops reference PRE-resize geometry: the client's buffer has the same capacity at
     * this point (the resize below hasn't run), so a geometry that doesn't fit is a malformed
     * op — skipped, never an out-of-bounds access.
     */
    private static int applyShiftOps(final Terminal terminal, final int[] ops, final int viewBottomLrd) {
        final int mainRows = terminal.height * Terminal.SCROLL_BACK_COUNT;
        int lrd = viewBottomLrd;
        for (int i = 0; i + SHIFT_OP_FIELDS <= ops.length; i += SHIFT_OP_FIELDS) {
            final int copySrcRow = ops[i];
            final int copyDstRow = ops[i + 1];
            final int copyRows = ops[i + 2];
            final int blankStartRow = ops[i + 3];
            final int blankRows = ops[i + 4];
            if (copyRows < 0 || blankRows < 0
                    || copyRows > mainRows || blankRows > mainRows
                    || copySrcRow < 0 || copySrcRow > mainRows - copyRows
                    || copyDstRow < 0 || copyDstRow > mainRows - copyRows
                    || blankStartRow < 0 || blankStartRow > mainRows - blankRows) {
                continue;
            }
            terminal.bufferManager.applyResolvedShift(
                    copySrcRow, copyDstRow, copyRows, blankStartRow, blankRows);
            final int viewBottom = lrd - 1;
            if (viewBottom >= copySrcRow && viewBottom < copySrcRow + copyRows) {
                lrd += copyDstRow - copySrcRow;
            }
        }
        return lrd;
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
        if (data == null
                || row < 0
                || (alt ? row >= terminal.height : row >= terminal.height * Terminal.SCROLL_BACK_COUNT)) {
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

    private static final int PROTOCOL_VERSION = 1;

    private static void writeSnapshot(final Snapshot s, final ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, PROTOCOL_VERSION);
        buf.writeBoolean(s.reset());
        ByteBufCodecs.VAR_INT.encode(buf, s.width());
        ByteBufCodecs.VAR_INT.encode(buf, s.height());
        buf.writeBoolean(s.altBuffer());
        writeByteArray(buf, encodeInts(s.rows()));
        ByteBufCodecs.VAR_INT.encode(buf, s.rowData().length);
        for (final byte[] row : s.rowData()) {
            writeByteArray(buf, row);
        }
        // Shift ops are resolved memmove geometry — quintuples, flat-encoded.
        writeByteArray(buf, encodeInts(s.shiftOps()));
        ByteBufCodecs.VAR_INT.encode(buf, s.cursorX());
        ByteBufCodecs.VAR_INT.encode(buf, s.cursorY());
        ByteBufCodecs.VAR_INT.encode(buf, s.lastRowToDisplay());
        ByteBufCodecs.VAR_INT.encode(buf, s.lastRowToDisplayMax());
        ByteBufCodecs.VAR_INT.encode(buf, s.cursorMode());
        buf.writeBoolean(s.cursorVisible());
        buf.writeBoolean(s.bell());
        ByteBufCodecs.VAR_LONG.encode(buf, s.inputModes());
        final int[] palette = s.palette();
        buf.writeBoolean(palette != null);
        if (palette != null) {
            writeByteArray(buf, encodeInts(palette));
        }
    }

    private static Snapshot readSnapshot(final ByteBuf buf) {
        final int version = ByteBufCodecs.VAR_INT.decode(buf);
        if (version != PROTOCOL_VERSION) {
            throw new IllegalArgumentException(
                    "unsupported terminal protocol version: " + version + " (expected " + PROTOCOL_VERSION + ")");
        }
        final boolean reset = buf.readBoolean();
        final int width = ByteBufCodecs.VAR_INT.decode(buf);
        final int height = ByteBufCodecs.VAR_INT.decode(buf);
        final boolean altBuffer = buf.readBoolean();
        final int[] rows = decodeInts(readByteArray(buf));
        final int rowCount = ByteBufCodecs.VAR_INT.decode(buf);
        // rowData entries pair with rows entries; a malformed count must not become a huge
        // allocation. A NEGATIVE count is rejected outright (raw VAR_INT, so a hostile -1
        // previously fell through to new byte[-1][] — a decoder-surface disconnect either
        // way, but as an unnamed NegativeArraySizeException instead of a diagnosed one).
        if (rowCount < 0) {
            throw new IllegalArgumentException("negative rowData count: " + rowCount);
        }
        // Bounded to rows.length: extras are consumed (stream integrity) and dropped,
        // missing entries decode as null (apply skips them).
        final int boundedCount = Math.min(rowCount, Math.max(rows.length, 0));
        final byte[][] rowData = new byte[boundedCount][];
        for (int i = 0; i < rowCount; i++) {
            final byte[] row = readByteArray(buf);
            if (i < boundedCount) {
                rowData[i] = row;
            }
        }
        final int[] shiftOps = decodeInts(readByteArray(buf));
        final int cursorX = ByteBufCodecs.VAR_INT.decode(buf);
        final int cursorY = ByteBufCodecs.VAR_INT.decode(buf);
        final int lastRowToDisplay = ByteBufCodecs.VAR_INT.decode(buf);
        final int lastRowToDisplayMax = ByteBufCodecs.VAR_INT.decode(buf);
        final int cursorMode = ByteBufCodecs.VAR_INT.decode(buf);
        final boolean cursorVisible = buf.readBoolean();
        final boolean bell = buf.readBoolean();
        final long inputModes = ByteBufCodecs.VAR_LONG.decode(buf);
        // Palette is written LAST (after inputModes) — read it last or every field above
        // decodes from the wrong offset. Repro: CodecRoundTripReproTest.
        final int[] palette = buf.readBoolean() ? decodeInts(readByteArray(buf)) : null;
        return new Snapshot(
                reset,
                width,
                height,
                altBuffer,
                rows,
                rowData,
                shiftOps,
                cursorX,
                cursorY,
                lastRowToDisplay,
                lastRowToDisplayMax,
                cursorMode,
                cursorVisible,
                bell,
                inputModes,
                palette);
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
