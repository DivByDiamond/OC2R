package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ImplementedPrivateModes;

public class CH3 extends CSISequenceHandler { // Combined Handler 3 (RM & DECRST)
    public CH3(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.questionMark) {
            handleDECRST(args, argCount);
        } else if (argCount == 2) {
            handleRM(args, argCount);
        }
    }

    private void handleDECRST(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case 1 -> terminal.currentPrivateModeState.DECCKM = false;
                case 2 -> terminal.currentPrivateModeState.DECANM = false;
                case 3 -> terminal.currentPrivateModeState.DECCOLM = false;
                case 4 -> terminal.currentPrivateModeState.DECSCLM = false;
                case 5 -> terminal.currentPrivateModeState.DECSCNM = false;
                case 6 -> {
                    terminal.currentPrivateModeState.DECOM = false;
                    terminal.setRelativeCursorPos(0, 0);
                    terminal.clear();
                }
                case 7 -> terminal.currentPrivateModeState.DECAWM = false;
                case 8 -> terminal.currentPrivateModeState.DECARM = false;
                case 9 -> terminal.currentPrivateModeState.X10MM = false;
                case 10 -> terminal.currentPrivateModeState.TOOLBAR = false;
                case 12 -> terminal.currentPrivateModeState.START_BLINKING_CURSOR = false;
                case 13 -> terminal.currentPrivateModeState.START_BLINKING_CURSOR2 = false;
                case 14 -> terminal.currentPrivateModeState.XORBLINK = false;
                case 18 -> terminal.currentPrivateModeState.DECPFF = false;
                case 19 -> terminal.currentPrivateModeState.DECPEX = false;
                case 25 -> terminal.currentPrivateModeState.DECTCEM = false;
                case 30 -> terminal.currentPrivateModeState.SHOW_SCROLL = false;
                case 35 -> terminal.currentPrivateModeState.FONT_SHIFT = false;
                case 38 -> terminal.currentPrivateModeState.TEKTRONIX = false;
                case 40 -> terminal.currentPrivateModeState.ENABLE_80_132 = false;
                case 41 -> terminal.currentPrivateModeState.MORE_FIX = false;
                case 42 -> terminal.currentPrivateModeState.DECNRCM = false;
                case 43 -> terminal.currentPrivateModeState.DECGEPM = false;
                case 44 -> terminal.currentPrivateModeState.MARG_BELL = false;
                case 45 -> terminal.currentPrivateModeState.XTREVWRAP = false;
                case 46 -> terminal.currentPrivateModeState.XTLOGGING = false;
                case 47 -> {
                    terminal.currentPrivateModeState.ALT_BUFFER = false;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j <= 23; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int finalDirtyLinesMask = dirtyLinesMask;
                    terminal.renderers.forEach(
                            model ->
                                    model.getDirtyMask()
                                            .accumulateAndGet(
                                                    finalDirtyLinesMask,
                                                    (left, right) -> left | right));
                }
                case 66 -> terminal.currentPrivateModeState.DECNKM = false;
                case 67 -> terminal.currentPrivateModeState.DECBKM = false;
                case 69 -> terminal.currentPrivateModeState.DECLRMM = false;
                case 80 -> terminal.currentPrivateModeState.DECSDM = false;
                case 96 -> terminal.currentPrivateModeState.DECNCSM = false;
                case 1000 -> terminal.currentPrivateModeState.X11MM = false;
                case 1001 -> terminal.currentPrivateModeState.HILITE_MOUSE = false;
                case 1002 -> terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                case 1003 -> terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                case 1004 -> terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT = false;
                case 1005 -> terminal.currentPrivateModeState.UTF8_MOUSE = false;
                case 1006 -> terminal.currentPrivateModeState.SGR_MOUSE = false;
                case 1007 -> terminal.currentPrivateModeState.ALTERNATE_SCROLL_MODE = false;
                case 1010 -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_OUTPUT = false;
                case 1011 -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS = false;
                case 1014 -> terminal.currentPrivateModeState.FAST_SCROLL = false;
                case 1015 -> terminal.currentPrivateModeState.URXVT_MOUSE = false;
                case 1016 -> terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                case 1034 -> terminal.currentPrivateModeState.META_KEY = false;
                case 1035 -> terminal.currentPrivateModeState.SPECIAL_MODIFIERS = false;
                case 1036 -> terminal.currentPrivateModeState.META_SENDS_ESCAPE = false;
                case 1037 -> terminal.currentPrivateModeState.DEL_EDIT_KEYPAD_DEL = false;
                case 1039 -> terminal.currentPrivateModeState.ALT_SENDS_ESC = false;
                case 1040 -> terminal.currentPrivateModeState.KEEP_SELECTION = false;
                case 1041 -> terminal.currentPrivateModeState.USE_CLIP = false;
                case 1042 -> terminal.currentPrivateModeState.ENABLE_URGENCY = false;
                case 1043 -> terminal.currentPrivateModeState.RAISE_ON_CTRL_G = false;
                case 1044 -> terminal.currentPrivateModeState.KEEP_CLIP = false;
                case 1045 -> terminal.currentPrivateModeState.EXT_REV_WRAP = false;
                case 1046 -> terminal.currentPrivateModeState.ALLOW_ALT_BUFFER = false;
                case 1047 -> {
                    terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = false;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j <= 23; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int finalDirtyLinesMask = dirtyLinesMask;
                    terminal.renderers.forEach(
                            model ->
                                    model.getDirtyMask()
                                            .accumulateAndGet(
                                                    finalDirtyLinesMask,
                                                    (left, right) -> left | right));
                }
                case 1048 -> {
                    terminal.currentPrivateModeState.SAVE_CURSOR = false;
                    if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                        terminal.x = terminal.altSavedX;
                        terminal.y = terminal.altSavedY;
                    } else {
                        terminal.x = terminal.savedX;
                        terminal.y = terminal.savedY;
                    }
                }
                case 1049 -> {
                    terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = false;
                    if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                        terminal.x = terminal.altSavedX;
                        terminal.y = terminal.altSavedY;
                    } else {
                        terminal.x = terminal.savedX;
                        terminal.y = terminal.savedY;
                    }
                    int dirtyLinesMask = 0;
                    for (int j = 0; j <= 23; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int finalDirtyLinesMask = dirtyLinesMask;
                    terminal.renderers.forEach(
                            model ->
                                    model.getDirtyMask()
                                            .accumulateAndGet(
                                                    finalDirtyLinesMask,
                                                    (left, right) -> left | right));
                }
                case 1050 -> terminal.currentPrivateModeState.SET_TERMINFO_FUNC_KEY_MODE = false;
                case 1051 -> terminal.currentPrivateModeState.SET_SUN_KEY_MODE = false;
                case 1052 -> terminal.currentPrivateModeState.SET_HP_K0EY_MODE = false;
                case 1053 -> terminal.currentPrivateModeState.SET_SCO_KEY_MODE = false;
                case 1060 -> terminal.currentPrivateModeState.SET_LEGACY_KEYBOARD = false;
                case 1061 -> terminal.currentPrivateModeState.SET_VT220_KEYBOARD = false;
                case 2001 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_1 = false;
                case 2002 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_2 = false;
                case 2003 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_3 = false;
                case 2004 -> terminal.currentPrivateModeState.SET_BRACKETED_PASTE = false;
                case 2005 -> terminal.currentPrivateModeState.ENABLE_READLINE_CHAR_QUOTE = false;
                case 2006 -> terminal.currentPrivateModeState.ENABLE_READLINE_NEWLINE_PASTE = false;
                case 2026 -> terminal.currentPrivateModeState.APPLICATION_SYNC = false;
                case 7727 -> terminal.currentPrivateModeState.APPLICATION_ESC_MODE = false;
                default -> {}
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], false);
        }
    }

    private void handleRM(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case 2 -> terminal.currentModeState.KAM = false;
                case 4 -> terminal.currentModeState.IRM = false;
                case 12 -> terminal.currentModeState.SRM = false;
                case 20 -> terminal.currentModeState.LNM = false;
                default -> {}
            }
        }
    }
}