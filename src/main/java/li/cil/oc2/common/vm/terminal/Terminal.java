package li.cil.oc2.common.vm.terminal;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorMode;
import li.cil.oc2.common.vm.terminal.escapes.apc.APCManager;
import li.cil.oc2.common.vm.terminal.escapes.csi.CSIManager;
import li.cil.oc2.common.vm.terminal.escapes.*;
import li.cil.oc2.common.vm.terminal.escapes.dcs.DCSManager;
import li.cil.oc2.common.vm.terminal.escapes.osc.OSCManager;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;

import static li.cil.oc2.common.vm.terminal.TerminalColors.Color;

@Serialized
public class Terminal {
    public boolean Use1006 = false;

    // Constants
    public static final int WIDTH = 80, HEIGHT = 24;
    public static final int CHAR_WIDTH = 8;
    public static final int CHAR_HEIGHT = 16;

    public static final int STYLE_BOLD_MASK = 1;
    public static final int STYLE_DIM_MASK = 1 << 1;
    public static final int STYLE_UNDERLINE_MASK = 1 << 2;
    public static final int STYLE_BLINK_MASK = 1 << 3;
    public static final int STYLE_INVERT_MASK = 1 << 4;
    public static final int STYLE_HIDDEN_MASK = 1 << 5;
    public static final int STYLE_ITALIC_MASK = 1 << 6;

    // Current Color Data
    public ColorMode currentForegroundColorMode = ColorMode.SIXTEEN_COLOR;
    public ColorMode currentBackgroundColorMode = ColorMode.SIXTEEN_COLOR;
    public ColorData sixteenColor;
    public ColorData sixteenColorBright;
    public ColorData twoFiftySixColor;
    public ColorData backgroundColor;
    public ColorData foregroundColor;
    public byte style;

    // Buffer Arrays
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

    // Alt Buffer
    public final int[] altBuffer = new int[WIDTH * HEIGHT];
    public final ColorData[] altColors = new ColorData[WIDTH * HEIGHT];
    public final ColorData[] altColorsBackground = new ColorData[WIDTH * HEIGHT];
    public final byte[] altStyles = new byte[WIDTH * HEIGHT];
    public final boolean[] altTabs = new boolean[WIDTH];

    // Render Data
    public final transient Set<RendererModel> renderers = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    public transient boolean displayOnly; // Set on client to not send responses to status requests.
    public transient boolean hasPendingBell;
    public boolean continuationByte;
    public int unicode;
    public int bytesRead;
    public int bytesToRead;
    public boolean useG0 = true;
    public int drawingModeG0;
    public int drawingModeG1;
    public int cursorMode;
    public ModeState currentModeState = new ModeState();
    public PrivateModeState currentPrivateModeState = new PrivateModeState();
    public PrivateModeState savePrivateModeState = new PrivateModeState();

    public enum State {
        NORMAL, ESCAPE, SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET,
        HASH, DCS, OSC, APC, CONTROL_SEQUENCE,
    }

    // Instances
    public final TerminalBuffer bufferManager;
    private final CSIManager csiManager = new CSIManager(this);
    private final OSCManager oscManager = new OSCManager(this);
    private final DCSManager dcsManager = new DCSManager(this);
    private final APCManager apcManager = new APCManager(this);

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
        final TerminalRenderer renderer = new TerminalRenderer(this);
        renderers.add(renderer);
        return renderer;
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
        displayOnly = value;
    }

    @OnlyIn(Dist.CLIENT)
    public void releaseRenderer(final RendererView renderer) {
        if (renderer instanceof final RendererModel rendererModel) {
            rendererModel.close();
            renderers.remove(rendererModel);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        if (hasPendingBell) {
            hasPendingBell = false;
            final Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.getSoundManager().play(SimpleSoundInstance.forUI(NoteBlockInstrument.PLING.getSoundEvent(), 1)));
        }
    }

    public synchronized int readInput() {
        if (input.isEmpty()) {
            return -1;
        } else {
            return input.dequeueByte() & 0xFF;
        }
    }

    @Nullable
    public synchronized ByteBuffer getInput() {
        if (input.isEmpty()) {
            return null;
        } else {
            if (!currentPrivateModeState.isAltBufferEnabled()) lastRowToDisplay = lastRowToDisplayMax;
            int dirtyLinesMask = 0;
            for (int i = 0; i <= 23; i++) {
                dirtyLinesMask |= 1 << i;
            }
            final int finalDirtyLinesMask = dirtyLinesMask;
            renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(finalDirtyLinesMask, (left, right) -> left | right));
            final ByteBuffer buffer = ByteBuffer.allocate(input.size());
            while (!input.isEmpty()) {
                buffer.put(input.dequeueByte());
            }
            buffer.flip();
            return buffer;
        }
    }

    public synchronized void putInput(final String value) {
        putInput(ByteBuffer.wrap(value.getBytes()));
    }

    public synchronized void putInput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            input.enqueue(values.get());
        }
    }

    public synchronized void putOutput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            putOutput(values.get());
        }
    }

    public synchronized void putOutput(final byte value) {
        synchronized (buffer) {
            synchronized (altBuffer) {
                final char ch = (char) value;
                if (!continuationByte && (ch & (1 << 7)) != 0) {
                    continuationByte = true;
                    bytesToRead = 0;
                    bytesRead = 0;
                    unicode = 0;
                    if ((ch & (1 << 6)) != 0) {
                        bytesToRead++;
                    } else {
                        continuationByte = false;
                        return;
                    }

                    if ((ch & (1 << 5)) != 0) {
                        bytesToRead++;
                    } else {
                        unicode = (ch & 0b11111) << 6;
                        return;
                    }

                    if ((ch & (1 << 4)) != 0) {
                        bytesToRead++;
                    } else {
                        unicode = (ch & 0b1111) << 12;
                        return;
                    }

                    unicode = (ch & 0b111) << 18;
                    return;
                } else if (continuationByte) {
                    if ((ch & (1 << 7)) == 0) {
                        continuationByte = false;
                        bytesToRead = 0;
                        bytesRead = 0;
                        return;
                    }

                    bytesRead++;

                    unicode |= (ch & 0b111111) << ((bytesToRead - bytesRead) * 6);

                    if (bytesToRead == bytesRead) {
                        bytesToRead = 0;
                        bytesRead = 0;
                    } else {
                        return;
                    }
                }
                switch (state) {
                    case NORMAL -> {
                        switch (value) {
                            case '\007' -> hasPendingBell = true;
                            case '\033' -> state = State.ESCAPE;
                            case '\016' -> useG0 = false;
                            case '\017' -> useG0 = true;

                            case (byte) '\r' -> setCursorPos(0, y);
                            case (byte) '\n', '\013', '\014' -> {
                                if (currentModeState.LNM) {
                                    NEL.execute(this);
                                } else {
                                    IND.execute(this);
                                }
                            }
                            case (byte) '\t' -> {
                                if (x < WIDTH) {
                                    do {
                                        x++;
                                    } while (x < WIDTH && (currentPrivateModeState.isAltBufferEnabled() ? !altTabs[x] : !tabs[x]));
                                }
                            }
                            case (byte) '\b' -> setCursorPos(Math.min(x, WIDTH - 1) - 1, y);

                            default -> bufferManager.putChar((continuationByte) ? unicode : ch);
                        }
                    }
                    case ESCAPE -> {
                        if (ch == '[') {
                            csiManager.reset();
                            state = State.CONTROL_SEQUENCE;
                        } else if (ch == '(') {
                            state = State.SHIFT_IN_CHARACTER_SET;
                        } else if (ch == ')') {
                            state = State.SHIFT_OUT_CHARACTER_SET;
                        } else if (ch == '#') {
                            state = State.HASH;
                        } else if (ch == 'P') {
                            dcsManager.reset();
                            state = State.DCS;
                        } else if (ch == ']') {
                            oscManager.reset();
                            state = State.OSC;
                        } else if (ch == '_') {
                            apcManager.reset();
                            state = State.APC;
                        } else {
                            state = State.NORMAL;
                            switch (ch) {
                                case 'D' -> IND.execute(this);
                                case 'E' -> NEL.execute(this);
                                case 'M' -> RI.execute(this);
                                case '7' -> DECSC.execute(this);
                                case '8' -> DECRC.execute(this);
                                case 'H' -> HTS.execute(this);
                                case 'c' -> RIS.execute(this);
                                case '=' -> {}
                                case '>' -> {}
                                default -> System.out.println("Invalid escape: " + ch);
                            }
                        }
                    }
                    case CONTROL_SEQUENCE -> csiManager.handle(ch);
                    case SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET -> {
                        state = State.NORMAL;
                        switch (ch) {
                            case 'A' -> {}
                            case 'B' -> drawingModeG0 = TerminalColors.DrawingMode.ASCII;
                            case '0' -> drawingModeG0 = TerminalColors.DrawingMode.SPECIAL_GRAPHICS;
                            case '1' -> {}
                            case '2' -> {}
                        }
                    }
                    case HASH -> {
                        state = State.NORMAL;
                        switch (ch) {
                            case '3', '4', '5', '6' -> {}
                            case '8' -> {
                                if (currentPrivateModeState.isAltBufferEnabled()) {
                                    Arrays.fill(altBuffer, 'E');
                                } else {
                                    Arrays.fill(buffer, (lastRowToDisplayMax - HEIGHT) * WIDTH, ((WIDTH - 1) + (HEIGHT - 1) * WIDTH) + 1, 'E');
                                }
                                renderers.forEach(model -> model.getDirtyMask().set(-1));
                            }
                        }
                    }
                    case DCS -> dcsManager.handle(ch);
                    case OSC -> oscManager.handle(ch);
                    case APC -> apcManager.handle(ch);
                }
            }
        }
    }

    public synchronized void putInput(final char value) {
        putInput((byte) value);
    }

    public synchronized void putInput(final byte value) {
        input.enqueue(value);
    }

    public void putResponse(final String value) {
        for (int i = 0; i < value.length(); i++) {
            putResponse((byte) value.charAt(i));
        }
    }

    public void putResponse(final byte value) {
        if (!displayOnly) {
            putInput(value);
        }
    }
}
