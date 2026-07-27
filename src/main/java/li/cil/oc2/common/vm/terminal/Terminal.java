package li.cil.oc2.common.vm.terminal;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import java.nio.ByteBuffer;
import java.util.*;
import javax.annotation.Nullable;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorMode;
import li.cil.oc2.common.vm.terminal.escapes.*;
import li.cil.oc2.common.vm.terminal.escapes.apc.APCManager;
import li.cil.oc2.common.vm.terminal.escapes.csi.CSIManager;
import li.cil.oc2.common.vm.terminal.escapes.dcs.DCSManager;
import li.cil.oc2.common.vm.terminal.escapes.osc.OSCManager;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Serialized
public class Terminal {
    public boolean Use1006 = false;

    public static final int WIDTH = 80, HEIGHT = 24;
    public static final int CHAR_WIDTH = 8, CHAR_HEIGHT = 16;

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

    public final int SCROLL_BACK_COUNT = 20;
    public final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    public final int[] buffer = new int[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public final ColorData[] colors = new ColorData[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public final ColorData[] colorsBackground = new ColorData[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public final byte[] styles = new byte[WIDTH * HEIGHT * SCROLL_BACK_COUNT];
    public final boolean[] tabs = new boolean[WIDTH];
    public State state = State.NORMAL;
    public int scrollFirst = 0, scrollLast = HEIGHT - 1;
    public int x, y;
    public int savedX, savedY, altSavedX, altSavedY;
    public int lastRowToDisplay = 24, lastRowToDisplayMax = 24;

    public final int[] altBuffer = new int[WIDTH * HEIGHT];
    public final ColorData[] altColors = new ColorData[WIDTH * HEIGHT];
    public final ColorData[] altColorsBackground = new ColorData[WIDTH * HEIGHT];
    public final byte[] altStyles = new byte[WIDTH * HEIGHT];
    public final boolean[] altTabs = new boolean[WIDTH];

    public final transient Set<RendererModel> renderers =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    public transient boolean displayOnly, hasPendingBell;
    public boolean continuationByte;
    public int unicode, bytesRead, bytesToRead;
    public boolean useG0 = true;
    public int drawingModeG0, drawingModeG1, cursorMode;
    public ModeState currentModeState = new ModeState();
    public PrivateModeState currentPrivateModeState = new PrivateModeState(),
            savePrivateModeState = new PrivateModeState();

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

    public final TerminalBuffer bufferManager;
    CSIManager csiManager = new CSIManager(this);
    OSCManager oscManager = new OSCManager(this);
    DCSManager dcsManager = new DCSManager(this);
    APCManager apcManager = new APCManager(this);
    final TerminalIO io = new TerminalIO(this);
    final TerminalClient client = new TerminalClient(this);

    public Terminal() {
        bufferManager = new TerminalBuffer(this);
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
        return client.getRenderer();
    }

    public void incrementLastLineToDisplay() {
        bufferManager.incrementLastLineToDisplay();
    }

    public void incrementLastLineToDisplay(boolean scroll) {
        bufferManager.incrementLastLineToDisplay(scroll);
    }

    public void decrementLastLineToDisplay() {
        bufferManager.decrementLastLineToDisplay();
    }

    public void clear() {
        bufferManager.clear();
    }

    public void clearAlt() {
        bufferManager.clearAlt();
    }

    public void clearLine(final int y) {
        bufferManager.clearLine(y);
    }

    public void clearLine(final int y, final int fromIndex, final int toIndex) {
        bufferManager.clearLine(y, fromIndex, toIndex);
    }

    public void shiftUp(int count) {
        bufferManager.shiftUp(count);
    }

    public void shiftLines(final int firstLine, final int lastLine, final int count) {
        bufferManager.shiftLines(firstLine, lastLine, count);
    }

    public void shiftDown(int count) {
        bufferManager.shiftDown(count);
    }

    public void shiftUpOne() {
        bufferManager.shiftUpOne();
    }

    public void shiftDownOne() {
        bufferManager.shiftDownOne();
    }

    public void setCursorPos(final int x, final int y) {
        this.x = Math.max(0, Math.min(WIDTH - 1, x));
        this.y = Math.max(0, Math.min(HEIGHT - 1, y));
    }

    public void setClampedCursorPos(final int x, final int y) {
        setCursorPos(x, Math.max(scrollFirst, Math.min(scrollLast, y)));
    }

    public void setRelativeCursorPos(final int x, final int y) {
        if (currentPrivateModeState.DECOM) {
            setCursorPos(x, Math.min(scrollFirst + y, scrollLast));
        } else {
            setCursorPos(x, y);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setDisplayOnly(final boolean value) {
        client.setDisplayOnly(value);
    }

    @OnlyIn(Dist.CLIENT)
    public void releaseRenderer(final RendererView renderer) {
        client.releaseRenderer(renderer);
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        client.clientTick();
    }

    public synchronized int readInput() {
        return io.readInput();
    }

    @Nullable
    public synchronized ByteBuffer getInput() {
        return io.getInput();
    }

    public synchronized void putInput(final String value) {
        io.putInput(value);
    }

    public synchronized void putInput(final ByteBuffer values) {
        io.putInput(values);
    }

    public synchronized void putOutput(final ByteBuffer values) {
        io.putOutput(values);
    }

    public synchronized void putOutput(final byte value) {
        io.putOutput(value);
    }

    public synchronized void putInput(final char value) {
        io.putInput(value);
    }

    public synchronized void putInput(final byte value) {
        io.putInput(value);
    }

    public void putResponse(final String value) {
        io.putResponse(value);
    }

    public void putResponse(final byte value) {
        io.putResponse(value);
    }
}