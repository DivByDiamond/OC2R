package li.cil.oc2.common.vm.terminal;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.vm.terminal.buffer.TerminalBuffer;
import li.cil.oc2.common.vm.terminal.buffer.TerminalBufferWriter;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;
import li.cil.oc2.common.vm.terminal.escapes.apc.APCManager;
import li.cil.oc2.common.vm.terminal.escapes.csi.CSIManager;
import li.cil.oc2.common.vm.terminal.escapes.dcs.DCSManager;
import li.cil.oc2.common.vm.terminal.escapes.index.RIS;
import li.cil.oc2.common.vm.terminal.escapes.osc.OSCManager;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;
import li.cil.oc2.common.vm.terminal.render.RendererModel;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Serialized
public class Terminal {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 24;
    // Largest geometry the engine supports. MAX_HEIGHT is the long dirty-mask ceiling
    // (1L << row must reach every screen row, i.e. rows 0..63). MAX_WIDTH mirrors xterm's
    // 1..255 DECSCPP/DECSNLS parameter range. setWidth/resizeWidth/resizeHeight CLAMP to
    // these so every caller — the escape handlers AND TerminalDiff.apply on the client —
    // stays inside the mask arithmetic regardless of what a guest (or a malformed snapshot)
    // asks for. Handlers still ignore out-of-range params entirely (xterm's skip behavior);
    // the clamp here is the last line of defense, not the policy.
    public static final int MAX_HEIGHT = 64;
    public static final int MAX_WIDTH = 255;
    public static final int CHAR_WIDTH = 8;
    public static final int CHAR_HEIGHT = 16;

    public static final int STYLE_BOLD_MASK = 1;
    public static final int STYLE_DIM_MASK = 1 << 1;
    public static final int STYLE_UNDERLINE_MASK = 1 << 2;
    public static final int STYLE_BLINK_MASK = 1 << 3;
    public static final int STYLE_INVERT_MASK = 1 << 4;
    public static final int STYLE_HIDDEN_MASK = 1 << 5;
    public static final int STYLE_ITALIC_MASK = 1 << 6;
    public static final int STYLE_CROSSED_OUT_MASK = 1 << 7;

    // Per-line double-size attributes (ESC #3/#4/#5/#6). Stored per absolute buffer row
    // (main buffer: height*SCROLL_BACK_COUNT rows, alt buffer: height rows). Values are
    // LINE_ATTR_* below; all rows default to SINGLE. Double-height (DHL) is double-width
    // double-height per VT520, so TOP/BOTTOM both imply double-width.
    public static final byte LINE_ATTR_SINGLE = 0;
    public static final byte LINE_ATTR_DOUBLE_WIDTH = 1;
    public static final byte LINE_ATTR_DOUBLE_HEIGHT_TOP = 2;
    public static final byte LINE_ATTR_DOUBLE_HEIGHT_BOTTOM = 3;

    public ColorMode currentForegroundColorMode = ColorMode.DEFAULT_FOREGROUND;
    public ColorMode currentBackgroundColorMode = ColorMode.DEFAULT_BACKGROUND;
    public ColorData sixteenColor,
            sixteenColorBright,
            twoFiftySixColor,
            backgroundColor,
            foregroundColor;
    // Per-instance 256-color palette: OSC 4 redefines an entry, OSC 104 resets it. Each
    // Terminal holds its own copy so a palette change never bleeds across terminals (the
    // static defaults stay immutable). Transient — ceres re-inits via the no-arg
    // constructor -> RIS -> getDefaultPalette256(), same lifecycle as the buffers.
    public transient int[] palette256;
    public byte style;

    public static final int SCROLL_BACK_COUNT = 20;
    public transient ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    // DECCOLM dynamic width; setWidth reallocates buffers. Transient: re-inits to WIDTH on load.
    public transient int width = WIDTH;
    // DECSLPP dynamic height; resizeHeight reallocates buffers. Transient: re-inits to HEIGHT on load.
    public transient int height = HEIGHT;
    // Width-dependent buffers are allocated solely by setWidth (called from the constructor via
    // RIS, and on every DECCOLM/RIS width switch). No field initializer here — that would just be
    // allocated and immediately discarded by setWidth's reallocation.
    public transient int[] buffer;
    public transient ColorData[] colors;
    public transient ColorData[] colorsBackground;
    public transient byte[] styles;
    public boolean[] tabs;
    public State state = State.NORMAL;
    // Transient like the buffers: margins address buffer rows, and the buffers re-init on
    // load — a restored margin would point into a re-initialized 24-row page (and, saved at
    // a larger dynamic height, could exceed the fresh buffer's row count entirely).
    public transient int scrollFirst = 0;
    public transient int scrollLast = HEIGHT - 1;
    // DECSLRM left/right margins (§44, CH6 `CSI Pl;Pr s`), only settable while DECLRMM is on.
    // Column-based like scrollFirst/scrollLast is row-based; same transient/re-init rationale.
    public transient int scrollColFirst = 0;
    public transient int scrollColLast = WIDTH - 1;
    public int x;
    public int y;
    /**
     * Autowrap-pending flag (DEC autowrap; xterm's {@code do_wrap}): set when a printable
     * fills the last column — the cursor stays at {@code width-1} and the wrap (NEL) fires
     * on the <em>next</em> printable, not the current one. Every cursor repositioning clears
     * it (matching xterm's {@code ResetWrap}), so the cursor never sits at a phantom
     * column {@code width}. Transient: a restored cursor must not carry a pending wrap.
     */
    public transient boolean autowrapPending;
    /**
     * The last printable character written, for REP ({@code CSI Ps b}, repeat preceding char).
     * Set by {@link li.cil.oc2.common.vm.terminal.buffer.TerminalBufferWriter#putChar}; reset to
     * a non-printable sentinel ({@code -1}) by cursor moves and resets, matching xterm's
     * {@code lastchar}. Transient: not part of saved/restored state.
     */
    public transient int lastPrintedChar = -1;
    /**
     * Saved cursor position + rendition for the main and alt buffers, written by DECSC/SCOSC and
     * read back by DECRC/SCORC (see {@link li.cil.oc2.common.vm.terminal.escapes.SavedCursor}).
     */
    public SavedCursorState savedCursor = new SavedCursorState();
    public SavedCursorState altSavedCursor = new SavedCursorState();
    // Transient like the buffers (see scrollFirst/scrollLast): these are absolute buffer
    // row indices bounded by height * SCROLL_BACK_COUNT. Persisting them across a dynamic
    // height change breaks that invariant on load (a 48-row save restores up to 960 into
    // a re-initialized 480-row window), producing out-of-bounds writer/renderer indices.
    public transient int lastRowToDisplay = HEIGHT;
    public transient int lastRowToDisplayMax = HEIGHT;

    public transient int[] altBuffer;
    public transient ColorData[] altColors;
    public transient ColorData[] altColorsBackground;
    public transient byte[] altStyles;
    public boolean[] altTabs;
    // Per-row double-size line attributes (ESC #3/#4/#5/#6). One byte per absolute row.
    public transient byte[] lineAttrs;
    public transient byte[] altLineAttrs;

    public final transient Set<RendererModel> renderers =
            Collections.newSetFromMap(new WeakHashMap<>());
    final transient ReentrantLock renderersLock = new ReentrantLock();
    // Network diff dirty-tracking (rows/shift-ops/palette revision) — extracted to
    // TerminalNetworkState (А1); geometry inputs (height, lastRowToDisplay, alt state) are
    // passed in per call since this terminal's own fields are the source of truth for those.
    final transient TerminalNetworkState networkState = new TerminalNetworkState(height);
    public transient boolean displayOnly;
    public transient volatile boolean hasPendingBell;
    public boolean useG0 = true;
    public int drawingModeG0;
    public int drawingModeG1;
    public int cursorMode;
    public ModeState currentModeState = new ModeState();
    public PrivateModeState currentPrivateModeState = new PrivateModeState();
    public PrivateModeState savePrivateModeState = new PrivateModeState();

    public enum State {
        NORMAL,
        ESCAPE,
        SHIFT_IN_CHARACTER_SET,
        SHIFT_OUT_CHARACTER_SET,
        HASH,
        DCS,
        OSC,
        APC,
        CONTROL_SEQUENCE,
    }

    public transient TerminalBuffer bufferManager;
    public transient TerminalBufferWriter bufferWriter;
    // Seqlock for the client render thread (§36 M4): the resize paths mutate geometry and swap
    // buffer arrays lock-free, so a frame capturing field-by-field could mix pre/post-resize
    // state. Bumped +1 around each commit stretch (odd = mid-commit); the renderer captures
    // between two even readings and retries on mismatch. Closes the geometry-tear class; the
    // per-cell content tear (rows written mid-tessellation) remains inherent to shared arrays.
    // Atomic only to keep ErrorProne's NonAtomicVolatileUpdate quiet — each terminal has a
    // single writer thread (VM/runner on the server, network apply on the client).
    final AtomicInteger geometryVersion = new AtomicInteger();
    transient CSIManager csiManager = new CSIManager(this);
    transient OSCManager oscManager = new OSCManager(this);
    transient DCSManager dcsManager = new DCSManager();
    transient APCManager apcManager = new APCManager();
    public transient TerminalIO io = new TerminalIO(this);
    private final transient TerminalRenderState renderState = new TerminalRenderState(this);
    private final transient TerminalResizer resizer = new TerminalResizer(this);
    private final transient TerminalHeightResizer heightResizer = new TerminalHeightResizer(this);

    public Terminal() {
        bufferManager = new TerminalBuffer(this);
        bufferWriter = new TerminalBufferWriter(this);
        RIS.execute(this);
    }

    public int getWidth() {
        return width * CHAR_WIDTH;
    }

    public int getHeight() {
        return height * CHAR_HEIGHT;
    }

    /**
     * Reset the current rendition to defaults — SGR attributes, color modes, and the active
     * color palette — as DECCOLM (VT100–VT420) does. Does not touch saved DECSC/DECRC state
     * (that's RIS/DECSC's domain). Must run before setWidth so the buffer erase fills with
     * the default background, not whatever SGR background was active when the mode change hit.
     */
    public void resetRendition() {
        currentForegroundColorMode = ColorMode.DEFAULT_FOREGROUND;
        currentBackgroundColorMode = ColorMode.DEFAULT_BACKGROUND;
        sixteenColor = TerminalColors.DEFAULT_COLORS.copy();
        sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
        backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
        foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
        twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
        style = TerminalColors.DEFAULT_STYLE;
    }

    /**
     * Resolve the CURRENT SGR background color — the color every erase/blank fill uses (the
     * VT510 "erase character" background: DECCOLM's destructive clear via {@link #setWidth}, ED
     * fills, shift-blanked rows) and the background written into cells by the character writer.
     * One resolution point for the mode-to-color mapping; callers copy the result when it must
     * outlive the current SGR state (the color fields are mutated in place by SGR).
     */
    public ColorData currentBackgroundColor() {
        return switch (currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> twoFiftySixColor;
            case TRUE_COLOR -> backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> sixteenColorBright;
            default -> TerminalColors.DEFAULT_BACKGROUND_COLOR;
        };
    }

    public int getTerminalWidth() {
        return width;
    }

    public int getTerminalHeight() {
        return height;
    }

    /** Seqlock version for lock-free geometry capture on the render thread (see field doc). */
    public int getGeometryVersion() {
        return geometryVersion.get();
    }

    public void setWidth(final int newWidth) {
        resizer.setWidth(newWidth);
    }

    /**
     * Non-destructive width change (DECSCPP, {@code CSI Pn $ |}). See
     * {@link TerminalResizer#resizeWidth} for the full rationale.
     */
    public void resizeWidth(final int newWidth) {
        resizer.resizeWidth(newWidth);
    }

    /**
     * Non-destructive height change (DECSLPP/DECSNLS/XTWINOPS case-8). See
     * {@link TerminalHeightResizer#resizeHeight} for the full rationale.
     */
    public void resizeHeight(final int newHeight) {
        heightResizer.resizeHeight(newHeight);
    }

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer() {
        return renderState.getRenderer();
    }

    public void setCursorPos(final int x, final int y) {
        autowrapPending = false; // any explicit cursor move clears the pending wrap (xterm ResetWrap)
        lastPrintedChar = -1; // a cursor move means no preceding graphic char for REP (xterm lastchar)
        this.x = Math.clamp(x, 0, width - 1);
        this.y = Math.clamp(y, 0, height - 1);
    }

    public void setClampedCursorPos(final int x, final int y) {
        if (this.y >= scrollFirst && this.y <= scrollLast) {
            setCursorPos(x, Math.clamp(y, scrollFirst, scrollLast));
        } else {
            setCursorPos(x, y);
        }
    }

    /**
     * Move the cursor by a relative delta, clamping the delta to the screen extent before the add.
     * CSI argument parsing saturates at {@link Integer#MAX_VALUE}, so {@code terminal.x + dx} would
     * overflow to a negative int and {@link #setClampedCursorPos} would then clamp that wrapped value
     * to 0 (the near edge) instead of the far edge. Bounding the delta first keeps the sum in range;
     * {@code setClampedCursorPos} still applies the screen and scroll-region clamp to the result.
     * Negative deltas (up/left) are bounded symmetrically.
     */
    public void moveCursorBy(final int dx, final int dy) {
        setClampedCursorPos(x + Math.clamp(dx, -width, width),
                y + Math.clamp(dy, -height, height));
    }

    public void setRelativeCursorPos(final int x, final int y) {
        setRelativeCursorPos(x, y, true);
    }

    /**
     * Move the cursor, treating {@code x}/{@code y} as origin-relative under DECOM.
     *
     * @param xRelative whether {@code x} is origin-relative to the left margin under DECOM (CUP/
     *         HVP/home) or an absolute column that should pass through untouched (VPA, which only
     *         ever repositions the row) — see the {@link #setRelativeCursorPos(int, int)} callers.
     */
    public void setRelativeCursorPos(final int x, final int y, final boolean xRelative) {
        if (currentPrivateModeState.DECOM) {
            // Clamp y into the scroll region (origin-relative under DECOM) BEFORE adding
            // scrollFirst: parseArgument saturates at Integer.MAX_VALUE, so scrollFirst + y
            // would overflow negative and clamp to scrollFirst (top) instead of scrollLast
            // (bottom). Bounding y to the region keeps the sum in range; row 1 = scrollFirst.
            final int clampedY = scrollFirst + Math.clamp(y, 0, scrollLast - scrollFirst);
            if (xRelative) {
                // Same overflow rationale, on the column axis: origin becomes the left margin.
                final int clampedX =
                        scrollColFirst + Math.clamp(x, 0, scrollColLast - scrollColFirst);
                setCursorPos(clampedX, clampedY);
            } else {
                setCursorPos(x, clampedY);
            }
        } else {
            setCursorPos(x, y);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setDisplayOnly(final boolean value) {
        renderState.setDisplayOnly(value);
    }

    @OnlyIn(Dist.CLIENT)
    public void releaseRenderer(final RendererView renderer) {
        renderState.releaseRenderer(renderer);
    }

    public void markDirty(final long mask) {
        final boolean alt = currentPrivateModeState.isAltBufferEnabled();
        networkState.recordDirtyScreenRows(mask, height, alt, lastRowToDisplay);
        renderersLock.lock();
        try {
            renderers.forEach(
                    model ->
                            model.getDirtyMask()
                                    .accumulateAndGet(mask, (left, right) -> left | right));
        } finally {
            renderersLock.unlock();
        }
    }

    public void markAllDirty() {
        networkState.markAllDirty();
        renderersLock.lock();
        try {
            renderers.forEach(model -> model.getDirtyMask().set(-1L));
        } finally {
            renderersLock.unlock();
        }
    }

    /**
     * Marks every absolute buffer row (visible + scrollback) dirty for the network diff and
     * forces a full renderer repaint. Used by erase-scrollback (ED 3 J) where the cleared
     * scrollback rows are not covered by the screen-row mask alone — the client's scrollback
     * copy must be brought to spaces. The buffer content has already been moved/cleared by the
     * caller; this just publishes the dirty state to both sinks.
     */
    public void markAllBufferRowsDirty() {
        networkState.markAllBufferRowsDirty(height);
        renderersLock.lock();
        try {
            renderers.forEach(model -> model.getDirtyMask().set(-1L));
        } finally {
            renderersLock.unlock();
        }
    }

    /**
     * Record one main-buffer shift's resolved memmove geometry for the network diff sink. The
     * client replays exactly this (see the shift-op replay in TerminalDiff.apply), so its
     * scrollback copy stays exact for rows the screen-row dirty mask cannot address (anything
     * above the visible window) — for as long as the op backlog survives. Op overflow drops the
     * backlog and marks every buffer row dirty instead of just the visible window, so the
     * resulting full refresh re-ships the entire scrollback and self-heals in one diff — see
     * {@link TerminalDiff#capture} (§46 sweep tail: this used to only flag a full refresh of
     * the visible window, leaving scrollback above it permanently diverged).
     */
    public void recordNetworkShift(
            final int copySrcRow,
            final int copyDstRow,
            final int copyRows,
            final int blankStartRow,
            final int blankRows) {
        networkState.recordShift(copySrcRow, copyDstRow, copyRows, blankStartRow, blankRows, height);
    }

    /** Dirty state since the last consume: full-refresh request plus changed buffer rows. */
    // Array components are intentional: transient internal diff record, rows is a fresh copy from
    // BitSet.stream().toArray() consumed immediately; List would add allocation overhead on hot path.
    @SuppressWarnings("ArrayRecordComponent")
    public record NetworkDirty(boolean fullRefresh, int[] rows, int[] shiftOps) {
        public NetworkDirty {
            // Owned copies (rows arrives fresh from the sink either way; cheap at diff rates).
            rows = rows.clone();
            shiftOps = shiftOps.clone();
        }

        @Override
        public int[] shiftOps() {
            return shiftOps.clone();
        }
    }

    public NetworkDirty consumeNetworkDirty() {
        return networkState.consume();
    }

    /**
     * Bump the palette revision so the next network diff ships the palette to clients. Called
     * wherever palette256 is written (RIS/OSC4/OSC104).
     */
    public void markPaletteDirty() {
        networkState.markPaletteDirty();
    }

    /**
     * Returns a clone of palette256 to ship to clients if it changed since the last diff, or
     * null if unchanged (zero steady-state cost). {@code force} is set by the reset path
     * (captureFull) so a RIS reset snapshot always carries the palette even when the revision
     * hasn't moved — otherwise a client that missed an earlier change would keep a stale one.
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull") // null is a load-bearing sentinel: the Snapshot record + stream codec use it to mean "palette unchanged this diff" (skip the ~1 KiB payload). An empty array can't express absence, and Optional<int[]> allocates on the hot path.
    @javax.annotation.Nullable
    public int[] consumePaletteDirty(final boolean force) {
        return networkState.consumePaletteDirty(force, palette256);
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        renderState.clientTick();
    }
}
