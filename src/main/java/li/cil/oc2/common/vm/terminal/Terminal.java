package li.cil.oc2.common.vm.terminal;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import java.util.*;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.vm.terminal.buffer.TerminalBuffer;
import li.cil.oc2.common.vm.terminal.buffer.TerminalBufferWriter;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;
import li.cil.oc2.common.vm.terminal.escapes.*;
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

    public ColorMode currentForegroundColorMode = ColorMode.SIXTEEN_COLOR;
    public ColorMode currentBackgroundColorMode = ColorMode.SIXTEEN_COLOR;
    public ColorData sixteenColor,
            sixteenColorBright,
            twoFiftySixColor,
            backgroundColor,
            foregroundColor;
    public byte style;

    public int SCROLL_BACK_COUNT = 20;
    public transient ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    public transient int[] buffer = new int[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public transient ColorData[] colors = new ColorData[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public transient ColorData[] colorsBackground = new ColorData[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public transient byte[] styles = new byte[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public boolean[] tabs = new boolean[WIDTH];
    public State state = State.NORMAL;
    public int scrollFirst = 0;
    public int scrollLast = HEIGHT - 1;
    public int x;
    public int y;
    public int savedX;
    public int savedY;
    public int altSavedX;
    public int altSavedY;
    public int lastRowToDisplay = 24;
    public int lastRowToDisplayMax = 24;

    public transient int[] altBuffer = new int[WIDTH * HEIGHT];
    public transient ColorData[] altColors = new ColorData[WIDTH * HEIGHT];
    public transient ColorData[] altColorsBackground = new ColorData[WIDTH * HEIGHT];
    public transient byte[] altStyles = new byte[WIDTH * HEIGHT];
    public boolean[] altTabs = new boolean[WIDTH];

    public final transient Set<RendererModel> renderers =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
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
    private transient TerminalClient client;

    public Terminal() {
        bufferManager = new TerminalBuffer(this);
        bufferWriter = new TerminalBufferWriter(this);
        RIS.execute(this);
    }

    public int getWidth() {
        return WIDTH * CHAR_WIDTH;
    }

    public int getHeight() {
        return HEIGHT * CHAR_HEIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer() {
        return client().getRenderer();
    }

    public void setCursorPos(final int x, final int y) {
        this.x = Math.max(0, Math.min(WIDTH - 1, x));
        this.y = Math.max(0, Math.min(HEIGHT - 1, y));
    }

    public void setClampedCursorPos(final int x, final int y) {
        setCursorPos(x, Math.max(0, Math.min(HEIGHT - 1, y)));
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
        renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(mask, (left, right) -> left | right));
    }

    public void markAllDirty() {
        renderers.forEach(model -> model.getDirtyMask().set(-1));
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        client().clientTick();
    }

    private TerminalClient client() {
        TerminalClient result = client;
        if (result == null) {
            synchronized (this) {
                result = client;
                if (result == null) {
                    result = new TerminalClient(this);
                    client = result;
                }
            }
        }
        return result;
    }
}
