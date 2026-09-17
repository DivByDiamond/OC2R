package li.cil.oc2.common.vm.terminal;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import java.util.*;
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
    // Monotonic revision bumped on every palette256 write (RIS/OSC4/OSC104). The network diff
    // ships the palette to clients only when this differs from lastSentPaletteRevision, so a
    // server-side OSC 4 redefinition actually reaches the player's screen (the render path
    // reads the client Terminal's palette256). Transient: a freshly-loaded terminal starts at
    // revision 0 with the default palette; runtime mutations sync via the diff, not persistence.
    private transient int paletteRevision = 0;
    private transient int lastSentPaletteRevision = -1;
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
    public int savedX;
    public int savedY;
    /**
     * Saved autowrap-pending flag (xterm's {@code sc->wrap_flag}), saved/restored by DECSC/DECRC
     * and the SCOSC/SCORC pair as part of the cursor state. Restored AFTER the cursor move in
     * {@link SavedCursor#restore}, mirroring xterm's "after CursorSet/ResetWrap" ordering.
     */
    public boolean savedAutowrapPending;
    public byte savedStyle;
    public boolean savedUseG0 = true;
    public int savedDrawingModeG0;
    public int savedDrawingModeG1;
    public ColorMode savedForegroundColorMode = ColorMode.DEFAULT_FOREGROUND;
    public ColorMode savedBackgroundColorMode = ColorMode.DEFAULT_BACKGROUND;
    public ColorData savedSixteenColor = TerminalColors.DEFAULT_COLORS.copy();
    public ColorData savedSixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
    public ColorData savedTwoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
    public ColorData savedForegroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
    public ColorData savedBackgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
    public int altSavedX;
    public int altSavedY;
    public boolean altSavedAutowrapPending;
    public byte altSavedStyle;
    public boolean altSavedUseG0 = true;
    public int altSavedDrawingModeG0;
    public int altSavedDrawingModeG1;
    public ColorMode altSavedForegroundColorMode = ColorMode.DEFAULT_FOREGROUND;
    public ColorMode altSavedBackgroundColorMode = ColorMode.DEFAULT_BACKGROUND;
    public ColorData altSavedSixteenColor = TerminalColors.DEFAULT_COLORS.copy();
    public ColorData altSavedSixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
    public ColorData altSavedTwoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
    public ColorData altSavedForegroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
    public ColorData altSavedBackgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
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

    public final transient Set<RendererModel> renderers =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    // Network diff sink: absolute buffer rows changed since the last consume. The server
    // serializes these rows into TerminalDiff messages; the client never parses VT100.
    private transient BitSet networkDirtyRows = new BitSet(HEIGHT * SCROLL_BACK_COUNT);
    private final transient ReentrantLock networkDirtyLock = new ReentrantLock();
    private transient boolean networkNeedsFullRefresh;
    public transient boolean displayOnly;
    public transient boolean hasPendingBell;
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
    transient CSIManager csiManager = new CSIManager(this);
    transient OSCManager oscManager = new OSCManager(this);
    transient DCSManager dcsManager = new DCSManager();
    transient APCManager apcManager = new APCManager();
    public transient TerminalIO io = new TerminalIO(this);
    private transient TerminalClient clientInstance;
    private final transient ReentrantLock clientLock = new ReentrantLock();

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

    public void setWidth(final int newWidth) {
        // Guard against degenerate widths: width-1 feeds Math.clamp as a max everywhere,
        // so a zero/negative width would throw IAE on the next cursor movement. Oversized
        // widths are likewise refused here — the escape handlers police their own params,
        // but TerminalDiff.apply on the client calls this with whatever the snapshot said.
        if (newWidth != Math.clamp(newWidth, 1, MAX_WIDTH)) {
            return;
        }
        this.width = newWidth;

        // Erase color: DECCOLM clears with the current SGR background (VT510 erase
        // character), matching bufferManager.clear(). RIS resets the modes before
        // calling setWidth, so it still fills with defaults.
        final ColorData background = currentBackgroundColor();

        // Reallocate main buffer arrays
        final int mainSize = newWidth * height * SCROLL_BACK_COUNT;
        this.buffer = new int[mainSize];
        this.colors = new ColorData[mainSize];
        this.colorsBackground = new ColorData[mainSize];
        this.styles = new byte[mainSize];
        Arrays.fill(this.buffer, ' ');
        Arrays.fill(this.colors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(this.colorsBackground, background.copy());
        Arrays.fill(this.styles, TerminalColors.DEFAULT_STYLE);

        // Reallocate alt buffer arrays
        final int altSize = newWidth * height;
        this.altBuffer = new int[altSize];
        this.altColors = new ColorData[altSize];
        this.altColorsBackground = new ColorData[altSize];
        this.altStyles = new byte[altSize];
        Arrays.fill(this.altBuffer, ' ');
        Arrays.fill(this.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(this.altColorsBackground, background.copy());
        Arrays.fill(this.altStyles, TerminalColors.DEFAULT_STYLE);

        // Reset tab stops
        this.tabs = new boolean[newWidth];
        this.altTabs = new boolean[newWidth];
        for (int i = 1; i < newWidth; i++) {
            if (i % TerminalColors.TAB_WIDTH == 0) {
                this.tabs[i] = true;
                this.altTabs[i] = true;
            }
        }

        // DECCOLM spec: clear screen, reset margins, home cursor
        this.scrollFirst = 0;
        this.scrollLast = height - 1;
        this.lastRowToDisplay = height;
        this.lastRowToDisplayMax = height;
        this.setCursorPos(0, 0);

        // Mark all rows dirty
        this.renderers.forEach(model -> model.getDirtyMask().set(-1L));
    }

    /**
     * Non-destructive width change (DECSCPP, {@code CSI Pn $ |}): reallocates the width-dependent
     * buffers at the new column count while COPYING existing contents into the surviving columns,
     * instead of clearing them as {@link #setWidth} does. Per DEC VT510-RM and xterm-410
     * {@code CASE_DECSCPP}: DECSCPP does not clear page memory, reset scrolling regions, reset
     * SGR, or reset tab stops — it only changes the column count, clamping the cursor if it now
     * sits beyond the new width. Columns beyond the new width are lost (132→80); new columns
     * (80→132) are default-initialized (blank, default fg/bg, no style) — a resize, not a clear,
     * so unlike {@link #setWidth} (DECCOLM's destructive clear, which fills with the current SGR
     * erase background) the new cells get defaults, matching xterm's {@code calloc}-zero on
     * {@code Reallocate}.
     *
     * <p>The caller (CH13) sets the DECCOLM flag to match — this method is flag-agnostic so it
     * can be reused by a future DECNCSM-gated non-destructive DECCOLM path.
     *
     * <p>Layout: the buffers are flat row-major with {@code width} as the stride (no circular
     * pointer — {@code lastRowToDisplay(Max)} are row-count windows, width-independent), so each
     * row is copied with a stride-aware {@link System#arraycopy}. The shared default object used
     * to fill new columns is safe because the write path ({@code TerminalBufferWriter.putChar})
     * REPLACES the {@code colors[idx]} reference rather than mutating it in place — the same
     * property {@link #setWidth} already relies on.
     */
    public void resizeWidth(final int newWidth) {
        // Guard: degenerate widths would break Math.clamp; a no-op resize avoids a pointless
        // reallocation (DECSCPP to the current width does nothing). Oversized widths are
        // refused at this boundary too (see setWidth). The width field is only assigned at
        // commit time below — if an allocation fails, the terminal keeps a consistent old
        // width and old buffers instead of a bogus stride over live data.
        if (newWidth != Math.clamp(newWidth, 1, MAX_WIDTH) || newWidth == this.width) {
            return;
        }
        final int oldWidth = this.width;
        final int copyCols = Math.min(oldWidth, newWidth);

        // New columns are default-initialized (blank, default fg/bg, no style) — DECSCPP is a
        // resize, not a clear, so the new cells get defaults rather than the current SGR erase
        // background that setWidth (DECCOLM's destructive clear) uses. Matches xterm's calloc-
        // zero on Reallocate. Surviving columns keep their actual colors via the arraycopy below.
        final ColorData defaultBackground = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();

        // Main buffer (incl. scrollback): reallocate at the new stride, fill with defaults, then
        // copy the surviving columns of every row. mainRows is width-independent, so the row
        // count and the lastRowToDisplay(Max) window are preserved as-is.
        final int mainRows = height * SCROLL_BACK_COUNT;
        final int[] newBuffer = new int[newWidth * mainRows];
        final ColorData[] newColors = new ColorData[newWidth * mainRows];
        final ColorData[] newColorsBackground = new ColorData[newWidth * mainRows];
        final byte[] newStyles = new byte[newWidth * mainRows];
        Arrays.fill(newBuffer, ' ');
        Arrays.fill(newColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newColorsBackground, defaultBackground);
        Arrays.fill(newStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < mainRows; r++) {
            final int src = r * oldWidth;
            final int dst = r * newWidth;
            System.arraycopy(this.buffer, src, newBuffer, dst, copyCols);
            System.arraycopy(this.colors, src, newColors, dst, copyCols);
            System.arraycopy(this.colorsBackground, src, newColorsBackground, dst, copyCols);
            System.arraycopy(this.styles, src, newStyles, dst, copyCols);
        }

        // Alt buffer (no scrollback): same per-row copy.
        final ColorData[] newAltColors = new ColorData[newWidth * height];
        final ColorData[] newAltColorsBackground = new ColorData[newWidth * height];
        final int[] newAltBuffer = new int[newWidth * height];
        final byte[] newAltStyles = new byte[newWidth * height];
        Arrays.fill(newAltBuffer, ' ');
        Arrays.fill(newAltColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newAltColorsBackground, defaultBackground);
        Arrays.fill(newAltStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < height; r++) {
            final int src = r * oldWidth;
            final int dst = r * newWidth;
            System.arraycopy(this.altBuffer, src, newAltBuffer, dst, copyCols);
            System.arraycopy(this.altColors, src, newAltColors, dst, copyCols);
            System.arraycopy(this.altColorsBackground, src, newAltColorsBackground, dst, copyCols);
            System.arraycopy(this.altStyles, src, newAltStyles, dst, copyCols);
        }

        // Tab stops: preserve existing stops in the surviving columns, default-fill new columns.
        final boolean[] newTabs = new boolean[newWidth];
        final boolean[] newAltTabs = new boolean[newWidth];
        for (int i = 1; i < newWidth; i++) {
            if (i < oldWidth) {
                newTabs[i] = this.tabs[i];
                newAltTabs[i] = this.altTabs[i];
            } else if (i % TerminalColors.TAB_WIDTH == 0) {
                newTabs[i] = true;
                newAltTabs[i] = true;
            }
        }

        // Commit: all allocations succeeded — swap the fields in one stretch. Any failure
        // above leaves the terminal fully consistent at the old width.
        this.buffer = newBuffer;
        this.colors = newColors;
        this.colorsBackground = newColorsBackground;
        this.styles = newStyles;
        this.altBuffer = newAltBuffer;
        this.altColors = newAltColors;
        this.altColorsBackground = newAltColorsBackground;
        this.altStyles = newAltStyles;
        this.tabs = newTabs;
        this.altTabs = newAltTabs;
        this.width = newWidth;

        // Scroll margins + scrollback window are row-based (width-independent) — preserved per
        // DECSCPP (does not reset DECSTBM). The active cursor is clamped only if it now sits
        // beyond the new width (xterm CursorSet on cur_col + 1 > value); setCursorPos clamps x
        // and clears the pending wrap + REP last-char, matching any cursor repositioning. The
        // saved cursor is left as-is — restore routes through the clamping setCursorPos (§36 Б6).
        if (this.x >= newWidth) {
            setCursorPos(newWidth - 1, this.y);
        }

        // Arm the full refresh atomically with the geometry commit: a consume landing between
        // the field swap and here would otherwise ship a partial diff at the new width with no
        // rows, blanking clients that apply it destructively (same seam class as #38 F1).
        networkDirtyLock.lock();
        try {
            networkNeedsFullRefresh = true;
        } finally {
            networkDirtyLock.unlock();
        }

        // Mark all rows dirty — BOTH sinks. The renderer mask drives local redraw; markAllDirty
        // drives the network diff (it also sets the renderer mask internally). The network mark
        // is load-bearing (Kimi gate, PR-2 review): DECSCPP is the first width path where the
        // server PRESERVES content while the client's TerminalDiff.apply responds to the width
        // change with a destructive setWidth — so the snapshot must re-ship the visible window
        // or the client blanks (screen + scrollback) while the server keeps everything,
        // diverging until the next captureFull. setWidth (DECCOLM) gets away without it only
        // because DECCOLM's escape paths (CH2/CH3) call markAllDirty themselves — the clear
        // ships symmetrically.
        markAllDirty();
    }

    /**
     * Non-destructive height change (DECSLPP/DECSNLS, {@code CSI Pn * |}; also XTWINOPS case-8,
     * {@code CSI 8;rows;cols t}): reallocates the height-dependent buffers at the new row count
     * while preserving contents, instead of clearing them. New rows are default-initialized
     * (blank, default fg/bg, no style) — a resize, not a clear.
     *
     * <p>Content anchoring follows xterm-410 {@code Reallocate} with the default SouthWest
     * resizeGravity (screen.c:447-570), NOT a prefix copy: newest content lives at high buffer
     * rows (the buffer shifts up at capacity), so anchoring at row 0 would display ancient
     * scrollback as the screen and destroy the live window.
     * <ul>
     * <li>Shrink: the excess drops off the BOTTOM of the screen first (the rows below the
     * cursor, xterm's {@code max_row - cur_row}); only the remainder scrolls off the top of
     * the buffer. If the surviving span still exceeds the new capacity, the oldest rows are
     * trimmed until it fits. The visible window keeps the cursor's region — e.g. with the
     * cursor on the bottom row, a 48→12 shrink shows the last 12 screen rows, cursor included.
     * <li>Grow: up to {@code delta} scrollback rows are pulled back onto the top of the screen
     * (xterm's {@code move_down}), keeping content glued to the bottom; any delta beyond the
     * available history yields new blank rows at the bottom. This is a pure window shift —
     * buffer rows do not move.
     * </ul>
     *
     * <p>Scroll margins and origin mode are RESET to full-page, also per xterm
     * ({@code ScreenResize} calls {@code resetMargins} and clears ORIGIN unconditionally,
     * screen.c:2427). DEC VT510-RM says DECSLPP preserves DECSTBM — we deliberately follow
     * xterm instead (verified against a physical VT420: the hardware keeps the margins and
     * silently stops using the rest of the page, because this engine's scroll-window machinery
     * gates on full-page margins). Convention: VT first, but when DEC did the big dumb, xterm
     * wins.
     *
     * <p>The caller (CH13) sets any mode flags — this method is flag-agnostic so it can be
     * reused by XTWINOPS case-8 which sets both dimensions at once. Out-of-range heights are
     * refused at this boundary (the handlers police their own params; TerminalDiff.apply on
     * the client does not), and all fields are assigned only after every allocation succeeds
     * (commit-after-alloc), so a failure leaves the terminal fully consistent.
     */
    public void resizeHeight(final int newHeight) { // NOPMD: NPath — relayout math mirrors resizeWidth's shape
        if (newHeight != Math.clamp(newHeight, 1, MAX_HEIGHT) || newHeight == this.height) {
            return;
        }
        final int oldHeight = this.height;
        final int oldMainRows = oldHeight * SCROLL_BACK_COUNT;
        final int newMainRows = newHeight * SCROLL_BACK_COUNT;

        // Relayout plan, computed in OLD buffer coordinates before allocating: source span
        // [srcStart, srcLen) of the old main buffer lands at new rows [0, srcLen).
        final int srcStart;
        final int srcLen;
        final int altSrcStart;
        final int newLrd;
        final int newLrdMax;
        final int newY;
        final int delta = newHeight - oldHeight;
        if (delta > 0) { // grow: keep every row; pull history down into the new top rows
            srcStart = 0;
            srcLen = oldMainRows;
            altSrcStart = 0;
            // The pull comes from the WRITE window's scrollback (lrdMax-based), not the
            // transient view. Both window fields gain +delta (the window is delta taller)
            // and lose -take (the top reclaim): a bottom-anchored view stays glued to the
            // content; a scrolled-back view keeps its rows. newLrdMax >= newHeight holds
            // because take <= lrdMax - oldHeight; the lrd >= height invariant every
            // renderer's (row + lrd - height) indexing relies on is enforced by the floor.
            final int take = Math.min(delta, Math.max(0, this.lastRowToDisplayMax - oldHeight));
            newLrdMax = this.lastRowToDisplayMax - take + delta;
            newLrd = this.lastRowToDisplay == this.lastRowToDisplayMax
                    ? newLrdMax
                    : Math.clamp(this.lastRowToDisplay, newHeight, newLrdMax);
            // The cursor rides the pull on the main buffer; the alt buffer has no scrollback
            // and its copy is top-anchored, so an alt-active grow leaves the cursor alone.
            newY = this.currentPrivateModeState.isAltBufferEnabled() ? this.y : this.y + take;
        } else { // shrink: drop below-cursor rows first, then off the buffer top (xterm move_up)
            final int excess = -delta;
            final int rowsBelowCursor = oldHeight - 1 - this.y;
            final int fromTop = Math.max(0, excess - rowsBelowCursor);
            final int fromBottom = excess - fromTop;
            int start = fromTop;
            int len = this.lastRowToDisplayMax - fromBottom - start;
            if (len > newMainRows) { // surviving span overflows the new capacity: trim oldest
                start += len - newMainRows;
                len = newMainRows;
            }
            srcStart = start;
            srcLen = len;
            // The alt buffer has no scrollback, but the same gravity applies to it (xterm's
            // Reallocate treats every ScrnBuf alike): the copy must anchor at the same
            // fromTop the cursor math uses, or the cursor would ride up while its content
            // stays top-anchored — parked on an unrelated row.
            altSrcStart = fromTop;
            newLrdMax = len;
            // lrd - srcStart may go negative when the capacity trim ate rows the view was
            // parked on (deep scrollback + aggressive shrink) — the floor at newHeight then
            // parks the view on the full new screen, which is the only sane answer.
            newLrd = Math.clamp(this.lastRowToDisplay - srcStart, newHeight, len);
            newY = Math.clamp(this.y - fromTop, 0, newHeight - 1);
        }

        final ColorData defaultBackground = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();

        // Main buffer (incl. scrollback): reallocate at the new height, fill with defaults,
        // then copy the planned span. Each row is width cells, stride unchanged.
        final int[] newBuffer = new int[width * newMainRows];
        final ColorData[] newColors = new ColorData[width * newMainRows];
        final ColorData[] newColorsBackground = new ColorData[width * newMainRows];
        final byte[] newStyles = new byte[width * newMainRows];
        Arrays.fill(newBuffer, ' ');
        Arrays.fill(newColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newColorsBackground, defaultBackground);
        Arrays.fill(newStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < srcLen; r++) {
            final int src = (srcStart + r) * width;
            final int dst = r * width;
            System.arraycopy(this.buffer, src, newBuffer, dst, width);
            System.arraycopy(this.colors, src, newColors, dst, width);
            System.arraycopy(this.colorsBackground, src, newColorsBackground, dst, width);
            System.arraycopy(this.styles, src, newStyles, dst, width);
        }

        // Alt buffer (no scrollback): top-anchored on grow; on shrink anchored at the same
        // fromTop as the cursor relayout (xterm Reallocate gravity, see the plan comment).
        final int copyAltRows = Math.min(oldHeight, newHeight);
        final ColorData[] newAltColors = new ColorData[width * newHeight];
        final ColorData[] newAltColorsBackground = new ColorData[width * newHeight];
        final int[] newAltBuffer = new int[width * newHeight];
        final byte[] newAltStyles = new byte[width * newHeight];
        Arrays.fill(newAltBuffer, ' ');
        Arrays.fill(newAltColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newAltColorsBackground, defaultBackground);
        Arrays.fill(newAltStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < copyAltRows; r++) {
            final int src = (altSrcStart + r) * width;
            final int dst = r * width;
            System.arraycopy(this.altBuffer, src, newAltBuffer, dst, width);
            System.arraycopy(this.altColors, src, newAltColors, dst, width);
            System.arraycopy(this.altColorsBackground, src, newAltColorsBackground, dst, width);
            System.arraycopy(this.altStyles, src, newAltStyles, dst, width);
        }

        // Commit: all allocations succeeded — swap every field in one stretch. Any failure
        // above leaves the terminal fully consistent at the old height.
        this.buffer = newBuffer;
        this.colors = newColors;
        this.colorsBackground = newColorsBackground;
        this.styles = newStyles;
        this.altBuffer = newAltBuffer;
        this.altColors = newAltColors;
        this.altColorsBackground = newAltColorsBackground;
        this.altStyles = newAltStyles;
        this.height = newHeight;
        this.lastRowToDisplay = newLrd;
        this.lastRowToDisplayMax = newLrdMax;

        // Margins + origin reset per xterm ScreenResize (see Javadoc).
        this.scrollFirst = 0;
        this.scrollLast = newHeight - 1;
        this.currentPrivateModeState.DECOM = false;

        // Move the cursor with its content (grow pull-down / shrink top-drop), clamped into
        // the new page; routes through setCursorPos for the pending-wrap/REP clearing that
        // matches any repositioning. Unmoved cursor (plain grow) is left alone.
        if (newY != this.y) {
            setCursorPos(this.x, newY);
        }

        // Reallocate the network dirty BitSet for the new capacity and arm the full refresh
        // atomically with the geometry commit: a consume landing between the field swap and
        // here would otherwise ship a partial diff at the new height with no rows.
        networkDirtyLock.lock();
        try {
            networkDirtyRows = new BitSet(newMainRows);
            networkNeedsFullRefresh = true;
        } finally {
            networkDirtyLock.unlock();
        }

        // Mark all rows dirty — BOTH sinks (same rationale as resizeWidth).
        markAllDirty();
    }

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer() {
        return client().getRenderer();
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
        if (currentPrivateModeState.DECOM) {
            // Clamp y into the scroll region (origin-relative under DECOM) BEFORE adding
            // scrollFirst: parseArgument saturates at Integer.MAX_VALUE, so scrollFirst + y
            // would overflow negative and clamp to scrollFirst (top) instead of scrollLast
            // (bottom). Bounding y to the region keeps the sum in range; row 1 = scrollFirst.
            setCursorPos(x, scrollFirst + Math.clamp(y, 0, scrollLast - scrollFirst));
        } else {
            setCursorPos(x, y);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setDisplayOnly(final boolean value) {
        client().setDisplayOnly(value);
    }

    @OnlyIn(Dist.CLIENT)
    public void releaseRenderer(final RendererView renderer) {
        client().releaseRenderer(renderer);
    }

    public void markDirty(final long mask) {
        recordNetworkDirtyScreenRows(mask);
        renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(mask, (left, right) -> left | right));
    }

    public void markAllDirty() {
        networkDirtyLock.lock();
        try {
            networkNeedsFullRefresh = true;
        } finally {
            networkDirtyLock.unlock();
        }
        renderers.forEach(model -> model.getDirtyMask().set(-1L));
    }

    /**
     * Converts a screen-row dirty bit mask into absolute buffer rows for the network diff
     * sink. Alt-buffer rows are indexed by screen row directly; main-buffer screen row
     * {@code s} lives at absolute buffer row {@code s + lastRowToDisplay - height}.
     */
    private void recordNetworkDirtyScreenRows(final long mask) {
        if (mask == 0) return;
        final boolean alt = currentPrivateModeState.isAltBufferEnabled();
        networkDirtyLock.lock();
        try {
            for (int s = 0; s < height; s++) {
                if ((mask & (1L << s)) == 0) continue;
                final int row = alt ? s : s + lastRowToDisplay - height;
                // Bound is the LOGICAL capacity (height * SCROLL_BACK_COUNT), not
                // networkDirtyRows.size() — a BitSet rounds its capacity up to 64-word
                // multiples, so .size() would admit rows beyond the buffer's real end.
                if (row >= 0 && row < height * SCROLL_BACK_COUNT) {
                    networkDirtyRows.set(row);
                }
            }
        } finally {
            networkDirtyLock.unlock();
        }
    }

    /** Dirty state since the last consume: full-refresh request plus changed buffer rows. */
    // Array component is intentional: transient internal diff record, rows is a fresh copy from
    // BitSet.stream().toArray() consumed immediately; List would add allocation overhead on hot path.
    @SuppressWarnings("ArrayRecordComponent")
    public record NetworkDirty(boolean fullRefresh, int[] rows) {}

    public NetworkDirty consumeNetworkDirty() {
        networkDirtyLock.lock();
        try {
            final boolean full = networkNeedsFullRefresh;
            final int[] rows = networkDirtyRows.stream().toArray();
            networkNeedsFullRefresh = false;
            networkDirtyRows.clear();
            return new NetworkDirty(full, rows);
        } finally {
            networkDirtyLock.unlock();
        }
    }

    /**
     * Bump the palette revision so the next network diff ships the palette to clients. Called
     * wherever palette256 is written (RIS/OSC4/OSC104). Under networkDirtyLock to keep the
     * revision check in consumePaletteDirty atomic with the bump.
     */
    public void markPaletteDirty() {
        networkDirtyLock.lock();
        try {
            paletteRevision++;
        } finally {
            networkDirtyLock.unlock();
        }
    }

    /**
     * Returns a clone of palette256 to ship to clients if it changed since the last diff, or
     * null if unchanged (zero steady-state cost). {@code force} is set by the reset path
     * (captureFull) so a RIS reset snapshot always carries the palette even when the revision
     * hasn't moved — otherwise a client that missed an earlier change would keep a stale one.
     * Updates lastSentPaletteRevision atomically.
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull") // null is a load-bearing sentinel: the Snapshot record + stream codec use it to mean "palette unchanged this diff" (skip the ~1 KiB payload). An empty array can't express absence, and Optional<int[]> allocates on the hot path.
    public int[] consumePaletteDirty(final boolean force) {
        networkDirtyLock.lock();
        try {
            if (!force && paletteRevision == lastSentPaletteRevision) {
                return null;
            }
            lastSentPaletteRevision = paletteRevision;
            return palette256.clone();
        } finally {
            networkDirtyLock.unlock();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        client().clientTick();
    }

    private TerminalClient client() {
        TerminalClient result = clientInstance;
        if (result == null) {
            clientLock.lock();
            try {
                result = clientInstance;
                if (result == null) {
                    result = new TerminalClient(this);
                    clientInstance = result;
                }
            } finally {
                clientLock.unlock();
            }
        }
        return result;
    }
}
