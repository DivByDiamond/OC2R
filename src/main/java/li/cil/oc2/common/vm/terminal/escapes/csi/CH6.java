package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class CH6 extends CSISequenceHandler { // Combined Handler 6 (XTSAVE, XTSHIFTESCAPE, DECSLRM, and SCOSC)
    public CH6(final Terminal terminal) {
        super(terminal);
    }

    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTSAVE
            handleXTSAVE(args[0]);
        }
        else if (state.greaterThan) { // XTSHIFTESCAPE
            System.out.println("XTSHIFTESCAPE not implemented");
        }
        else if (argsCount == 2) { // DECSLRM
            System.out.println("DECSLRM not implemented");
        }
        else if (argsCount == 0) { // SCOSC
            if (!terminal.currentPrivateModeState.DECLRMM) {
                terminal.savedX = terminal.x;
                terminal.savedY = terminal.y;
            }
        }
    }

    private void handleXTSAVE(final int mode) {
        switch (mode) {
            case 1 -> terminal.savePrivateModeState.DECCKM = terminal.currentPrivateModeState.DECCKM;
            case 2 -> terminal.savePrivateModeState.DECANM = terminal.currentPrivateModeState.DECANM;
            case 3 -> terminal.savePrivateModeState.DECCOLM = terminal.currentPrivateModeState.DECCOLM;
            case 4 -> terminal.savePrivateModeState.DECSCLM = terminal.currentPrivateModeState.DECSCLM;
            case 5 -> terminal.savePrivateModeState.DECSCNM = terminal.currentPrivateModeState.DECSCNM;
            case 6 -> terminal.savePrivateModeState.DECOM = terminal.currentPrivateModeState.DECOM;
            case 7 -> terminal.savePrivateModeState.DECAWM = terminal.currentPrivateModeState.DECAWM;
            case 8 -> terminal.savePrivateModeState.DECARM = terminal.currentPrivateModeState.DECARM;
            case 9 -> terminal.savePrivateModeState.X10MM = terminal.currentPrivateModeState.X10MM;
            case 10 -> terminal.savePrivateModeState.TOOLBAR = terminal.currentPrivateModeState.TOOLBAR;
            case 12 -> terminal.savePrivateModeState.START_BLINKING_CURSOR = terminal.currentPrivateModeState.START_BLINKING_CURSOR;
            case 13 -> terminal.savePrivateModeState.START_BLINKING_CURSOR2 = terminal.currentPrivateModeState.START_BLINKING_CURSOR2;
            case 14 -> terminal.savePrivateModeState.XORBLINK = terminal.currentPrivateModeState.XORBLINK;
            case 18 -> terminal.savePrivateModeState.DECPFF = terminal.currentPrivateModeState.DECPFF;
            case 19 -> terminal.savePrivateModeState.DECPEX = terminal.currentPrivateModeState.DECPEX;
            case 25 -> terminal.savePrivateModeState.DECTCEM = terminal.currentPrivateModeState.DECTCEM;
            case 30 -> terminal.savePrivateModeState.SHOW_SCROLL = terminal.currentPrivateModeState.SHOW_SCROLL;
            case 35 -> terminal.savePrivateModeState.FONT_SHIFT = terminal.currentPrivateModeState.FONT_SHIFT;
            case 38 -> terminal.savePrivateModeState.TEKTRONIX = terminal.currentPrivateModeState.TEKTRONIX;
            case 40 -> terminal.savePrivateModeState.ENABLE_80_132 = terminal.currentPrivateModeState.ENABLE_80_132;
            case 41 -> terminal.savePrivateModeState.MORE_FIX = terminal.currentPrivateModeState.MORE_FIX;
            case 42 -> terminal.savePrivateModeState.DECNRCM = terminal.currentPrivateModeState.DECNRCM;
            case 43 -> terminal.savePrivateModeState.DECGEPM = terminal.currentPrivateModeState.DECGEPM;
            case 44 -> terminal.savePrivateModeState.MARG_BELL = terminal.currentPrivateModeState.MARG_BELL;
            case 45 -> terminal.savePrivateModeState.XTREVWRAP = terminal.currentPrivateModeState.XTREVWRAP;
            case 46 -> terminal.savePrivateModeState.XTLOGGING = terminal.currentPrivateModeState.XTLOGGING;
            case 47 -> terminal.savePrivateModeState.ALT_BUFFER = terminal.currentPrivateModeState.ALT_BUFFER;
            case 66 -> terminal.savePrivateModeState.DECNKM = terminal.currentPrivateModeState.DECNKM;
            case 67 -> terminal.savePrivateModeState.DECBKM = terminal.currentPrivateModeState.DECBKM;
            case 69 -> terminal.savePrivateModeState.DECLRMM = terminal.currentPrivateModeState.DECLRMM;
            case 80 -> terminal.savePrivateModeState.DECSDM = terminal.currentPrivateModeState.DECSDM;
            case 96 -> terminal.savePrivateModeState.DECNCSM = terminal.currentPrivateModeState.DECNCSM;
            case 1000 -> terminal.savePrivateModeState.X11MM = terminal.currentPrivateModeState.X11MM;
            case 1001 -> terminal.savePrivateModeState.HILITE_MOUSE = terminal.currentPrivateModeState.HILITE_MOUSE;
            case 1002 -> terminal.savePrivateModeState.CELL_MOTION_MOUSE = terminal.currentPrivateModeState.CELL_MOTION_MOUSE;
            case 1003 -> terminal.savePrivateModeState.ALL_MOTION_MOUSE_TRACKING = terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING;
            case 1004 -> terminal.savePrivateModeState.FOCUS_IN_FOCUS_OUT = terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT;
            case 1005 -> terminal.savePrivateModeState.UTF8_MOUSE = terminal.currentPrivateModeState.UTF8_MOUSE;
            case 1006 -> terminal.savePrivateModeState.SGR_MOUSE = terminal.currentPrivateModeState.SGR_MOUSE;
            case 1007 -> terminal.savePrivateModeState.ALTERNATE_SCROLL_MODE = terminal.currentPrivateModeState.ALTERNATE_SCROLL_MODE;
            case 1010 -> terminal.savePrivateModeState.SCROLL_BOTTOM_ON_OUTPUT = terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_OUTPUT;
            case 1011 -> terminal.savePrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS = terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS;
            case 1014 -> terminal.savePrivateModeState.FAST_SCROLL = terminal.currentPrivateModeState.FAST_SCROLL;
            case 1015 -> terminal.savePrivateModeState.URXVT_MOUSE = terminal.currentPrivateModeState.URXVT_MOUSE;
            case 1016 -> terminal.savePrivateModeState.SGR_MOUSE_PIXEL = terminal.currentPrivateModeState.SGR_MOUSE_PIXEL;
            case 1034 -> terminal.savePrivateModeState.META_KEY = terminal.currentPrivateModeState.META_KEY;
            case 1035 -> terminal.savePrivateModeState.SPECIAL_MODIFIERS = terminal.currentPrivateModeState.SPECIAL_MODIFIERS;
            case 1036 -> terminal.savePrivateModeState.META_SENDS_ESCAPE = terminal.currentPrivateModeState.META_SENDS_ESCAPE;
            case 1037 -> terminal.savePrivateModeState.DEL_EDIT_KEYPAD_DEL = terminal.currentPrivateModeState.DEL_EDIT_KEYPAD_DEL;
            case 1039 -> terminal.savePrivateModeState.ALT_SENDS_ESC = terminal.currentPrivateModeState.ALT_SENDS_ESC;
            case 1040 -> terminal.savePrivateModeState.KEEP_SELECTION = terminal.currentPrivateModeState.KEEP_SELECTION;
            case 1041 -> terminal.savePrivateModeState.USE_CLIP = terminal.currentPrivateModeState.USE_CLIP;
            case 1042 -> terminal.savePrivateModeState.ENABLE_URGENCY = terminal.currentPrivateModeState.ENABLE_URGENCY;
            case 1043 -> terminal.savePrivateModeState.RAISE_ON_CTRL_G = terminal.currentPrivateModeState.RAISE_ON_CTRL_G;
            case 1044 -> terminal.savePrivateModeState.KEEP_CLIP = terminal.currentPrivateModeState.KEEP_CLIP;
            case 1045 -> terminal.savePrivateModeState.EXT_REV_WRAP = terminal.currentPrivateModeState.EXT_REV_WRAP;
            case 1046 -> terminal.savePrivateModeState.ALLOW_ALT_BUFFER = terminal.currentPrivateModeState.ALLOW_ALT_BUFFER;
            case 1047 -> terminal.savePrivateModeState.SWITCH_ALT_BUFFER = terminal.currentPrivateModeState.SWITCH_ALT_BUFFER;
            case 1048 -> terminal.savePrivateModeState.SAVE_CURSOR = terminal.currentPrivateModeState.SAVE_CURSOR;
            case 1049 -> terminal.savePrivateModeState.SAVE_CLEAR_AND_SWITCH = terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH;
            case 1050 -> terminal.savePrivateModeState.SET_TERMINFO_FUNC_KEY_MODE = terminal.currentPrivateModeState.SET_TERMINFO_FUNC_KEY_MODE;
            case 1051 -> terminal.savePrivateModeState.SET_SUN_KEY_MODE = terminal.currentPrivateModeState.SET_SUN_KEY_MODE;
            case 1052 -> terminal.savePrivateModeState.SET_HP_K0EY_MODE = terminal.currentPrivateModeState.SET_HP_K0EY_MODE;
            case 1053 -> terminal.savePrivateModeState.SET_SCO_KEY_MODE = terminal.currentPrivateModeState.SET_SCO_KEY_MODE;
            case 1060 -> terminal.savePrivateModeState.SET_LEGACY_KEYBOARD = terminal.currentPrivateModeState.SET_LEGACY_KEYBOARD;
            case 1061 -> terminal.savePrivateModeState.SET_VT220_KEYBOARD = terminal.currentPrivateModeState.SET_VT220_KEYBOARD;
            case 2001 -> terminal.savePrivateModeState.ENABLE_READLINE_MOUSE_1 = terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_1;
            case 2002 -> terminal.savePrivateModeState.ENABLE_READLINE_MOUSE_2 = terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_2;
            case 2003 -> terminal.savePrivateModeState.ENABLE_READLINE_MOUSE_3 = terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_3;
            case 2004 -> terminal.savePrivateModeState.SET_BRACKETED_PASTE = terminal.currentPrivateModeState.SET_BRACKETED_PASTE;
            case 2005 -> terminal.savePrivateModeState.ENABLE_READLINE_CHAR_QUOTE = terminal.currentPrivateModeState.ENABLE_READLINE_CHAR_QUOTE;
            case 2006 -> terminal.savePrivateModeState.ENABLE_READLINE_NEWLINE_PASTE = terminal.currentPrivateModeState.ENABLE_READLINE_NEWLINE_PASTE;
            case 2026 -> terminal.savePrivateModeState.APPLICATION_SYNC = terminal.currentPrivateModeState.APPLICATION_SYNC;
            case 7727 -> terminal.savePrivateModeState.APPLICATION_ESC_MODE = terminal.currentPrivateModeState.APPLICATION_ESC_MODE;
        }
    }
}
