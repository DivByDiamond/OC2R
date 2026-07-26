/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.API;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// VT100 emulation: https://vt100.net/docs/vt100-ug/chapter3.html
// Still around in case of needing to look back when new bugs surface
@Serialized
@SuppressWarnings("all")
public final class OldTerminal {
    public static final int WIDTH = 80, HEIGHT = 24;
    public static final int CHAR_WIDTH = 8;
    public static final int CHAR_HEIGHT = 16;
    public MouseMode CurrentMouseMode = MouseMode.None;
    public boolean Use1006 = false;

    private static final int TAB_WIDTH = 4;

    @SuppressWarnings("unused")
    private static final class Color {
        static final int BLACK = 0;
        static final int RED = 1;
        static final int GREEN = 2;
        static final int YELLOW = 3;
        static final int BLUE = 4;
        static final int MAGENTA = 5;
        static final int CYAN = 6;
        static final int WHITE = 7;
    }

    @SuppressWarnings("unused")
    private static final class Mode {
        static final int LNM = 20;    // Line Feed/New Line Mode
        static final int DECCKM = 1;  // Cursor key
        static final int DECANM = 2;  // ANSI/VT52
        static final int DECCOLM = 3; // Column
        static final int DECSCLM = 4; // Scrolling
        static final int DECSCNM = 5; // Screen
        static final int DECOM = 6;   // Origin
        static final int DECAWM = 7;  // Auto wrap
        static final int DECARM = 8;  // Auto-repeating
        static final int DECINLM = 9; // Interlace
    }

    private static final int COLOR_MASK = 0b111;
    private static final int COLOR_FOREGROUND_SHIFT = 3;

    private static final int STYLE_BOLD_MASK = 1;
    private static final int STYLE_DIM_MASK = 1 << 1;
    private static final int STYLE_UNDERLINE_MASK = 1 << 2;
    private static final int STYLE_BLINK_MASK = 1 << 3;
    private static final int STYLE_INVERT_MASK = 1 << 4;
    private static final int STYLE_HIDDEN_MASK = 1 << 5;

    // Default style: no modifiers, white foreground, black background.
    private static final byte DEFAULT_COLORS = Color.WHITE << COLOR_FOREGROUND_SHIFT;
    private static final byte DEFAULT_STYLE = 0;

    private static final ColorData DEFAULT_TRUE_COLOR_FOREGROUND = new ColorData(238, 238, 238);
    private static final ColorData DEFAULT_TRUE_COLOR_BACKGROUND = new ColorData(0, 0, 0);

    /// ////////////////////////////////////////////////////////////////

    public enum State { // Must be public for serialization.
        NORMAL, // Reading characters normally.
        ESCAPE, // Last character was ESC, figure out what kind next.
        SHIFT_IN_CHARACTER_SET, // Shift in character set.
        SHIFT_OUT_CHARACTER_SET, // Shift out character set.
        HASH, // Escape sequence with # intermediate.
        DCS,
        OSC,
        CONTROL_SEQUENCE, // Know what sequence we have, now parsing it.
    }

    public static class ColorData {
        public int R;
        public int G;
        public int B;

        public ColorData() {
            R = 0;
            G = 0;
            B = 0;
        }

        public ColorData(final int r, final int g, final int b) {
            R = r;
            G = g;
            B = b;
        }

        public int asInt() {
            return (((R & 0b11111111) << 16) | ((G & 0b11111111) << 8) | (B & 0b11111111));
        }
    }

    public ColorData fromByte(byte color) {
        return new ColorData(color, 0, 0);
    }

    public enum ColorMode {
        @SerializedName("0")
        SIXTEEN_COLOR,
        @SerializedName("1")
        TWO_FIFTY_SIX_COLOR,
        @SerializedName("2")
        TWENTY_FOUR_BIT_COLOR
    }

    public enum MouseMode {
        @SerializedName("0")
        None,
        @SerializedName("1")
        X10Compat,
        @SerializedName("2")
        Normal,
        @SerializedName("3")
        MouseHilite
    }

    public interface RendererView {
        void render(final PoseStack stack, final Matrix4f projectionMatrix);
    }

    /// ////////////////////////////////////////////////////////////////

    private final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    private final byte[] buffer = new byte[WIDTH * HEIGHT];
    private final ColorMode[] colorModes = new ColorMode[WIDTH * HEIGHT];
    private final ColorData[] colors = new ColorData[WIDTH * HEIGHT];
    private final ColorData[] colorsBackground = new ColorData[WIDTH * HEIGHT]; // only used for TrueColor
    private final byte[] styles = new byte[WIDTH * HEIGHT];
    private final boolean[] tabs = new boolean[WIDTH];
    private State state = State.NORMAL;
    private final int[] args = new int[5];
    private int argCount = 0;
    private int modes;
    private int scrollFirst = 0, scrollLast = HEIGHT - 1;
    private int x, y;
    private int savedX, savedY;

    // Color info packed into one byte for compact storage
    // 0-2: background color (index)
    // 3-5: foreground color (index)
    private ColorData color;
    // 256 color
    private boolean enable256;
    private byte foregroundColor256;
    private byte backgroundColor256;
    // true color
    private boolean trueColor;
    private ColorData backgroundColor;
    private ColorData foregroundColor;
    // Style info packed into one byte for compact storage
    private byte style;

    // Rendering data for client
    private final transient Set<RendererModel> renderers = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private transient boolean displayOnly; // Set on client to not send responses to status requests.
    private transient boolean hasPendingBell;
    private char lastChar;

    /// ////////////////////////////////////////////////////////////////

    public OldTerminal() {
        RIS();
    }

    /// ////////////////////////////////////////////////////////////////

    public int getWidth() {
        return WIDTH * CHAR_WIDTH;
    }

    public int getHeight() {
        return HEIGHT * CHAR_HEIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    public void setDisplayOnly(final boolean value) {
        displayOnly = value;
    }

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer() {
        final Renderer renderer = new Renderer(this);
        renderers.add(renderer);
        return renderer;
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

    public synchronized void putInput(final char value) { putInput((byte) value); }

    public synchronized void putInput(final byte value) {
        input.enqueue(value);
    }

    public void putOutput(final byte value) {
        final char ch = (char) value;
        switch (state) {
            case NORMAL -> {
                switch (value) {
                    case '\007' -> hasPendingBell = true;
                    case '\033' -> state = State.ESCAPE;
                    case '\016' -> {
                    } // SO
                    case '\017' -> {
                    } // SI

                    case (byte) '\r' /* 015 */ -> setCursorPos(0, y);
                    case (byte) '\n' /* 012 */, '\013', '\014' -> {
                        if (getMode(Mode.LNM)) {
                            NEL();
                        } else {
                            IND();
                        }
                    }
                    case (byte) '\t' /* 011 */ -> {
                        if (x < WIDTH) {
                            do {
                                x++;
                            } while (x < WIDTH && !tabs[x]);
                        }
                    }
                    case (byte) '\b' /* 010 */ -> setCursorPos(Math.min(x, WIDTH - 1) - 1, y);

                    default -> putChar(ch);
                }
            }
            case ESCAPE -> {
                if (ch == '[') { // Control Sequence Indicator
                    Arrays.fill(args, (byte) 0);
                    argCount = 0;
                    state = State.CONTROL_SEQUENCE;
                } else if (ch == '(') { // SCS – Select Character Set
                    state = State.SHIFT_IN_CHARACTER_SET;
                } else if (ch == ')') { // SCS – Select Character Set
                    state = State.SHIFT_OUT_CHARACTER_SET;
                } else if (ch == '#') { // # Intermediate
                    state = State.HASH;
                } else if (ch == 'P') {
                    state = State.DCS;
                } else if (ch == ']') {
                    state = State.OSC;
                } else {
                    state = State.NORMAL;
                    switch (ch) {
                        case 'D' -> IND();   // IND – Index
                        case 'E' -> NEL();   // NEL – Next Line
                        case 'M' -> RI();    // RI – Reverse Index
                        case '7' -> DECSC(); // DECSC – Save Cursor (DEC Private)
                        case '8' -> DECRC(); // DECRC – Restore Cursor (DEC Private)
                        case 'H' -> HTS();   // HTS – Horizontal Tabulation Set
                        case 'c' -> RIS();   // RIS – Reset To Initial State
                        case '=' -> {
                        }      // DECKPAM – Keypad Application Mode (DEC Private)
                        case '>' -> {
                        }      // DECKPNM – Keypad Numeric Mode (DEC Private)
                        default -> System.out.println("Invalid escape: " + ch);
                    }
                }
            }
            case CONTROL_SEQUENCE -> {
                if (ch >= '0' && ch <= '9') {
                    if (argCount < args.length) {
                        final int digit = ch - '0';
                        if (args[argCount] < (Integer.MAX_VALUE - digit) / 10) {
                            args[argCount] = args[argCount] * 10 + digit;
                        } else {
                            args[argCount] = Integer.MAX_VALUE;
                        }
                    }
                } else {
                    if (ch == '?') {
                        break; // Ignore ? intermediate character.
                    }

                    if (argCount < args.length) {
                        argCount++;
                    }

                    if (ch == ';') {
                        break; // Keep going, we have another argument.
                    }

                    state = State.NORMAL;
                    switch (ch) {
                        case 'A' -> CUU(); // CUU - Cursor Up
                        case 'B' -> CUD(); // CUD – Cursor Down
                        case 'C' -> CUF(); // CUF – Cursor Forward
                        case 'D' -> CUB(); // CUB – Cursor Backward
                        case 'H' -> CUP(); // CUP - Cursor Position
                        case 'f' -> HVP(); // HVP – Horizontal and Vertical Position
                        case 'm' -> SGR(); // SGR – Select Graphic Rendition
                        case 'K' -> EL();  // EL – Erase In Line
                        case 'J' -> ED();  // ED – Erase In Display
                        case 'r' -> DECSTBM(); // DECSTBM – Set Top and Bottom Margins (DEC Private)
                        case 'g' -> TBC(); // TBC – Tabulation Clear
                        case 'h' -> SM();  // SM – Set Mode
                        case 'l' -> RM();  // RM – Reset Mode
                        case 'n' -> DSR(); // DSR – Device Status Report
                        case 'c' -> DA();  // DA – Device Attributes
                        case 'd' -> {
                            setClampedCursorPos(x, args[0] - 1);
                        }
                        case 'G' -> {
                            setClampedCursorPos(args[0] - 1, y);
                        }
                        case 't' -> {
                            if (args[0] == 14) {
                                putInput("\033[4;80;24t");
                            }
                        }
                        case 'L' -> IL();
                        case 'M' -> DL();
                        case '>' -> {
                            if (args[0] == 6) {
                                putInput("\033[" + (y + 1) + ";" + (x + 1) + "R");
                                return;
                            } else if (args[0] == 0) {
                                //putInput("\033[0>"); // Don't respond?
                                return;
                            }
                            System.out.println("Invalid parameter: " + args[0]);
                        }
                        case '%' -> {
                            System.out.println("%: " + argCount);

                            for(int i = 0; i < argCount; i++) System.out.println(args[i]);
                        }
                        default -> {
                            System.out.println("Control sequence: " + ch);
                        }
                    }
                }
            }
            case SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET -> {
                state = State.NORMAL;
                switch (ch) {
                    case 'A' -> {
                    } // United Kingdom Set
                    case 'B' -> {
                    } // ASCII Set
                    case '0' -> {
                    } // Special Graphics
                    case '1' -> {
                    } // Alternate Character ROM Standard Character Set
                    case '2' -> {
                    } // Alternate Character ROM Special Graphics
                }
            }
            case HASH -> {
                state = State.NORMAL;
                switch (ch) {
                    case '3' -> {
                    } // Change this line to double-height top half (DECDHL)
                    case '4' -> {
                    } // Change this line to double-height bottom half (DECDHL)
                    case '5' -> {
                    } // Change this line to single-width single-height (DECSWL)
                    case '6' -> {
                    } // Change this line to double-width single-height (DECDWL)
                    case '8' -> { // Fill Screen with Es (DECALN)
                        Arrays.fill(buffer, (byte) 'E');
                        renderers.forEach(model -> model.getDirtyMask().set(-1));
                    }
                }
            }
            case DCS -> {
                if (lastChar == '\033' && ch == '\\') {
                    state = State.NORMAL;
                } else {
                    lastChar = ch;
                }
                // Used for mapping Function keys and possibly other things
            }
            case OSC -> {
                if (lastChar == '\033' && ch == '\\' || ch == '\007') {
                    state = State.NORMAL;
                } else {
                    lastChar = ch;
                }
            }
        }
    }

    private void IND() {
        if (y >= scrollLast) {
            shiftUpOne();
        } else {
            setCursorPos(x, y + 1);
        }
    }

    private void NEL() {
        if (y >= scrollLast) {
            shiftUpOne();
            setCursorPos(0, y);
        } else {
            setCursorPos(0, y + 1);
        }
    }

    private void RI() {
        if (y <= scrollFirst) {
            shiftDownOne();
        } else {
            setCursorPos(0, y - 1);
        }
    }

    private void DECSC() {
        savedX = x;
        savedY = y;
    }

    private void DECRC() {
        x = savedX;
        y = savedY;
    }

    private void IL() {
        int lines = args[0];

        shiftLines(y, scrollLast-1, (Math.max(1, lines)));
    }

    private void DL() {
        int lines = args[0];

        setCursorPos(0, y);

        for (int i = 0; i < Math.max(1, lines); i++) {
            clearLine(y + i);
        }

        shiftLines(y + (Math.max(1, lines)), scrollLast, -(Math.max(1, lines)));
    }

    private void HTS() {
        if (x >= 0 && x < WIDTH) {
            tabs[x] = true;
        }
    }

    private void RIS() {
        enable256 = false;
        trueColor = false;
        Use1006 = false;
        color = fromByte(DEFAULT_COLORS);
        backgroundColor = null;
        foregroundColor = null;
        style = DEFAULT_STYLE;
        CurrentMouseMode = MouseMode.None;
        clear();
        Arrays.fill(tabs, false);
        for (int i = 1; i < WIDTH; i++) {
            if (i % TAB_WIDTH == 0) {
                tabs[i] = true;
            }
        }
    }

    private void CUU() {
        setClampedCursorPos(x, y - Math.max(1, args[0]));
    }

    private void CUD() {
        setClampedCursorPos(x, y + Math.max(1, args[0]));
    }

    private void CUF() {
        setClampedCursorPos(x + Math.max(1, args[0]), y);
    }

    private void CUB() {
        setClampedCursorPos(x - Math.max(1, args[0]), y);
    }

    private void CUP() {
        setRelativeCursorPos(args[1] - 1, args[0] - 1);
    }

    private void HVP() {
        CUP();
    }

    private void SGR() {
        if (args[0] == 38 || args[0] == 48) {
            if (args[0] == 38) {
                if (args[1] == 5) {
                    // 256
                    enable256 = true;
                    trueColor = false;
                    foregroundColor256 = (byte) args[2];
                } else if (args[1] == 2) {
                    // true color
                    enable256 = false;
                    trueColor = true;
                    foregroundColor = new ColorData(args[2], args[3], args[4]);
                }
            } else {
                if (args[1] == 5) {
                    // 256
                    enable256 = true;
                    trueColor = false;
                    backgroundColor256 = (byte) args[2];
                } else if (args[1] == 2) {
                    // true color
                    enable256 = false;
                    trueColor = true;
                    backgroundColor = new ColorData(args[2], args[3], args[4]);
                }
            }

            return;
        }
        for (int i = 0; i < argCount; i++) {
            selectStyle(args[i]);
        }
    }

    private void EL() {
        switch (args[0]) {
            case 0 ->  // From cursor to end of line
                clearLine(y, x, WIDTH);
            case 1 ->  // From beginning of line to cursor
                clearLine(y, 0, x + 1);
            case 2 ->  // Entire line containing cursor
                clearLine(y);
        }
    }

    private void ED() {
        switch (args[0]) {
            case 0 -> {  // From cursor to end of screen
                clearLine(y, x, WIDTH);
                for (int iy = y + 1; iy < HEIGHT; iy++) {
                    clearLine(iy);
                }
            }
            case 1 -> {  // From beginning of screen to cursor
                for (int iy = 0; iy < y; iy++) {
                    clearLine(iy);
                }
                clearLine(y, 0, x + 1);
            }
            case 2 ->  // Entire screen
                clear();
        }
    }

    private void DECSTBM() {
        final int first, last;
        if (argCount == 2) {
            first = args[0] - 1;
            last = args[1] - 1;
        } else {
            first = 0;
            last = HEIGHT - 1;
        }
        if (first < 0 || last > HEIGHT - 1 || last - first <= 0) {
            return;
        }
        scrollFirst = first; // to index
        scrollLast = last; // to index
        setRelativeCursorPos(0, 0); // send cursor home
    }

    private void TBC() {
        switch (args[0]) {
            case 0 -> { // Clear tab at current column
                if (x >= 0 && x < WIDTH) {
                    tabs[x] = false;
                }
            }
            case 3 -> // Clear all tabs
                Arrays.fill(tabs, false);
        }
    }

    private void SM() {
        for (int i = 0; i < argCount; i++) {
            final int mode = args[i];
            if (mode == 1000) {
                CurrentMouseMode = MouseMode.Normal;
                System.out.println("Current mouse mode: " + CurrentMouseMode);
                continue;
            } else if (mode == 1001) {
                CurrentMouseMode = MouseMode.MouseHilite;
                System.out.println("Current mouse mode: " + CurrentMouseMode);
                continue;
            } else if (mode == 9) {
                CurrentMouseMode = MouseMode.X10Compat;
                System.out.println("Current mouse mode: " + CurrentMouseMode);
                continue;
            } else if (mode == 1005) {
                System.out.println("bad1");
            } else if (mode == 1006) {
                Use1006 = true;
                continue;
            } else if (mode == 1015) {
                System.out.println("bad3");
                continue;
            } else if (mode == 1016) {
                System.out.println("bad4");
                continue;
            }

            if (mode != 0) {
                setMode(mode);
            }
            if (mode == Mode.DECOM) {
                setRelativeCursorPos(0, 0);
            }
        }
    }

    private void RM() {
        for (int i = 0; i < argCount; i++) {
            final int mode = args[i];
            if (mode == 9 || mode == 1000 || mode == 1001) {
                CurrentMouseMode = MouseMode.None;
                System.out.println("Current mouse mode: " + CurrentMouseMode);
                continue;
            } else if (mode == 1006) {
                Use1006 = false;
                continue;
            }
            if (mode != 0) {
                resetMode(mode);
            }
            if (mode == Mode.DECOM) {
                setRelativeCursorPos(0, 0);
                clear();
            }
        }
    }

    private void DSR() {
        switch (args[0]) {
            case 5 -> // Report console status
                putResponse("\033[0n"); // Ready, No malfunctions detected
            case 6 -> { // Report cursor position
                if (getMode(Mode.DECOM)) {
                    putResponse(String.format("\033[%d;%dR", (y - scrollFirst) + 1, x + 1));
                } else {
                    putResponse(String.format("\033[%d;%dR", (y % HEIGHT) + 1, x + 1));
                }
            }
        }
    }

    private void DA() {
        putResponse("\033[?1;0c"); // No options.
    }

    private void setMode(final int mode) {
        modes |= 1 << mode;
    }

    private void resetMode(final int mode) {
        modes &= ~(1 << mode);
    }

    private boolean getMode(final int mode) {
        return (modes & (1 << mode)) != 0;
    }

    private void putResponse(final String value) {
        for (int i = 0; i < value.length(); i++) {
            putResponse((byte) value.charAt(i));
        }
    }

    private void putResponse(final byte value) {
        if (!displayOnly) {
            putInput(value);
        }
    }

    private void selectStyle(final int sgr) {
        switch (sgr) {
            case 0 -> { // Reset / Normal
                color = fromByte(DEFAULT_COLORS);
                style = DEFAULT_STYLE;
                enable256 = false;
                trueColor = false;
                foregroundColor256 = 0;
                backgroundColor256 = 0;
                foregroundColor = null;
                backgroundColor = null;
            }
            case 1 -> // Bold or increased intensity
                style |= STYLE_BOLD_MASK;
            case 2 -> // Faint or decreased intensity
                style |= STYLE_DIM_MASK;
            case 4 -> // Underscore
                style |= STYLE_UNDERLINE_MASK;
            case 5 -> // Blink
                style |= STYLE_BLINK_MASK;
            case 7 -> // Negative (reverse) image
                style |= STYLE_INVERT_MASK;
            case 8 -> // Conceal aka Hide
            {
                style |= STYLE_HIDDEN_MASK;
            }
            case 22 -> // Normal color or intensity
                style &= ~(STYLE_BOLD_MASK | STYLE_DIM_MASK);
            case 24 -> // Underline off
                style &= ~STYLE_UNDERLINE_MASK;
            case 25 -> // Blink off
                style &= ~STYLE_BLINK_MASK;
            case 27 -> // Reverse/invert off
                style &= ~STYLE_INVERT_MASK;
            case 28 -> // Reveal conceal off
            {
                style &= ~STYLE_HIDDEN_MASK;
            }
            case 30, 31, 32, 33, 34, 35, 36, 37 -> { // Set foreground color
                enable256 = false;
                trueColor = false;
                final int color = sgr - 30;
                System.out.println(color);
                this.color = fromByte((byte) ((this.color.R & ~(COLOR_MASK << COLOR_FOREGROUND_SHIFT)) | (color << COLOR_FOREGROUND_SHIFT)));
            }
            case 40, 41, 42, 43, 44, 45, 46, 47 -> { //–47 Set background color
                enable256 = false;
                trueColor = false;
                final int color = sgr - 40;
                this.color = fromByte((byte) ((this.color.R & ~COLOR_MASK) | color));
            }
        }
    }

    private void setRelativeCursorPos(final int x, final int y) {
        if (getMode(Mode.DECOM)) {
            setCursorPos(x, Math.min(scrollFirst + y, scrollLast));
        } else {
            setCursorPos(x, y);
        }
    }

    private void setClampedCursorPos(final int x, final int y) {
        setCursorPos(x, Math.max(scrollFirst, Math.min(scrollLast, y)));
    }

    private void setCursorPos(final int x, final int y) {
        this.x = Math.max(0, Math.min(WIDTH - 1, x));
        this.y = Math.max(0, Math.min(HEIGHT - 1, y));
    }

    private void putChar(final char ch) {
        if (Character.isISOControl(ch))
            return;

        if (x >= WIDTH) {
            if (getMode(Mode.DECAWM)) {
                NEL();
            } else {
                setCursorPos(WIDTH - 1, y);
            }
        }

        setChar(x, y, ch);
        x++;
    }

    private void setChar(final int x, final int y, final char ch) {
        final int index = x + y * WIDTH;

        buffer[index] = (byte) ch;
        if (!enable256 && !trueColor) {
            colorModes[index] = ColorMode.SIXTEEN_COLOR;
            colors[index] = color;
        } else if (enable256) {
            colorModes[index] = ColorMode.TWO_FIFTY_SIX_COLOR;
            colors[index] = new ColorData((backgroundColor256 & ~(0b11111111 << 8)) | (foregroundColor256 << 8), 0, 0);
        } else {
            colorModes[index] = ColorMode.TWENTY_FOUR_BIT_COLOR;
            colors[index] = (foregroundColor != null) ? foregroundColor : DEFAULT_TRUE_COLOR_FOREGROUND;
            colorsBackground[index] = (backgroundColor != null) ? backgroundColor : DEFAULT_TRUE_COLOR_BACKGROUND;
        }

        styles[index] = style;
        renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(1 << y, (prev, next) -> prev | next));
    }

    private void clear() {
        Arrays.fill(buffer, (byte) ' ');
        Arrays.fill(colorModes, ColorMode.SIXTEEN_COLOR);
        Arrays.fill(colors, fromByte(DEFAULT_COLORS));
        Arrays.fill(colorsBackground, fromByte(DEFAULT_COLORS));
        Arrays.fill(styles, DEFAULT_STYLE);
        setCursorPos(0, 0);
        renderers.forEach(model -> model.getDirtyMask().set(-1));
    }

    private void clearLine(final int y) {
        clearLine(y, 0, WIDTH);
    }

    private void clearLine(final int y, final int fromIndex, final int toIndex) {
        enable256 = false;
        trueColor = false;
        Arrays.fill(buffer, y * WIDTH + fromIndex, y * WIDTH + toIndex, (byte) ' ');
        Arrays.fill(colorModes, y * WIDTH + fromIndex, y * WIDTH + toIndex, ColorMode.SIXTEEN_COLOR);
        Arrays.fill(colors, y * WIDTH + fromIndex, y * WIDTH + toIndex, fromByte(DEFAULT_COLORS));
        Arrays.fill(colorsBackground, y * WIDTH + fromIndex, y * WIDTH + toIndex, fromByte(DEFAULT_COLORS));
        Arrays.fill(styles, y * WIDTH + fromIndex, y * WIDTH + toIndex, DEFAULT_STYLE);
        renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(1 << y, (prev, next) -> prev | next));
    }

    private void shiftUpOne() {
        shiftLines(scrollFirst + 1, scrollLast, -1);
    }

    private void shiftDownOne() {
        shiftLines(scrollFirst, scrollLast - 1, 1);
    }

    private void shiftLines(final int firstLine, final int lastLine, final int count) {
        if (count == 0)
            return;

        final int srcIndex = firstLine * WIDTH;
        final int charCount = (lastLine + 1) * WIDTH - srcIndex;
        final int dstIndex = srcIndex + count * WIDTH;

        System.arraycopy(buffer, srcIndex, buffer, dstIndex, charCount);
        System.arraycopy(colorModes, srcIndex, colorModes, dstIndex, charCount);
        System.arraycopy(colors, srcIndex, colors, dstIndex, charCount);
        System.arraycopy(colorsBackground, srcIndex, colorsBackground, dstIndex, charCount);
        System.arraycopy(styles, srcIndex, styles, dstIndex, charCount);

        final int clearIndex = count > 0 ? srcIndex : (dstIndex + charCount);
        final int clearCount = Math.abs(count * WIDTH);
        Arrays.fill(buffer, clearIndex, clearIndex + clearCount, (byte) ' ');
        // TODO Copy color and style from last line.
        // TODO Copy color and style from last line.
        Arrays.fill(colorModes, clearIndex, clearIndex + clearCount, ColorMode.SIXTEEN_COLOR);
        Arrays.fill(colors, clearIndex, clearIndex + clearCount, fromByte(DEFAULT_COLORS));
        Arrays.fill(colorsBackground, clearIndex, clearIndex + clearCount, fromByte(DEFAULT_COLORS));
        Arrays.fill(styles, clearIndex, clearIndex + clearCount, DEFAULT_STYLE);

        int dirtyLinesMask = 0;
        final int dirtyStart = Math.min(firstLine, firstLine + count);
        final int dirtyEnd = Math.max(lastLine, lastLine + count);
        for (int i = dirtyStart; i <= dirtyEnd; i++) {
            dirtyLinesMask |= 1 << i;
        }
        final int finalDirtyLinesMask = dirtyLinesMask;
        renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(finalDirtyLinesMask, (left, right) -> left | right));

        enable256 = false;
        trueColor = false;
    }

    /// ////////////////////////////////////////////////////////////////

    private interface RendererModel {
        AtomicInteger getDirtyMask();

        void close();
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Renderer implements RendererModel, RendererView {
        private static final ResourceLocation LOCATION_FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "textures/font/terminus.png");
        private static final int TEXTURE_RESOLUTION = 256;
        private static final float ONE_OVER_TEXTURE_RESOLUTION = 1.0f / (float) TEXTURE_RESOLUTION;
        private static final int TEXTURE_COLUMNS = 16;
        private static final int TEXTURE_BOLD_SHIFT = TEXTURE_COLUMNS; // Bold chars are in right half of texture.

        private static final int[] COLORS = {
            0x555555, // Black
            0xEE3322, // Red
            0x33DD44, // Green
            0xFFCC11, // Yellow
            0x1188EE, // Blue
            0xDD33CC, // Magenta
            0x22CCDD, // Cyan
            0xEEEEEE, // White
        };

        private static final int[] DIM_COLORS = {
            0x010101, // Black
            0x772211, // Red
            0x116622, // Green
            0x886611, // Yellow
            0x115588, // Blue
            0x771177, // Magenta
            0x116677, // Cyan
            0x777777, // White
        };

        private static final int[] COLORS_256 = {
            0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0, 0x808080, 0xff0000,
            0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff, 0x000000, 0x00005f, 0x000087, 0x0000af,
            0x0000d7, 0x0000ff, 0x005f00, 0x005f5f, 0x005f87, 0x005faf, 0x005fd7, 0x005fff, 0x008700, 0x00875f,
            0x008787, 0x0087af, 0x0087d7, 0x0087ff, 0x00af00, 0x00af5f, 0x00af87, 0x00afaf, 0x00afd7, 0x00afff,
            0x00d700, 0x00d75f, 0x00d787, 0x00d7af, 0x00d7d7, 0x00d7ff, 0x00ff00, 0x00ff5f, 0x00ff87, 0x00ffaf,
            0x00ffd7, 0x00ffff, 0x5f0000, 0x5f005f, 0x5f0087, 0x5f00af, 0x5f00d7, 0x5f00ff, 0x5f5f00, 0x5f5f5f,
            0x5f5f87, 0x5f5faf, 0x5f5fd7, 0x5f5fff, 0x5f8700, 0x5f875f, 0x5f8787, 0x5f87af, 0x5f87d7, 0x5f87ff,
            0x5faf00, 0x5faf5f, 0x5faf87, 0x5fafaf, 0x5fafd7, 0x5fafff, 0x5fd700, 0x5fd75f, 0x5fd787, 0x5fd7af,
            0x5fd7d7, 0x5fd7ff, 0x5fff00, 0x5fff5f, 0x5fff87, 0x5fffaf, 0x5fffd7, 0x5fffff, 0x870000, 0x87005f,
            0x870087, 0x8700af, 0x8700d7, 0x8700ff, 0x875f00, 0x875f5f, 0x875f87, 0x875faf, 0x875fd7, 0x875fff,
            0x878700, 0x87875f, 0x878787, 0x8787af, 0x8787d7, 0x8787ff, 0x87af00, 0x87af5f, 0x87af87, 0x87afaf,
            0x87afd7, 0x87afff, 0x87d700, 0x87d75f, 0x87d787, 0x87d7af, 0x87d7d7, 0x87d7ff, 0x87ff00, 0x87ff5f,
            0x87ff87, 0x87ffaf, 0x87ffd7, 0x87ffff, 0xaf0000, 0xaf005f, 0xaf0087, 0xaf00af, 0xaf00d7, 0xaf00ff,
            0xaf5f00, 0xaf5f5f, 0xaf5f87, 0xaf5faf, 0xaf5fd7, 0xaf5fff, 0xaf8700, 0xaf875f, 0xaf8787, 0xaf87af,
            0xaf87d7, 0xaf87ff, 0xafaf00, 0xafaf5f, 0xafaf87, 0xafafaf, 0xafafd7, 0xafafff, 0xafd700, 0xafd75f,
            0xafd787, 0xafd7af, 0xafd7d7, 0xafd7ff, 0xafff00, 0xafff5f, 0xafff87, 0xafffaf, 0xafffd7, 0xafffff,
            0xd70000, 0xd7005f, 0xd70087, 0xd700af, 0xd700d7, 0xd700ff, 0xd75f00, 0xd75f5f, 0xd75f87, 0xd75faf,
            0xd75fd7, 0xd75fff, 0xd78700, 0xd7875f, 0xd78787, 0xd787af, 0xd787d7, 0xd787ff, 0xdfaf00, 0xdfaf5f,
            0xdfaf87, 0xdfafaf, 0xdfafdf, 0xdfafff, 0xdfdf00, 0xdfdf5f, 0xdfdf87, 0xdfdfaf, 0xdfdfdf, 0xdfdfff,
            0xdfff00, 0xdfff5f, 0xdfff87, 0xdfffaf, 0xdfffdf, 0xdfffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af,
            0xff00df, 0xff00ff, 0xff5f00, 0xff5f5f, 0xff5f87, 0xff5faf, 0xff5fdf, 0xff5fff, 0xff8700, 0xff875f,
            0xff8787, 0xff87af, 0xff87df, 0xff87ff, 0xffaf00, 0xffaf5f, 0xffaf87, 0xffafaf, 0xffafdf, 0xffafff,
            0xffdf00, 0xffdf5f, 0xffdf87, 0xffdfaf, 0xffdfdf, 0xffdfff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf,
            0xffffdf, 0xffffff, 0x080808, 0x121212, 0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e,
            0x585858, 0x626262, 0x6c6c6c, 0x767676, 0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e, 0xa8a8a8, 0xb2b2b2,
            0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada, 0xe4e4e4, 0xeeeeee
        };

        /// ////////////////////////////////////////////////////////////

        private final OldTerminal terminal;
        private final VertexBuffer[] lines = new VertexBuffer[HEIGHT];

        private final AtomicInteger dirty = new AtomicInteger(-1);

        /// ////////////////////////////////////////////////////////////

        public Renderer(final OldTerminal terminal) {
            this.terminal = terminal;
        }

        /// ////////////////////////////////////////////////////////////

        @Override
        public void render(final PoseStack stack, final Matrix4f projectionMatrix) {
            validateLineCache();
            renderBuffer(stack, projectionMatrix);

            if ((System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500) {
                renderCursor(stack);
            }
        }

        @Override
        public AtomicInteger getDirtyMask() {
            return dirty;
        }

        @Override
        public void close() {
            for (int i = 0; i < lines.length; i++) {
                final VertexBuffer line = lines[i];
                if (line != null) {
                    line.close();
                    lines[i] = null;
                }
            }
        }

        /// ////////////////////////////////////////////////////////////

        private int findLineIndex(VertexBuffer[] vba, VertexBuffer vb) {
            int i = 0;
            while (i < vba.length) {
                if (vba[i] == vb) {
                    return i;
                }
                i++;
            }
            return -1;
        }

        private void renderBuffer(final PoseStack stack, final Matrix4f projectionMatrix) {
            final ShaderInstance shader = GameRenderer.getPositionTexColorShader();
            if (shader == null) {
                return;
            }

            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, LOCATION_FONT_TEXTURE);

            for (final VertexBuffer line : lines) {
                if (!line.isInvalid()) {
                    try {
                        line.bind();
                        line.drawWithShader(stack.last().pose(), projectionMatrix, shader);
                        VertexBuffer.unbind();
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.getMessage());
                        System.out.println(findLineIndex(lines, line));
                    }
                }
            }

            RenderSystem.depthMask(true);
        }

        private void validateLineCache() {
            if (dirty.get() == 0) {
                return;
            }


            final int mask = dirty.getAndSet(0);
            for (int row = 0; row < lines.length; row++) {
                if ((mask & (1 << row)) == 0) {
                    continue;
                }
                BufferBuilder builder = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                final Matrix4f matrix = new Matrix4f().translate(0, row * CHAR_HEIGHT, 0);

                renderBackground(matrix, builder, row);
                renderForeground(matrix, builder, row);

                MeshData rb = builder.buildOrThrow();

                if (lines[row] == null) {
                    lines[row] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                } else if (lines[row] != null) {
                    lines[row].close();
                    lines[row] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                }

                if (!lines[row].isInvalid()) {
                    lines[row].bind();
                    lines[row].upload(rb);
                    VertexBuffer.unbind();
                }
            }
        }

        private void renderBackground(final Matrix4f matrix, final BufferBuilder buffer, final int row) {
            // State tracking for drawing background quads spanning multiple characters.
            float backgroundStartX = -1;
            int backgroundColor = 0;

            float tx = 0f;
            for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
                final ColorMode colorMode = terminal.colorModes[index];
                final ColorData colors = terminal.colors[index];
                final byte style = terminal.styles[index];

                if ((style & STYLE_HIDDEN_MASK) != 0) continue;

                final int[] palette = (style & STYLE_DIM_MASK) != 0 ? DIM_COLORS : COLORS;
                int background;
                int foregroundIndex;
                int backgroundIndex;
                switch (colorMode) {
                    case SIXTEEN_COLOR:
                        foregroundIndex = (colors.R >> COLOR_FOREGROUND_SHIFT) & COLOR_MASK;
                        backgroundIndex = colors.R & COLOR_MASK;
                        background = palette[(style & STYLE_INVERT_MASK) == 0 ? backgroundIndex : foregroundIndex];
                        break;
                    case TWO_FIFTY_SIX_COLOR:
                        foregroundIndex = (char)((colors.R >> 8) & 0b11111111);
                        backgroundIndex = (char)(colors.R & 0b11111111);
                        background = COLORS_256[(style & STYLE_INVERT_MASK) == 0 ? backgroundIndex : foregroundIndex];
                        break;
                    case TWENTY_FOUR_BIT_COLOR:
                        background = terminal.colorsBackground[index].asInt();
                        break;
                    default:
                        background = colors.R & COLOR_MASK;
                        break;
                }

                final boolean hadBackground = backgroundStartX >= 0;
                final boolean hasBackground = background != palette[0];
                if (!hadBackground && hasBackground) {
                    backgroundStartX = tx;
                    backgroundColor = background;
                } else if (hadBackground && (!hasBackground || backgroundColor != background)) {
                    renderBackground(matrix, buffer, backgroundStartX, tx, backgroundColor);

                    if (hasBackground) {
                        backgroundStartX = tx;
                        backgroundColor = background;
                    } else {
                        backgroundStartX = -1;
                    }
                }

                tx += CHAR_WIDTH;
            }

            if (backgroundStartX >= 0) {
                renderBackground(matrix, buffer, backgroundStartX, tx, backgroundColor);
            }
        }

        private void renderBackground(final Matrix4f matrix, final BufferBuilder buffer, final float x0, final float x1, final int color) {
            final float r = ((color >> 16) & 0xFF) / 255f;
            final float g = ((color >> 8) & 0xFF) / 255f;
            final float b = (color & 0xFF) / 255f;

            final float ulu = (TEXTURE_RESOLUTION - 1) / (float) TEXTURE_RESOLUTION;
            final float ulv = 1 / (float) TEXTURE_RESOLUTION;

            buffer.addVertex(matrix, x0, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
            buffer.addVertex(matrix, x1, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
            buffer.addVertex(matrix, x1, 0, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
            buffer.addVertex(matrix, x0, 0, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
        }

        private void renderForeground(final Matrix4f matrix, final BufferBuilder buffer, final int row) {
            float tx = 0f;
            for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
                final ColorMode colorMode = terminal.colorModes[index];
                final ColorData color = terminal.colors[index];
                final byte style = terminal.styles[index];

                if ((style & STYLE_HIDDEN_MASK) != 0) continue;

                int foreground;
                int foregroundIndex;
                int backgroundIndex;
                switch (colorMode) {
                    case SIXTEEN_COLOR:
                        final int[] palette = (style & STYLE_DIM_MASK) != 0 ? DIM_COLORS : COLORS;
                        foregroundIndex = (color.R >> COLOR_FOREGROUND_SHIFT) & COLOR_MASK;
                        backgroundIndex = color.R & COLOR_MASK;
                        foreground = palette[(style & STYLE_INVERT_MASK) == 0 ? foregroundIndex : backgroundIndex];
                        break;
                    case TWO_FIFTY_SIX_COLOR:
                        foregroundIndex = (char)((color.R >> 8) & 0b11111111);
                        backgroundIndex = (char)(color.R & 0b11111111);
                        foreground = COLORS_256[(style & STYLE_INVERT_MASK) == 0 ? foregroundIndex : backgroundIndex];
                        break;
                    case TWENTY_FOUR_BIT_COLOR:
                        foreground = terminal.colors[index].asInt();
                        break;
                    default:
                        foreground = color.R & COLOR_MASK;
                        break;
                }

                final int character = terminal.buffer[index] & 0xFF;

                renderForeground(matrix, buffer, tx, character, foreground, style);

                tx += CHAR_WIDTH;
            }
        }

        private void renderForeground(final Matrix4f matrix, final BufferBuilder buffer, final float offset, final int character, final int color, final byte style) {
            final float r = ((color >> 16) & 0xFF) / 255f;
            final float g = ((color >> 8) & 0xFF) / 255f;
            final float b = (color & 0xFF) / 255f;

            if (isPrintableCharacter((char) character)) {
                final int x = character % TEXTURE_COLUMNS + ((style & STYLE_BOLD_MASK) != 0 ? TEXTURE_BOLD_SHIFT : 0);
                final int y = character / TEXTURE_COLUMNS;
                final float u0 = x * (CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION);
                final float u1 = (x + 1) * (CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION);
                final float v0 = y * (CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION);
                final float v1 = (y + 1) * (CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION);

                buffer.addVertex(matrix, offset, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(u0, v1);
                buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(u1, v1);
                buffer.addVertex(matrix, offset + CHAR_WIDTH, 0, 0).setColor(r, g, b, 1).setUv(u1, v0);
                buffer.addVertex(matrix, offset, 0, 0).setColor(r, g, b, 1).setUv(u0, v0);
            }

            if ((style & STYLE_UNDERLINE_MASK) != 0) {
                final float ulu = (TEXTURE_RESOLUTION - 1) / (float) TEXTURE_RESOLUTION;
                final float ulv = 1 / (float) TEXTURE_RESOLUTION;

                buffer.addVertex(matrix, offset, CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
                buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
                buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
                buffer.addVertex(matrix, offset, CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(ulu, ulv);
            }
        }

        private void renderCursor(final PoseStack stack) {
            BufferUploader.reset();
            if (terminal.x < 0 || terminal.x >= WIDTH || terminal.y < 0 || terminal.y >= HEIGHT) {
                return;
            }

            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            stack.pushPose();
            stack.translate(terminal.x * CHAR_WIDTH, terminal.y * CHAR_HEIGHT, 0);

            final Matrix4f matrix = stack.last().pose();
            final BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            final int foreground = COLORS[Color.WHITE];
            final float r = ((foreground >> 16) & 0xFF) / 255f;
            final float g = ((foreground >> 8) & 0xFF) / 255f;
            final float b = ((foreground) & 0xFF) / 255f;

            buffer.addVertex(matrix, 0, CHAR_HEIGHT, 0).setColor(r, g, b, 1);
            buffer.addVertex(matrix, CHAR_WIDTH, CHAR_HEIGHT, 0).setColor(r, g, b, 1);
            buffer.addVertex(matrix, CHAR_WIDTH, 0, 0).setColor(r, g, b, 1);
            buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);

            MeshData rb = buffer.buildOrThrow();
            BufferUploader.drawWithShader(rb);

            stack.popPose();

            RenderSystem.depthMask(true);
        }

        private static boolean isPrintableCharacter(final char ch) {
            return ch == 0 ||
                (ch > ' ' && ch <= '~') ||
                ch >= 177;
        }
    }
}


