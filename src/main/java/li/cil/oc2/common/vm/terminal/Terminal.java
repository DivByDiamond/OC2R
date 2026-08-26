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
    public static final int CHAR_WIDTH = 8;
    public static final int CHAR_HEIGHT = 16;

    public static final int STYLE_BOLD_MASK = 1;
    public static final int STYLE_DIM_MASK = 1 << 1;
    public static final int STYLE_UNDERLINE_MASK = 1 << 2;
    public static final int STYLE_BLINK_MASK = 1 << 3;
    public static final int STYLE_INVERT_MASK = 1 << 4;
    public static final int STYLE_HIDDEN_MASK = 1 << 5;
    public static final int STYLE_ITALIC_MASK = 1 << 6;

    public ColorMode currentForegroundColorMode = ColorMode.DEFAULT_FOREGROUND;
    public ColorMode currentBackgroundColorMode = ColorMode.DEFAULT_BACKGROUND;
    public ColorData sixteenColor,
            sixteenColorBright,
            twoFiftySixColor,
            backgroundColor,
            foregroundColor;
    public byte style;

    public int SCROLL_BACK_COUNT = 20;
    public transient ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    // DECCOLM dynamic width; setWidth reallocates buffers. Transient: re-inits to WIDTH on load.
    public transient int width = WIDTH;
    // Width-dependent buffers are allocated solely by setWidth (called from the constructor via
    // RIS, and on every DECCOLM/RIS width switch). No field initializer here — that would just be
    // allocated and immediately discarded by setWidth's reallocation.
    public transient int[] buffer;
    public transient ColorData[] colors;
    public transient ColorData[] colorsBackground;
    public transient byte[] styles;
    public boolean[] tabs;
    public State state = State.NORMAL;
    public int scrollFirst = 0;
    public int scrollLast = HEIGHT - 1;
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
    public int lastRowToDisplay = 24;
    public int lastRowToDisplayMax = 24;

    public transient int[] altBuffer;
    public transient ColorData[] altColors;
    public transient ColorData[] altColorsBackground;
    public transient byte[] altStyles;
    public boolean[] altTabs;

    public final transient Set<RendererModel> renderers =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    // Network diff sink: absolute buffer rows changed since the last consume. The server
    // serializes these rows into TerminalDiff messages; the client never parses VT100.
    private final transient BitSet networkDirtyRows = new BitSet(HEIGHT * SCROLL_BACK_COUNT);
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
    transient DCSManager dcsManager = new DCSManager(this);
    transient APCManager apcManager = new APCManager(this);
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
        return HEIGHT * CHAR_HEIGHT;
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

    public int getTerminalWidth() {
        return width;
    }

    public void setWidth(final int newWidth) {
        // Guard against degenerate widths: width-1 feeds Math.clamp as a max everywhere,
        // so a zero/negative width would throw IAE on the next cursor movement.
        if (newWidth < 1) {
            return;
        }
        this.width = newWidth;

        // Erase color: DECCOLM clears with the current SGR background (VT510 erase
        // character), matching bufferManager.clear(). RIS resets the modes before
        // calling setWidth, so it still fills with defaults.
        final ColorData background = switch (currentBackgroundColorMode) {
            case SIXTEEN_COLOR -> sixteenColor;
            case TWO_FIFTY_SIX_COLOR -> twoFiftySixColor;
            case TRUE_COLOR -> backgroundColor;
            case SIXTEEN_COLOR_BRIGHT -> sixteenColorBright;
            default -> TerminalColors.DEFAULT_BACKGROUND_COLOR;
        };

        // Reallocate main buffer arrays
        final int mainSize = newWidth * HEIGHT * SCROLL_BACK_COUNT;
        this.buffer = new int[mainSize];
        this.colors = new ColorData[mainSize];
        this.colorsBackground = new ColorData[mainSize];
        this.styles = new byte[mainSize];
        Arrays.fill(this.buffer, ' ');
        Arrays.fill(this.colors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(this.colorsBackground, background.copy());
        Arrays.fill(this.styles, TerminalColors.DEFAULT_STYLE);

        // Reallocate alt buffer arrays
        final int altSize = newWidth * HEIGHT;
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
        this.scrollLast = HEIGHT - 1;
        this.lastRowToDisplay = HEIGHT;
        this.lastRowToDisplayMax = HEIGHT;
        this.setCursorPos(0, 0);

        // Mark all rows dirty
        this.renderers.forEach(model -> model.getDirtyMask().set(-1));
    }

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer() {
        return client().getRenderer();
    }

    public void setCursorPos(final int x, final int y) {
        autowrapPending = false; // any explicit cursor move clears the pending wrap (xterm ResetWrap)
        lastPrintedChar = -1; // a cursor move means no preceding graphic char for REP (xterm lastchar)
        this.x = Math.clamp(x, 0, width - 1);
        this.y = Math.clamp(y, 0, HEIGHT - 1);
    }

    public void setClampedCursorPos(final int x, final int y) {
        if (this.y >= scrollFirst && this.y <= scrollLast) {
            setCursorPos(x, Math.clamp(y, scrollFirst, scrollLast));
        } else {
            setCursorPos(x, y);
        }
    }

    public void setRelativeCursorPos(final int x, final int y) {
        if (currentPrivateModeState.DECOM) {
            setCursorPos(x, Math.max(scrollFirst, Math.min(scrollFirst + y, scrollLast)));
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

    public void markDirty(final int mask) {
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
        renderers.forEach(model -> model.getDirtyMask().set(-1));
    }

    /**
     * Converts a screen-row dirty bit mask into absolute buffer rows for the network diff
     * sink. Alt-buffer rows are indexed by screen row directly; main-buffer screen row
     {@code s} lives at absolute buffer row {@code s + lastRowToDisplay - HEIGHT}.
     */
    private void recordNetworkDirtyScreenRows(final int mask) {
        if (mask == 0) return;
        final boolean alt = currentPrivateModeState.isAltBufferEnabled();
        networkDirtyLock.lock();
        try {
            for (int s = 0; s < HEIGHT; s++) {
                if ((mask & (1 << s)) == 0) continue;
                final int row = alt ? s : s + lastRowToDisplay - HEIGHT;
                if (row >= 0 && row < networkDirtyRows.size()) {
                    networkDirtyRows.set(row);
                }
            }
        } finally {
            networkDirtyLock.unlock();
        }
    }

    /** Dirty state since the last consume: full-refresh request plus changed buffer rows. */
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
