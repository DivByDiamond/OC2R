package li.cil.oc2.common.vm.terminal.modes;

import li.cil.ceres.api.Serialized;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

@Serialized
public class PrivateModeState {
    public boolean DECCKM = false;
    public boolean DECANM = false;
    public boolean DECCOLM = false;
    public boolean DECSCLM = false;
    public boolean DECSCNM = false;
    public boolean DECOM = false;
    public boolean DECAWM = true;
    public boolean DECARM = true;
    public boolean X10MM = false;
    public boolean TOOLBAR = false;
    public boolean START_BLINKING_CURSOR = false;
    public boolean START_BLINKING_CURSOR2 = false;
    public boolean XORBLINK = false;
    public boolean DECPFF = false;
    public boolean DECPEX = false;
    public boolean DECTCEM = true;
    public boolean SHOW_SCROLL = false;
    public boolean FONT_SHIFT = false;
    public boolean TEKTRONIX = false;
    public boolean ENABLE_80_132 = false;
    public boolean MORE_FIX = false;
    public boolean DECNRCM = false;
    public boolean DECGEPM = false;
    public boolean MARG_BELL = false;
    public boolean XTREVWRAP = false;
    public boolean XTLOGGING = false;
    public boolean ALT_BUFFER = false;
    public boolean DECNKM = false;
    public boolean DECBKM = false;
    public boolean DECLRMM = false;
    public boolean DECSDM = false;
    public boolean DECNCSM = false;
    public boolean X11MM = false;
    public boolean HILITE_MOUSE = false;
    public boolean CELL_MOTION_MOUSE = false;
    public boolean ALL_MOTION_MOUSE_TRACKING = false;
    public boolean FOCUS_IN_FOCUS_OUT = false;
    public boolean UTF8_MOUSE = false;
    public boolean SGR_MOUSE = false;
    public boolean ALTERNATE_SCROLL_MODE = false;
    public boolean SCROLL_BOTTOM_ON_OUTPUT = false;
    public boolean SCROLL_BOTTOM_ON_KEY_PRESS = false;
    public boolean FAST_SCROLL = false;
    public boolean URXVT_MOUSE = false;
    public boolean SGR_MOUSE_PIXEL = false;
    public boolean META_KEY = false;
    public boolean SPECIAL_MODIFIERS = false;
    public boolean META_SENDS_ESCAPE = false;
    public boolean DEL_EDIT_KEYPAD_DEL = false;
    public boolean ALT_SENDS_ESC = false;
    public boolean KEEP_SELECTION = false;
    public boolean USE_CLIP = false;
    public boolean ENABLE_URGENCY = false;
    public boolean RAISE_ON_CTRL_G = false;
    public boolean KEEP_CLIP = false;
    public boolean EXT_REV_WRAP = false;
    public boolean ALLOW_ALT_BUFFER = false;
    public boolean SWITCH_ALT_BUFFER = false;
    public boolean SAVE_CURSOR = false;
    public boolean SAVE_CLEAR_AND_SWITCH = false;
    public boolean SET_TERMINFO_FUNC_KEY_MODE = false;
    public boolean SET_SUN_KEY_MODE = false;
    public boolean SET_HP_K0EY_MODE = false;
    public boolean SET_SCO_KEY_MODE = false;
    public boolean SET_LEGACY_KEYBOARD = false;
    public boolean SET_VT220_KEYBOARD = false;
    public boolean ENABLE_READLINE_MOUSE_1 = false;
    public boolean ENABLE_READLINE_MOUSE_2 = false;
    public boolean ENABLE_READLINE_MOUSE_3 = false;
    public boolean SET_BRACKETED_PASTE = false;
    public boolean ENABLE_READLINE_CHAR_QUOTE = false;
    public boolean ENABLE_READLINE_NEWLINE_PASTE = false;
    public boolean APPLICATION_SYNC = false;
    public boolean APPLICATION_ESC_MODE = false;

    public int getModeForRequest(int mode) {
        Boolean modeState = getMode(mode);
        if (modeState == null) return 0;
        if (modeState) return 1;
        return 2;
    }

    @Nullable
    public Boolean getMode(int mode) {
        return switch (mode) {
            case 1 -> DECCKM;
            case 2 -> DECANM;
            case 3 -> DECCOLM;
            case 4 -> DECSCLM;
            case 5 -> DECSCNM;
            case 6 -> DECOM;
            case 7 -> DECAWM;
            case 8 -> DECARM;
            case 9 -> X10MM;
            case 10 -> TOOLBAR;
            case 12 -> START_BLINKING_CURSOR;
            case 13 -> START_BLINKING_CURSOR2;
            case 14 -> XORBLINK;
            case 18 -> DECPFF;
            case 19 -> DECPEX;
            case 25 -> DECTCEM;
            case 30 -> SHOW_SCROLL;
            case 35 -> FONT_SHIFT;
            case 38 -> TEKTRONIX;
            case 40 -> ENABLE_80_132;
            case 41 -> MORE_FIX;
            case 42 -> DECNRCM;
            case 43 -> DECGEPM;
            case 44 -> MARG_BELL;
            case 45 -> XTREVWRAP;
            case 46 -> XTLOGGING;
            case 47 -> ALT_BUFFER;
            case 66 -> DECNKM;
            case 67 -> DECBKM;
            case 69 -> DECLRMM;
            case 80 -> DECSDM;
            case 96 -> DECNCSM;
            case 1000 -> X11MM;
            case 1001 -> HILITE_MOUSE;
            case 1002 -> CELL_MOTION_MOUSE;
            case 1003 -> ALL_MOTION_MOUSE_TRACKING;
            case 1004 -> FOCUS_IN_FOCUS_OUT;
            case 1005 -> UTF8_MOUSE;
            case 1006 -> SGR_MOUSE;
            case 1007 -> ALTERNATE_SCROLL_MODE;
            case 1010 -> SCROLL_BOTTOM_ON_OUTPUT;
            case 1011 -> SCROLL_BOTTOM_ON_KEY_PRESS;
            case 1014 -> FAST_SCROLL;
            case 1015 -> URXVT_MOUSE;
            case 1016 -> SGR_MOUSE_PIXEL;
            case 1034 -> META_KEY;
            case 1035 -> SPECIAL_MODIFIERS;
            case 1036 -> META_SENDS_ESCAPE;
            case 1037 -> DEL_EDIT_KEYPAD_DEL;
            case 1039 -> ALT_SENDS_ESC;
            case 1040 -> KEEP_SELECTION;
            case 1041 -> USE_CLIP;
            case 1042 -> ENABLE_URGENCY;
            case 1043 -> RAISE_ON_CTRL_G;
            case 1044 -> KEEP_CLIP;
            case 1045 -> EXT_REV_WRAP;
            case 1046 -> ALLOW_ALT_BUFFER;
            case 1047 -> SWITCH_ALT_BUFFER;
            case 1048 -> SAVE_CURSOR;
            case 1049 -> SAVE_CLEAR_AND_SWITCH;
            case 1050 -> SET_TERMINFO_FUNC_KEY_MODE;
            case 1051 -> SET_SUN_KEY_MODE;
            case 1052 -> SET_HP_K0EY_MODE;
            case 1053 -> SET_SCO_KEY_MODE;
            case 1060 -> SET_LEGACY_KEYBOARD;
            case 1061 -> SET_VT220_KEYBOARD;
            case 2001 -> ENABLE_READLINE_MOUSE_1;
            case 2002 -> ENABLE_READLINE_MOUSE_2;
            case 2003 -> ENABLE_READLINE_MOUSE_3;
            case 2004 -> SET_BRACKETED_PASTE;
            case 2005 -> ENABLE_READLINE_CHAR_QUOTE;
            case 2006 -> ENABLE_READLINE_NEWLINE_PASTE;
            case 2026 -> APPLICATION_SYNC;
            case 7727 -> APPLICATION_ESC_MODE;
            default -> null;
        };
    }

    public MouseMode getMouseMode() {
        int mode;
        List<Integer> secondaryModes = new ArrayList<>();
        if (X10MM) mode = 9;
        else if (X11MM) mode = 1000;
        else if (CELL_MOTION_MOUSE) mode = 1002;
        else if (ALL_MOTION_MOUSE_TRACKING) mode = 1003;
        else mode = 0;
        if (UTF8_MOUSE) secondaryModes.add(1005);
        if (SGR_MOUSE) secondaryModes.add(1006);
        if (URXVT_MOUSE) secondaryModes.add(1015);
        if (SGR_MOUSE_PIXEL) secondaryModes.add(1016);

        return new MouseMode(mode, secondaryModes.stream().mapToInt(Integer::intValue).toArray());
    }

    public boolean isAltBufferEnabled() {
        return ALT_BUFFER || SWITCH_ALT_BUFFER || SAVE_CLEAR_AND_SWITCH;
    }
}
