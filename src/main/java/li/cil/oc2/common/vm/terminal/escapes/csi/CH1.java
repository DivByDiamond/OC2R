package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH1 extends CSISequenceHandler { // Combined Handler 1 (DECSTBM & XTRESTORE)
    public CH1(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.questionMark) { // XTRESTORE
            handleXTRESTORE(args[0]);
        } else if (state.dollarSign) { // DECCARA
            System.out.println("DECCARA is not implemented");
        } else { // DECSTBM with 0 or 1 args = reset to full screen
            handleDECSTBM(args, argCount);
        }
    }

    private void handleXTRESTORE(int mode) {
        switch (mode) {
            case 1 ->
                    terminal.currentPrivateModeState.DECCKM = terminal.savePrivateModeState.DECCKM;
            case 2 ->
                    terminal.currentPrivateModeState.DECANM = terminal.savePrivateModeState.DECANM;
            case 3 ->
                    terminal.currentPrivateModeState.DECCOLM =
                            terminal.savePrivateModeState.DECCOLM;
            case 4 ->
                    terminal.currentPrivateModeState.DECSCLM =
                            terminal.savePrivateModeState.DECSCLM;
            case 5 ->
                    terminal.currentPrivateModeState.DECSCNM =
                            terminal.savePrivateModeState.DECSCNM;
            case 6 -> terminal.currentPrivateModeState.DECOM = terminal.savePrivateModeState.DECOM;
            case 7 ->
                    terminal.currentPrivateModeState.DECAWM = terminal.savePrivateModeState.DECAWM;
            case 8 ->
                    terminal.currentPrivateModeState.DECARM = terminal.savePrivateModeState.DECARM;
            case 9 -> terminal.currentPrivateModeState.X10MM = terminal.savePrivateModeState.X10MM;
            case 10 ->
                    terminal.currentPrivateModeState.TOOLBAR =
                            terminal.savePrivateModeState.TOOLBAR;
            case 12 ->
                    terminal.currentPrivateModeState.START_BLINKING_CURSOR =
                            terminal.savePrivateModeState.START_BLINKING_CURSOR;
            case 13 ->
                    terminal.currentPrivateModeState.START_BLINKING_CURSOR2 =
                            terminal.savePrivateModeState.START_BLINKING_CURSOR2;
            case 14 ->
                    terminal.currentPrivateModeState.XORBLINK =
                            terminal.savePrivateModeState.XORBLINK;
            case 18 ->
                    terminal.currentPrivateModeState.DECPFF = terminal.savePrivateModeState.DECPFF;
            case 19 ->
                    terminal.currentPrivateModeState.DECPEX = terminal.savePrivateModeState.DECPEX;
            case 25 ->
                    terminal.currentPrivateModeState.DECTCEM =
                            terminal.savePrivateModeState.DECTCEM;
            case 30 ->
                    terminal.currentPrivateModeState.SHOW_SCROLL =
                            terminal.savePrivateModeState.SHOW_SCROLL;
            case 35 ->
                    terminal.currentPrivateModeState.FONT_SHIFT =
                            terminal.savePrivateModeState.FONT_SHIFT;
            case 38 ->
                    terminal.currentPrivateModeState.TEKTRONIX =
                            terminal.savePrivateModeState.TEKTRONIX;
            case 40 ->
                    terminal.currentPrivateModeState.ENABLE_80_132 =
                            terminal.savePrivateModeState.ENABLE_80_132;
            case 41 ->
                    terminal.currentPrivateModeState.MORE_FIX =
                            terminal.savePrivateModeState.MORE_FIX;
            case 42 ->
                    terminal.currentPrivateModeState.DECNRCM =
                            terminal.savePrivateModeState.DECNRCM;
            case 43 ->
                    terminal.currentPrivateModeState.DECGEPM =
                            terminal.savePrivateModeState.DECGEPM;
            case 44 ->
                    terminal.currentPrivateModeState.MARG_BELL =
                            terminal.savePrivateModeState.MARG_BELL;
            case 45 ->
                    terminal.currentPrivateModeState.XTREVWRAP =
                            terminal.savePrivateModeState.XTREVWRAP;
            case 46 ->
                    terminal.currentPrivateModeState.XTLOGGING =
                            terminal.savePrivateModeState.XTLOGGING;
            case 47 ->
                    terminal.currentPrivateModeState.ALT_BUFFER =
                            terminal.savePrivateModeState.ALT_BUFFER;
            case 66 ->
                    terminal.currentPrivateModeState.DECNKM = terminal.savePrivateModeState.DECNKM;
            case 67 ->
                    terminal.currentPrivateModeState.DECBKM = terminal.savePrivateModeState.DECBKM;
            case 69 ->
                    terminal.currentPrivateModeState.DECLRMM =
                            terminal.savePrivateModeState.DECLRMM;
            case 80 ->
                    terminal.currentPrivateModeState.DECSDM = terminal.savePrivateModeState.DECSDM;
            case 96 ->
                    terminal.currentPrivateModeState.DECNCSM =
                            terminal.savePrivateModeState.DECNCSM;
            case 1000 ->
                    terminal.currentPrivateModeState.X11MM = terminal.savePrivateModeState.X11MM;
            case 1001 ->
                    terminal.currentPrivateModeState.HILITE_MOUSE =
                            terminal.savePrivateModeState.HILITE_MOUSE;
            case 1002 ->
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE =
                            terminal.savePrivateModeState.CELL_MOTION_MOUSE;
            case 1003 ->
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING =
                            terminal.savePrivateModeState.ALL_MOTION_MOUSE_TRACKING;
            case 1004 ->
                    terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT =
                            terminal.savePrivateModeState.FOCUS_IN_FOCUS_OUT;
            case 1005 ->
                    terminal.currentPrivateModeState.UTF8_MOUSE =
                            terminal.savePrivateModeState.UTF8_MOUSE;
            case 1006 ->
                    terminal.currentPrivateModeState.SGR_MOUSE =
                            terminal.savePrivateModeState.SGR_MOUSE;
            case 1007 ->
                    terminal.currentPrivateModeState.ALTERNATE_SCROLL_MODE =
                            terminal.savePrivateModeState.ALTERNATE_SCROLL_MODE;
            case 1010 ->
                    terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_OUTPUT =
                            terminal.savePrivateModeState.SCROLL_BOTTOM_ON_OUTPUT;
            case 1011 ->
                    terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS =
                            terminal.savePrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS;
            case 1014 ->
                    terminal.currentPrivateModeState.FAST_SCROLL =
                            terminal.savePrivateModeState.FAST_SCROLL;
            case 1015 ->
                    terminal.currentPrivateModeState.URXVT_MOUSE =
                            terminal.savePrivateModeState.URXVT_MOUSE;
            case 1016 ->
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL =
                            terminal.savePrivateModeState.SGR_MOUSE_PIXEL;
            case 1034 ->
                    terminal.currentPrivateModeState.META_KEY =
                            terminal.savePrivateModeState.META_KEY;
            case 1035 ->
                    terminal.currentPrivateModeState.SPECIAL_MODIFIERS =
                            terminal.savePrivateModeState.SPECIAL_MODIFIERS;
            case 1036 ->
                    terminal.currentPrivateModeState.META_SENDS_ESCAPE =
                            terminal.savePrivateModeState.META_SENDS_ESCAPE;
            case 1037 ->
                    terminal.currentPrivateModeState.DEL_EDIT_KEYPAD_DEL =
                            terminal.savePrivateModeState.DEL_EDIT_KEYPAD_DEL;
            case 1039 ->
                    terminal.currentPrivateModeState.ALT_SENDS_ESC =
                            terminal.savePrivateModeState.ALT_SENDS_ESC;
            case 1040 ->
                    terminal.currentPrivateModeState.KEEP_SELECTION =
                            terminal.savePrivateModeState.KEEP_SELECTION;
            case 1041 ->
                    terminal.currentPrivateModeState.USE_CLIP =
                            terminal.savePrivateModeState.USE_CLIP;
            case 1042 ->
                    terminal.currentPrivateModeState.ENABLE_URGENCY =
                            terminal.savePrivateModeState.ENABLE_URGENCY;
            case 1043 ->
                    terminal.currentPrivateModeState.RAISE_ON_CTRL_G =
                            terminal.savePrivateModeState.RAISE_ON_CTRL_G;
            case 1044 ->
                    terminal.currentPrivateModeState.KEEP_CLIP =
                            terminal.savePrivateModeState.KEEP_CLIP;
            case 1045 ->
                    terminal.currentPrivateModeState.EXT_REV_WRAP =
                            terminal.savePrivateModeState.EXT_REV_WRAP;
            case 1046 ->
                    terminal.currentPrivateModeState.ALLOW_ALT_BUFFER =
                            terminal.savePrivateModeState.ALLOW_ALT_BUFFER;
            case 1047 ->
                    terminal.currentPrivateModeState.SWITCH_ALT_BUFFER =
                            terminal.savePrivateModeState.SWITCH_ALT_BUFFER;
            case 1048 ->
                    terminal.currentPrivateModeState.SAVE_CURSOR =
                            terminal.savePrivateModeState.SAVE_CURSOR;
            case 1049 ->
                    terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH =
                            terminal.savePrivateModeState.SAVE_CLEAR_AND_SWITCH;
            case 1050 ->
                    terminal.currentPrivateModeState.SET_TERMINFO_FUNC_KEY_MODE =
                            terminal.savePrivateModeState.SET_TERMINFO_FUNC_KEY_MODE;
            case 1051 ->
                    terminal.currentPrivateModeState.SET_SUN_KEY_MODE =
                            terminal.savePrivateModeState.SET_SUN_KEY_MODE;
            case 1052 ->
                    terminal.currentPrivateModeState.SET_HP_K0EY_MODE =
                            terminal.savePrivateModeState.SET_HP_K0EY_MODE;
            case 1053 ->
                    terminal.currentPrivateModeState.SET_SCO_KEY_MODE =
                            terminal.savePrivateModeState.SET_SCO_KEY_MODE;
            case 1060 ->
                    terminal.currentPrivateModeState.SET_LEGACY_KEYBOARD =
                            terminal.savePrivateModeState.SET_LEGACY_KEYBOARD;
            case 1061 ->
                    terminal.currentPrivateModeState.SET_VT220_KEYBOARD =
                            terminal.savePrivateModeState.SET_VT220_KEYBOARD;
            case 2001 ->
                    terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_1 =
                            terminal.savePrivateModeState.ENABLE_READLINE_MOUSE_1;
            case 2002 ->
                    terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_2 =
                            terminal.savePrivateModeState.ENABLE_READLINE_MOUSE_2;
            case 2003 ->
                    terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_3 =
                            terminal.savePrivateModeState.ENABLE_READLINE_MOUSE_3;
            case 2004 ->
                    terminal.currentPrivateModeState.SET_BRACKETED_PASTE =
                            terminal.savePrivateModeState.SET_BRACKETED_PASTE;
            case 2005 ->
                    terminal.currentPrivateModeState.ENABLE_READLINE_CHAR_QUOTE =
                            terminal.savePrivateModeState.ENABLE_READLINE_CHAR_QUOTE;
            case 2006 ->
                    terminal.currentPrivateModeState.ENABLE_READLINE_NEWLINE_PASTE =
                            terminal.savePrivateModeState.ENABLE_READLINE_NEWLINE_PASTE;
            case 2026 ->
                    terminal.currentPrivateModeState.APPLICATION_SYNC =
                            terminal.savePrivateModeState.APPLICATION_SYNC;
            case 7727 ->
                    terminal.currentPrivateModeState.APPLICATION_ESC_MODE =
                            terminal.savePrivateModeState.APPLICATION_ESC_MODE;
            default -> {}
        }
    }

    private void handleDECSTBM(int[] args, int argCount) {
        final int first;
        final int last;
        // Each parameter defaults independently: top=1, bottom=HEIGHT
        int top = (argCount > 0 && args[0] > 0) ? args[0] : 1;
        int bottom = (argCount > 1 && args[1] > 0) ? args[1] : Terminal.HEIGHT;
        first = top - 1;
        last = bottom - 1;
        if (last - first <= 0) {
            return;
        }
        terminal.scrollFirst = first; // to index
        terminal.scrollLast = last; // to index
        terminal.setRelativeCursorPos(0, 0); // send cursor home
    }
}
