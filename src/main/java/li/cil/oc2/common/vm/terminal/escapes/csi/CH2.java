package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;
import li.cil.oc2.common.vm.terminal.modes.ImplementedPrivateModes;

public class CH2 extends CSISequenceHandler { // Combined Handler 2 (SM & DECSET)
    public CH2(final Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argCount, CSIState state) {
        if (state.questionMark) {
            handleDECSET(args, argCount);
        } else {
            handleSM(args, argCount);
        }
    }

    private void handleDECSET(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case 1 -> terminal.currentPrivateModeState.DECCKM = true;
                case 2 -> terminal.currentPrivateModeState.DECANM = true;
                case 3 -> terminal.currentPrivateModeState.DECCOLM = true;
                case 4 -> terminal.currentPrivateModeState.DECSCLM = true;
                case 5 -> terminal.currentPrivateModeState.DECSCNM = true;
                case 6 -> {
                    terminal.currentPrivateModeState.DECOM = true;
                    terminal.setRelativeCursorPos(0, 0);
                }
                case 7 -> terminal.currentPrivateModeState.DECAWM = true;
                case 8 -> terminal.currentPrivateModeState.DECARM = true;
                case 9 ->  {
                    terminal.currentPrivateModeState.X11MM = false;
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                    terminal.currentPrivateModeState.X10MM = true;
                }
                case 10 -> terminal.currentPrivateModeState.TOOLBAR = true;
                case 12 -> {
                    terminal.cursorMode = switch (terminal.cursorMode) {
                        case 2 -> 1;
                        case 4 -> 3;
                        case 6 -> 5;
                        default -> terminal.cursorMode;
                    };
                    terminal.currentPrivateModeState.START_BLINKING_CURSOR = true;
                }
                case 13 -> terminal.currentPrivateModeState.START_BLINKING_CURSOR2 = true;
                case 14 -> terminal.currentPrivateModeState.XORBLINK = true;
                case 18 -> terminal.currentPrivateModeState.DECPFF = true;
                case 19 -> terminal.currentPrivateModeState.DECPEX = true;
                case 25 -> terminal.currentPrivateModeState.DECTCEM = true;
                case 30 -> terminal.currentPrivateModeState.SHOW_SCROLL = true;
                case 35 -> terminal.currentPrivateModeState.FONT_SHIFT = true;
                case 38 -> terminal.currentPrivateModeState.TEKTRONIX = true;
                case 40 -> terminal.currentPrivateModeState.ENABLE_80_132 = true;
                case 41 -> terminal.currentPrivateModeState.MORE_FIX = true;
                case 42 -> terminal.currentPrivateModeState.DECNRCM = true;
                case 43 -> terminal.currentPrivateModeState.DECGEPM = true;
                case 44 -> terminal.currentPrivateModeState.MARG_BELL = true;
                case 45 -> terminal.currentPrivateModeState.XTREVWRAP = true;
                case 46 -> terminal.currentPrivateModeState.XTLOGGING = true;
                case 47 -> {
                    terminal.clearAlt();
                    terminal.setCursorPos(0, 0);
                    terminal.currentPrivateModeState.ALT_BUFFER = true;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j <= 23; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int finalDirtyLinesMask = dirtyLinesMask;
                    terminal.renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(finalDirtyLinesMask, (left, right) -> left | right));
                }
                case 66 -> terminal.currentPrivateModeState.DECNKM = true;
                case 67 -> terminal.currentPrivateModeState.DECBKM = true;
                case 69 -> terminal.currentPrivateModeState.DECLRMM = true;
                case 80 -> terminal.currentPrivateModeState.DECSDM = true;
                case 96 -> terminal.currentPrivateModeState.DECNCSM = true;
                case 1000 -> {
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                    terminal.currentPrivateModeState.X10MM = false;
                    terminal.currentPrivateModeState.X11MM = true;
                }
                case 1001 -> terminal.currentPrivateModeState.HILITE_MOUSE = true;
                case 1002 -> {
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                    terminal.currentPrivateModeState.X10MM = false;
                    terminal.currentPrivateModeState.X11MM = false;
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = true;
                }
                case 1003 -> {
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                    terminal.currentPrivateModeState.X10MM = false;
                    terminal.currentPrivateModeState.X11MM = false;
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = true;
                }
                case 1004 -> terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT = true;
                case 1005 -> {
                    terminal.currentPrivateModeState.SGR_MOUSE = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                    terminal.currentPrivateModeState.UTF8_MOUSE = true;
                }
                case 1006 -> {
                    terminal.currentPrivateModeState.UTF8_MOUSE = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                    terminal.currentPrivateModeState.SGR_MOUSE = true;
                }
                case 1007 -> terminal.currentPrivateModeState.ALTERNATE_SCROLL_MODE = true;
                case 1010 -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_OUTPUT = true;
                case 1011 -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS = true;
                case 1014 -> terminal.currentPrivateModeState.FAST_SCROLL = true;
                case 1015 -> {
                    terminal.currentPrivateModeState.UTF8_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = true;
                }
                case 1016 -> {
                    terminal.currentPrivateModeState.UTF8_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = true;
                }
                case 1034 -> terminal.currentPrivateModeState.META_KEY = true;
                case 1035 -> terminal.currentPrivateModeState.SPECIAL_MODIFIERS = true;
                case 1036 -> terminal.currentPrivateModeState.META_SENDS_ESCAPE = true;
                case 1037 -> terminal.currentPrivateModeState.DEL_EDIT_KEYPAD_DEL = true;
                case 1039 -> terminal.currentPrivateModeState.ALT_SENDS_ESC = true;
                case 1040 -> terminal.currentPrivateModeState.KEEP_SELECTION = true;
                case 1041 -> terminal.currentPrivateModeState.USE_CLIP = true;
                case 1042 -> terminal.currentPrivateModeState.ENABLE_URGENCY = true;
                case 1043 -> terminal.currentPrivateModeState.RAISE_ON_CTRL_G = true;
                case 1044 -> terminal.currentPrivateModeState.KEEP_CLIP = true;
                case 1045 -> terminal.currentPrivateModeState.EXT_REV_WRAP = true;
                case 1046 -> terminal.currentPrivateModeState.ALLOW_ALT_BUFFER = true;
                case 1047 -> {
                    terminal.clearAlt();
                    terminal.setCursorPos(0, 0);
                    terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = true;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j <= 23; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int finalDirtyLinesMask = dirtyLinesMask;
                    terminal.renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(finalDirtyLinesMask, (left, right) -> left | right));
                }
                case 1048 -> {
                    if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                        terminal.altSavedX = terminal.x;
                        terminal.altSavedY = terminal.y;
                    } else {
                        terminal.savedX = terminal.x;
                        terminal.savedY = terminal.y;
                    }
                    terminal.currentPrivateModeState.SAVE_CURSOR = true;
                }
                case 1049 -> {
                    if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                        terminal.altSavedX = terminal.x;
                        terminal.altSavedY = terminal.y;
                    } else {
                        terminal.savedX = terminal.x;
                        terminal.savedY = terminal.y;
                    }
                    terminal.clearAlt();
                    terminal.setCursorPos(0, 0);
                    terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = true;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j <= 23; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int finalDirtyLinesMask = dirtyLinesMask;
                    terminal.renderers.forEach(model -> model.getDirtyMask().accumulateAndGet(finalDirtyLinesMask, (left, right) -> left | right));
                }
                case 1050 -> terminal.currentPrivateModeState.SET_TERMINFO_FUNC_KEY_MODE = true;
                case 1051 -> terminal.currentPrivateModeState.SET_SUN_KEY_MODE = true;
                case 1052 -> terminal.currentPrivateModeState.SET_HP_K0EY_MODE = true;
                case 1053 -> terminal.currentPrivateModeState.SET_SCO_KEY_MODE = true;
                case 1060 -> terminal.currentPrivateModeState.SET_LEGACY_KEYBOARD = true;
                case 1061 -> terminal.currentPrivateModeState.SET_VT220_KEYBOARD = true;
                case 2001 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_1 = true;
                case 2002 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_2 = true;
                case 2003 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_3 = true;
                case 2004 -> terminal.currentPrivateModeState.SET_BRACKETED_PASTE = true;
                case 2005 -> terminal.currentPrivateModeState.ENABLE_READLINE_CHAR_QUOTE = true;
                case 2006 -> terminal.currentPrivateModeState.ENABLE_READLINE_NEWLINE_PASTE = true;
                case 2026 -> terminal.currentPrivateModeState.APPLICATION_SYNC = true;
                case 7727 -> terminal.currentPrivateModeState.APPLICATION_ESC_MODE = true;
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], true);
        }
    }

    private void handleSM(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case 2 -> terminal.currentModeState.KAM = true;
                case 4 -> terminal.currentModeState.IRM = true;
                case 12 -> terminal.currentModeState.SRM = true;
                case 20 -> terminal.currentModeState.LNM = true;
            }
        }
    }
}
