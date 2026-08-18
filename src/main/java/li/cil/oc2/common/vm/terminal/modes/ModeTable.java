package li.cil.oc2.common.vm.terminal.modes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Single source of truth for the terminal mode table: the 74 DEC private modes handled by
 * DECSET/DECRST ({@code CSI ? N h/l}) and the 4 ANSI modes handled by SM/RM ({@code CSI N h/l}).
 *
 * <p>The {@link #get(PrivateModeState)}/{@link #set(PrivateModeState, boolean)} accessors let the
 * CSI handlers ({@code CH1}/{@code CH2}/{@code CH3}/{@code CH6}) apply set/reset/save/restore
 * generically instead of duplicating a ~74-case switch in every file. Mode numbers 2/4/12 are
 * context-dependent ({@code ?} selects the private mode, otherwise the ANSI mode) and are resolved
 * via {@link #forPrivateMode(int)} vs {@link #forAnsiMode(int)}.
 */
public enum ModeTable { // NOPMD: inherently large data-driven 1:1 mode-number lookup table
    // --- ANSI modes (SM/RM, no '?' modifier) ---
    KAM(Mode.KAM, Kind.ANSI, true),
    IRM(Mode.IRM, Kind.ANSI, true),
    SRM(Mode.SRM, Kind.ANSI, true),
    LNM(Mode.LNM, Kind.ANSI, true),

    // --- DEC private modes (DECSET/DECRST, '?' modifier) ---
    DECCKM(PrivateMode.DECCKM, Kind.PRIVATE, true),
    DECANM(PrivateMode.DECANM, Kind.PRIVATE, false),
    DECCOLM(PrivateMode.DECCOLM, Kind.PRIVATE, false),
    DECSCLM(PrivateMode.DECSCLM, Kind.PRIVATE, true),
    DECSCNM(PrivateMode.DECSCNM, Kind.PRIVATE, true),
    DECOM(PrivateMode.DECOM, Kind.PRIVATE, true),
    DECAWM(PrivateMode.DECAWM, Kind.PRIVATE, true),
    DECARM(PrivateMode.DECARM, Kind.PRIVATE, true),
    X10MM(PrivateMode.X10MM, Kind.PRIVATE, false),
    TOOLBAR(PrivateMode.TOOLBAR, Kind.PRIVATE, false),
    START_BLINKING_CURSOR(PrivateMode.START_BLINKING_CURSOR, Kind.PRIVATE, true),
    START_BLINKING_CURSOR2(PrivateMode.START_BLINKING_CURSOR2, Kind.PRIVATE, true),
    XORBLINK(PrivateMode.XORBLINK, Kind.PRIVATE, false),
    DECPFF(PrivateMode.DECPFF, Kind.PRIVATE, false),
    DECPEX(PrivateMode.DECPEX, Kind.PRIVATE, false),
    DECTCEM(PrivateMode.DECTCEM, Kind.PRIVATE, true),
    SHOW_SCROLL(PrivateMode.SHOW_SCROLL, Kind.PRIVATE, false),
    FONT_SHIFT(PrivateMode.FONT_SHIFT, Kind.PRIVATE, false),
    TEKTRONIX(PrivateMode.TEKTRONIX, Kind.PRIVATE, false),
    ENABLE_80_132(PrivateMode.ENABLE_80_132, Kind.PRIVATE, false),
    MORE_FIX(PrivateMode.MORE_FIX, Kind.PRIVATE, false),
    DECNRCM(PrivateMode.DECNRCM, Kind.PRIVATE, false),
    DECGEPM(PrivateMode.DECGEPM, Kind.PRIVATE, false),
    MARG_BELL(PrivateMode.MARG_BELL, Kind.PRIVATE, false),
    XTREVWRAP(PrivateMode.XTREVWRAP, Kind.PRIVATE, false),
    XTLOGGING(PrivateMode.XTLOGGING, Kind.PRIVATE, false),
    ALT_BUFFER(PrivateMode.ALT_BUFFER, Kind.PRIVATE, true),
    DECNKM(PrivateMode.DECNKM, Kind.PRIVATE, false),
    DECBKM(PrivateMode.DECBKM, Kind.PRIVATE, false),
    DECLRMM(PrivateMode.DECLRMM, Kind.PRIVATE, false),
    DECSDM(PrivateMode.DECSDM, Kind.PRIVATE, false),
    DECNCSM(PrivateMode.DECNCSM, Kind.PRIVATE, false),
    X11MM(PrivateMode.X11MM, Kind.PRIVATE, true),
    HILITE_MOUSE(PrivateMode.HILITE_MOUSE, Kind.PRIVATE, false),
    CELL_MOTION_MOUSE(PrivateMode.CELL_MOTION_MOUSE, Kind.PRIVATE, true),
    ALL_MOTION_MOUSE_TRACKING(PrivateMode.ALL_MOTION_MOUSE_TRACKING, Kind.PRIVATE, false),
    FOCUS_IN_FOCUS_OUT(PrivateMode.FOCUS_IN_FOCUS_OUT, Kind.PRIVATE, true),
    UTF8_MOUSE(PrivateMode.UTF8_MOUSE, Kind.PRIVATE, true),
    SGR_MOUSE(PrivateMode.SGR_MOUSE, Kind.PRIVATE, true),
    ALTERNATE_SCROLL_MODE(PrivateMode.ALTERNATE_SCROLL_MODE, Kind.PRIVATE, false),
    SCROLL_BOTTOM_ON_OUTPUT(PrivateMode.SCROLL_BOTTOM_ON_OUTPUT, Kind.PRIVATE, false),
    SCROLL_BOTTOM_ON_KEY_PRESS(PrivateMode.SCROLL_BOTTOM_ON_KEY_PRESS, Kind.PRIVATE, false),
    FAST_SCROLL(PrivateMode.FAST_SCROLL, Kind.PRIVATE, false),
    URXVT_MOUSE(PrivateMode.URXVT_MOUSE, Kind.PRIVATE, true),
    SGR_MOUSE_PIXEL(PrivateMode.SGR_MOUSE_PIXEL, Kind.PRIVATE, false),
    META_KEY(PrivateMode.META_KEY, Kind.PRIVATE, false),
    SPECIAL_MODIFIERS(PrivateMode.SPECIAL_MODIFIERS, Kind.PRIVATE, false),
    META_SENDS_ESCAPE(PrivateMode.META_SENDS_ESCAPE, Kind.PRIVATE, false),
    DEL_EDIT_KEYPAD_DEL(PrivateMode.DEL_EDIT_KEYPAD_DEL, Kind.PRIVATE, false),
    ALT_SENDS_ESC(PrivateMode.ALT_SENDS_ESC, Kind.PRIVATE, false),
    KEEP_SELECTION(PrivateMode.KEEP_SELECTION, Kind.PRIVATE, false),
    USE_CLIP(PrivateMode.USE_CLIP, Kind.PRIVATE, false),
    ENABLE_URGENCY(PrivateMode.ENABLE_URGENCY, Kind.PRIVATE, false),
    RAISE_ON_CTRL_G(PrivateMode.RAISE_ON_CTRL_G, Kind.PRIVATE, false),
    KEEP_CLIP(PrivateMode.KEEP_CLIP, Kind.PRIVATE, false),
    EXT_REV_WRAP(PrivateMode.EXT_REV_WRAP, Kind.PRIVATE, false),
    ALLOW_ALT_BUFFER(PrivateMode.ALLOW_ALT_BUFFER, Kind.PRIVATE, false),
    SWITCH_ALT_BUFFER(PrivateMode.SWITCH_ALT_BUFFER, Kind.PRIVATE, true),
    SAVE_CURSOR(PrivateMode.SAVE_CURSOR, Kind.PRIVATE, true),
    SAVE_CLEAR_AND_SWITCH(PrivateMode.SAVE_CLEAR_AND_SWITCH, Kind.PRIVATE, true),
    SET_TERMINFO_FUNC_KEY_MODE(PrivateMode.SET_TERMINFO_FUNC_KEY_MODE, Kind.PRIVATE, false),
    SET_SUN_KEY_MODE(PrivateMode.SET_SUN_KEY_MODE, Kind.PRIVATE, false),
    SET_HP_K0EY_MODE(PrivateMode.SET_HP_K0EY_MODE, Kind.PRIVATE, false),
    SET_SCO_KEY_MODE(PrivateMode.SET_SCO_KEY_MODE, Kind.PRIVATE, false),
    SET_LEGACY_KEYBOARD(PrivateMode.SET_LEGACY_KEYBOARD, Kind.PRIVATE, false),
    SET_VT220_KEYBOARD(PrivateMode.SET_VT220_KEYBOARD, Kind.PRIVATE, false),
    ENABLE_READLINE_MOUSE_1(PrivateMode.ENABLE_READLINE_MOUSE_1, Kind.PRIVATE, false),
    ENABLE_READLINE_MOUSE_2(PrivateMode.ENABLE_READLINE_MOUSE_2, Kind.PRIVATE, false),
    ENABLE_READLINE_MOUSE_3(PrivateMode.ENABLE_READLINE_MOUSE_3, Kind.PRIVATE, false),
    SET_BRACKETED_PASTE(PrivateMode.SET_BRACKETED_PASTE, Kind.PRIVATE, true),
    ENABLE_READLINE_CHAR_QUOTE(PrivateMode.ENABLE_READLINE_CHAR_QUOTE, Kind.PRIVATE, false),
    ENABLE_READLINE_NEWLINE_PASTE(PrivateMode.ENABLE_READLINE_NEWLINE_PASTE, Kind.PRIVATE, false),
    APPLICATION_SYNC(PrivateMode.APPLICATION_SYNC, Kind.PRIVATE, true),
    APPLICATION_ESC_MODE(PrivateMode.APPLICATION_ESC_MODE, Kind.PRIVATE, true);

    private static final Map<Integer, ModeTable> PRIVATE_BY_NUMBER = byNumber(Kind.PRIVATE);

    private static final Map<Integer, ModeTable> ANSI_BY_NUMBER = byNumber(Kind.ANSI);

    private static Map<Integer, ModeTable> byNumber(final Kind kind) {
        return Arrays.stream(values())
                .filter(mode -> mode.kind == kind)
                .collect(Collectors.toUnmodifiableMap(ModeTable::getNumber, mode -> mode));
    }

    private final int number;
    private final Kind kind;
    private final boolean implemented;

    ModeTable(final int number, final Kind kind, final boolean implemented) {
        this.number = number;
        this.kind = kind;
        this.implemented = implemented;
    }

    public int getNumber() {
        return number;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isImplemented() {
        return implemented;
    }

    /**
     * Looks up a DEC private mode by its number, for {@code CSI ? N ...} sequences where the number
     * denotes a private mode.
     *
     * @param number the mode number from the CSI parameter.
     * @return the matching private mode, or {@code null} when the number is not a private mode.
     */
    @Nullable
    public static ModeTable forPrivateMode(final int number) {
        return PRIVATE_BY_NUMBER.get(number);
    }

    /**
     * Looks up an ANSI mode by its number, for {@code CSI N ...} sequences without the
     * {@code ?} modifier.
     *
     * @param number the mode number from the CSI parameter.
     * @return the matching ANSI mode, or {@code null} when the number is not an ANSI mode.
     */
    @Nullable
    public static ModeTable forAnsiMode(final int number) {
        return ANSI_BY_NUMBER.get(number);
    }

    /**
     * Reads the current value of this private mode from a state object.
     *
     * @param state the state to read from.
     * @return the current boolean value of this mode.
     * @throws IllegalStateException when called on a non-private mode.
     */
    public boolean get(final PrivateModeState state) { // NOPMD: 1:1 data-mapping switch
        return switch (this) {
            case DECCKM -> state.DECCKM;
            case DECANM -> state.DECANM;
            case DECCOLM -> state.DECCOLM;
            case DECSCLM -> state.DECSCLM;
            case DECSCNM -> state.DECSCNM;
            case DECOM -> state.DECOM;
            case DECAWM -> state.DECAWM;
            case DECARM -> state.DECARM;
            case X10MM -> state.X10MM;
            case TOOLBAR -> state.TOOLBAR;
            case START_BLINKING_CURSOR -> state.START_BLINKING_CURSOR;
            case START_BLINKING_CURSOR2 -> state.START_BLINKING_CURSOR2;
            case XORBLINK -> state.XORBLINK;
            case DECPFF -> state.DECPFF;
            case DECPEX -> state.DECPEX;
            case DECTCEM -> state.DECTCEM;
            case SHOW_SCROLL -> state.SHOW_SCROLL;
            case FONT_SHIFT -> state.FONT_SHIFT;
            case TEKTRONIX -> state.TEKTRONIX;
            case ENABLE_80_132 -> state.ENABLE_80_132;
            case MORE_FIX -> state.MORE_FIX;
            case DECNRCM -> state.DECNRCM;
            case DECGEPM -> state.DECGEPM;
            case MARG_BELL -> state.MARG_BELL;
            case XTREVWRAP -> state.XTREVWRAP;
            case XTLOGGING -> state.XTLOGGING;
            case ALT_BUFFER -> state.ALT_BUFFER;
            case DECNKM -> state.DECNKM;
            case DECBKM -> state.DECBKM;
            case DECLRMM -> state.DECLRMM;
            case DECSDM -> state.DECSDM;
            case DECNCSM -> state.DECNCSM;
            case X11MM -> state.X11MM;
            case HILITE_MOUSE -> state.HILITE_MOUSE;
            case CELL_MOTION_MOUSE -> state.CELL_MOTION_MOUSE;
            case ALL_MOTION_MOUSE_TRACKING -> state.ALL_MOTION_MOUSE_TRACKING;
            case FOCUS_IN_FOCUS_OUT -> state.FOCUS_IN_FOCUS_OUT;
            case UTF8_MOUSE -> state.UTF8_MOUSE;
            case SGR_MOUSE -> state.SGR_MOUSE;
            case ALTERNATE_SCROLL_MODE -> state.ALTERNATE_SCROLL_MODE;
            case SCROLL_BOTTOM_ON_OUTPUT -> state.SCROLL_BOTTOM_ON_OUTPUT;
            case SCROLL_BOTTOM_ON_KEY_PRESS -> state.SCROLL_BOTTOM_ON_KEY_PRESS;
            case FAST_SCROLL -> state.FAST_SCROLL;
            case URXVT_MOUSE -> state.URXVT_MOUSE;
            case SGR_MOUSE_PIXEL -> state.SGR_MOUSE_PIXEL;
            case META_KEY -> state.META_KEY;
            case SPECIAL_MODIFIERS -> state.SPECIAL_MODIFIERS;
            case META_SENDS_ESCAPE -> state.META_SENDS_ESCAPE;
            case DEL_EDIT_KEYPAD_DEL -> state.DEL_EDIT_KEYPAD_DEL;
            case ALT_SENDS_ESC -> state.ALT_SENDS_ESC;
            case KEEP_SELECTION -> state.KEEP_SELECTION;
            case USE_CLIP -> state.USE_CLIP;
            case ENABLE_URGENCY -> state.ENABLE_URGENCY;
            case RAISE_ON_CTRL_G -> state.RAISE_ON_CTRL_G;
            case KEEP_CLIP -> state.KEEP_CLIP;
            case EXT_REV_WRAP -> state.EXT_REV_WRAP;
            case ALLOW_ALT_BUFFER -> state.ALLOW_ALT_BUFFER;
            case SWITCH_ALT_BUFFER -> state.SWITCH_ALT_BUFFER;
            case SAVE_CURSOR -> state.SAVE_CURSOR;
            case SAVE_CLEAR_AND_SWITCH -> state.SAVE_CLEAR_AND_SWITCH;
            case SET_TERMINFO_FUNC_KEY_MODE -> state.SET_TERMINFO_FUNC_KEY_MODE;
            case SET_SUN_KEY_MODE -> state.SET_SUN_KEY_MODE;
            case SET_HP_K0EY_MODE -> state.SET_HP_K0EY_MODE;
            case SET_SCO_KEY_MODE -> state.SET_SCO_KEY_MODE;
            case SET_LEGACY_KEYBOARD -> state.SET_LEGACY_KEYBOARD;
            case SET_VT220_KEYBOARD -> state.SET_VT220_KEYBOARD;
            case ENABLE_READLINE_MOUSE_1 -> state.ENABLE_READLINE_MOUSE_1;
            case ENABLE_READLINE_MOUSE_2 -> state.ENABLE_READLINE_MOUSE_2;
            case ENABLE_READLINE_MOUSE_3 -> state.ENABLE_READLINE_MOUSE_3;
            case SET_BRACKETED_PASTE -> state.SET_BRACKETED_PASTE;
            case ENABLE_READLINE_CHAR_QUOTE -> state.ENABLE_READLINE_CHAR_QUOTE;
            case ENABLE_READLINE_NEWLINE_PASTE -> state.ENABLE_READLINE_NEWLINE_PASTE;
            case APPLICATION_SYNC -> state.APPLICATION_SYNC;
            case APPLICATION_ESC_MODE -> state.APPLICATION_ESC_MODE;
            default -> throw new IllegalStateException("Not a private mode: " + this);
        };
    }

    /**
     * Writes the value of this private mode into a state object.
     *
     * @param state the state to write to.
     * @param value the boolean value to store.
     * @throws IllegalStateException when called on a non-private mode.
     */
    public void set(final PrivateModeState state, final boolean value) { // NOPMD: 1:1 data-mapping switch
        switch (this) {
            case DECCKM -> state.DECCKM = value;
            case DECANM -> state.DECANM = value;
            case DECCOLM -> state.DECCOLM = value;
            case DECSCLM -> state.DECSCLM = value;
            case DECSCNM -> state.DECSCNM = value;
            case DECOM -> state.DECOM = value;
            case DECAWM -> state.DECAWM = value;
            case DECARM -> state.DECARM = value;
            case X10MM -> state.X10MM = value;
            case TOOLBAR -> state.TOOLBAR = value;
            case START_BLINKING_CURSOR -> state.START_BLINKING_CURSOR = value;
            case START_BLINKING_CURSOR2 -> state.START_BLINKING_CURSOR2 = value;
            case XORBLINK -> state.XORBLINK = value;
            case DECPFF -> state.DECPFF = value;
            case DECPEX -> state.DECPEX = value;
            case DECTCEM -> state.DECTCEM = value;
            case SHOW_SCROLL -> state.SHOW_SCROLL = value;
            case FONT_SHIFT -> state.FONT_SHIFT = value;
            case TEKTRONIX -> state.TEKTRONIX = value;
            case ENABLE_80_132 -> state.ENABLE_80_132 = value;
            case MORE_FIX -> state.MORE_FIX = value;
            case DECNRCM -> state.DECNRCM = value;
            case DECGEPM -> state.DECGEPM = value;
            case MARG_BELL -> state.MARG_BELL = value;
            case XTREVWRAP -> state.XTREVWRAP = value;
            case XTLOGGING -> state.XTLOGGING = value;
            case ALT_BUFFER -> state.ALT_BUFFER = value;
            case DECNKM -> state.DECNKM = value;
            case DECBKM -> state.DECBKM = value;
            case DECLRMM -> state.DECLRMM = value;
            case DECSDM -> state.DECSDM = value;
            case DECNCSM -> state.DECNCSM = value;
            case X11MM -> state.X11MM = value;
            case HILITE_MOUSE -> state.HILITE_MOUSE = value;
            case CELL_MOTION_MOUSE -> state.CELL_MOTION_MOUSE = value;
            case ALL_MOTION_MOUSE_TRACKING -> state.ALL_MOTION_MOUSE_TRACKING = value;
            case FOCUS_IN_FOCUS_OUT -> state.FOCUS_IN_FOCUS_OUT = value;
            case UTF8_MOUSE -> state.UTF8_MOUSE = value;
            case SGR_MOUSE -> state.SGR_MOUSE = value;
            case ALTERNATE_SCROLL_MODE -> state.ALTERNATE_SCROLL_MODE = value;
            case SCROLL_BOTTOM_ON_OUTPUT -> state.SCROLL_BOTTOM_ON_OUTPUT = value;
            case SCROLL_BOTTOM_ON_KEY_PRESS -> state.SCROLL_BOTTOM_ON_KEY_PRESS = value;
            case FAST_SCROLL -> state.FAST_SCROLL = value;
            case URXVT_MOUSE -> state.URXVT_MOUSE = value;
            case SGR_MOUSE_PIXEL -> state.SGR_MOUSE_PIXEL = value;
            case META_KEY -> state.META_KEY = value;
            case SPECIAL_MODIFIERS -> state.SPECIAL_MODIFIERS = value;
            case META_SENDS_ESCAPE -> state.META_SENDS_ESCAPE = value;
            case DEL_EDIT_KEYPAD_DEL -> state.DEL_EDIT_KEYPAD_DEL = value;
            case ALT_SENDS_ESC -> state.ALT_SENDS_ESC = value;
            case KEEP_SELECTION -> state.KEEP_SELECTION = value;
            case USE_CLIP -> state.USE_CLIP = value;
            case ENABLE_URGENCY -> state.ENABLE_URGENCY = value;
            case RAISE_ON_CTRL_G -> state.RAISE_ON_CTRL_G = value;
            case KEEP_CLIP -> state.KEEP_CLIP = value;
            case EXT_REV_WRAP -> state.EXT_REV_WRAP = value;
            case ALLOW_ALT_BUFFER -> state.ALLOW_ALT_BUFFER = value;
            case SWITCH_ALT_BUFFER -> state.SWITCH_ALT_BUFFER = value;
            case SAVE_CURSOR -> state.SAVE_CURSOR = value;
            case SAVE_CLEAR_AND_SWITCH -> state.SAVE_CLEAR_AND_SWITCH = value;
            case SET_TERMINFO_FUNC_KEY_MODE -> state.SET_TERMINFO_FUNC_KEY_MODE = value;
            case SET_SUN_KEY_MODE -> state.SET_SUN_KEY_MODE = value;
            case SET_HP_K0EY_MODE -> state.SET_HP_K0EY_MODE = value;
            case SET_SCO_KEY_MODE -> state.SET_SCO_KEY_MODE = value;
            case SET_LEGACY_KEYBOARD -> state.SET_LEGACY_KEYBOARD = value;
            case SET_VT220_KEYBOARD -> state.SET_VT220_KEYBOARD = value;
            case ENABLE_READLINE_MOUSE_1 -> state.ENABLE_READLINE_MOUSE_1 = value;
            case ENABLE_READLINE_MOUSE_2 -> state.ENABLE_READLINE_MOUSE_2 = value;
            case ENABLE_READLINE_MOUSE_3 -> state.ENABLE_READLINE_MOUSE_3 = value;
            case SET_BRACKETED_PASTE -> state.SET_BRACKETED_PASTE = value;
            case ENABLE_READLINE_CHAR_QUOTE -> state.ENABLE_READLINE_CHAR_QUOTE = value;
            case ENABLE_READLINE_NEWLINE_PASTE -> state.ENABLE_READLINE_NEWLINE_PASTE = value;
            case APPLICATION_SYNC -> state.APPLICATION_SYNC = value;
            case APPLICATION_ESC_MODE -> state.APPLICATION_ESC_MODE = value;
            default -> throw new IllegalStateException("Not a private mode: " + this);
        }
    }

    public enum Kind {
        PRIVATE,
        ANSI
    }
}