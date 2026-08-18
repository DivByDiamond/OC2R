package li.cil.oc2.common.vm.terminal.modes;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.vm.terminal.modes.impl.MouseMode;

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
        final ModeTable table = ModeTable.forPrivateMode(mode);
        if (table == null) {
            return null;
        }
        return table.get(this);
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