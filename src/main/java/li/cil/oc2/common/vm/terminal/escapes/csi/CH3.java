package li.cil.oc2.common.vm.terminal.escapes.csi;

import static li.cil.oc2.common.vm.terminal.modes.Mode.*;
import static li.cil.oc2.common.vm.terminal.modes.PrivateMode.*;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.impl.ImplementedPrivateModes;

public class CH3 extends CSISequenceHandler { // Combined Handler 3 (RM & DECRST)
    public CH3(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.questionMark) {
            handleDECRST(args, argCount);
        } else {
            handleRM(args, argCount);
        }
    }

    private void handleDECRST(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case DECCKM -> terminal.currentPrivateModeState.DECCKM = false;
                case DECANM -> terminal.currentPrivateModeState.DECANM = false;
                case DECCOLM -> {
                    terminal.currentPrivateModeState.DECCOLM = false;
                    /* DECCOLM spec: clear screen and reset margins */
                    terminal.bufferManager.clear();
                    terminal.scrollFirst = 0;
                    terminal.scrollLast = Terminal.HEIGHT - 1;
                    terminal.setRelativeCursorPos(0, 0);
                    int dirtyLinesMask = 0;
                    for (int j = 0; j < Terminal.HEIGHT; j++) {
                        dirtyLinesMask |= 1 << j;
                    }
                    final int mask = dirtyLinesMask;
                    terminal.renderers.forEach(
                            m -> m.getDirtyMask().accumulateAndGet(mask, (l, r) -> l | r));
                }
                case DECSCLM -> terminal.currentPrivateModeState.DECSCLM = false;
                case DECSCNM -> terminal.currentPrivateModeState.DECSCNM = false;
                case DECOM -> {
                    terminal.currentPrivateModeState.DECOM = false;
                    terminal.setRelativeCursorPos(0, 0);
                }
                case DECAWM -> terminal.currentPrivateModeState.DECAWM = false;
                case DECARM -> terminal.currentPrivateModeState.DECARM = false;
                case X10MM -> terminal.currentPrivateModeState.X10MM = false;
                case TOOLBAR -> terminal.currentPrivateModeState.TOOLBAR = false;
                case START_BLINKING_CURSOR -> terminal.currentPrivateModeState.START_BLINKING_CURSOR = false;
                case START_BLINKING_CURSOR2 -> terminal.currentPrivateModeState.START_BLINKING_CURSOR2 = false;
                case XORBLINK -> terminal.currentPrivateModeState.XORBLINK = false;
                case DECPFF -> terminal.currentPrivateModeState.DECPFF = false;
                case DECPEX -> terminal.currentPrivateModeState.DECPEX = false;
                case DECTCEM -> terminal.currentPrivateModeState.DECTCEM = false;
                case SHOW_SCROLL -> terminal.currentPrivateModeState.SHOW_SCROLL = false;
                case FONT_SHIFT -> terminal.currentPrivateModeState.FONT_SHIFT = false;
                case TEKTRONIX -> terminal.currentPrivateModeState.TEKTRONIX = false;
                case ENABLE_80_132 -> terminal.currentPrivateModeState.ENABLE_80_132 = false;
                case MORE_FIX -> terminal.currentPrivateModeState.MORE_FIX = false;
                case DECNRCM -> terminal.currentPrivateModeState.DECNRCM = false;
                case DECGEPM -> terminal.currentPrivateModeState.DECGEPM = false;
                case MARG_BELL -> terminal.currentPrivateModeState.MARG_BELL = false;
                case XTREVWRAP -> terminal.currentPrivateModeState.XTREVWRAP = false;
                case XTLOGGING -> terminal.currentPrivateModeState.XTLOGGING = false;
                case ALT_BUFFER -> {
                    terminal.currentPrivateModeState.ALT_BUFFER = false;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j < Terminal.HEIGHT; j++) {
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
                case DECNKM -> terminal.currentPrivateModeState.DECNKM = false;
                case DECBKM -> terminal.currentPrivateModeState.DECBKM = false;
                case DECLRMM -> terminal.currentPrivateModeState.DECLRMM = false;
                case DECSDM -> terminal.currentPrivateModeState.DECSDM = false;
                case DECNCSM -> terminal.currentPrivateModeState.DECNCSM = false;
                case X11MM -> terminal.currentPrivateModeState.X11MM = false;
                case HILITE_MOUSE -> terminal.currentPrivateModeState.HILITE_MOUSE = false;
                case CELL_MOTION_MOUSE -> terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                case ALL_MOTION_MOUSE_TRACKING -> terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                case FOCUS_IN_FOCUS_OUT -> terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT = false;
                case UTF8_MOUSE -> terminal.currentPrivateModeState.UTF8_MOUSE = false;
                case SGR_MOUSE -> terminal.currentPrivateModeState.SGR_MOUSE = false;
                case ALTERNATE_SCROLL_MODE -> terminal.currentPrivateModeState.ALTERNATE_SCROLL_MODE = false;
                case SCROLL_BOTTOM_ON_OUTPUT -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_OUTPUT = false;
                case SCROLL_BOTTOM_ON_KEY_PRESS -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS = false;
                case FAST_SCROLL -> terminal.currentPrivateModeState.FAST_SCROLL = false;
                case URXVT_MOUSE -> terminal.currentPrivateModeState.URXVT_MOUSE = false;
                case SGR_MOUSE_PIXEL -> terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                case META_KEY -> terminal.currentPrivateModeState.META_KEY = false;
                case SPECIAL_MODIFIERS -> terminal.currentPrivateModeState.SPECIAL_MODIFIERS = false;
                case META_SENDS_ESCAPE -> terminal.currentPrivateModeState.META_SENDS_ESCAPE = false;
                case DEL_EDIT_KEYPAD_DEL -> terminal.currentPrivateModeState.DEL_EDIT_KEYPAD_DEL = false;
                case ALT_SENDS_ESC -> terminal.currentPrivateModeState.ALT_SENDS_ESC = false;
                case KEEP_SELECTION -> terminal.currentPrivateModeState.KEEP_SELECTION = false;
                case USE_CLIP -> terminal.currentPrivateModeState.USE_CLIP = false;
                case ENABLE_URGENCY -> terminal.currentPrivateModeState.ENABLE_URGENCY = false;
                case RAISE_ON_CTRL_G -> terminal.currentPrivateModeState.RAISE_ON_CTRL_G = false;
                case KEEP_CLIP -> terminal.currentPrivateModeState.KEEP_CLIP = false;
                case EXT_REV_WRAP -> terminal.currentPrivateModeState.EXT_REV_WRAP = false;
                case ALLOW_ALT_BUFFER -> terminal.currentPrivateModeState.ALLOW_ALT_BUFFER = false;
                case SWITCH_ALT_BUFFER -> {
                    terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = false;
                    int dirtyLinesMask = 0;
                    for (int j = 0; j < Terminal.HEIGHT; j++) {
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
                case SAVE_CURSOR -> {
                    terminal.currentPrivateModeState.SAVE_CURSOR = false;
                    if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                        terminal.x = terminal.altSavedX;
                        terminal.y = terminal.altSavedY;
                    } else {
                        terminal.x = terminal.savedX;
                        terminal.y = terminal.savedY;
                    }
                }
                case SAVE_CLEAR_AND_SWITCH -> {
                    terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = false;
                    if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                        terminal.x = terminal.altSavedX;
                        terminal.y = terminal.altSavedY;
                    } else {
                        terminal.x = terminal.savedX;
                        terminal.y = terminal.savedY;
                    }
                    int dirtyLinesMask = 0;
                    for (int j = 0; j < Terminal.HEIGHT; j++) {
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
                case SET_TERMINFO_FUNC_KEY_MODE -> terminal.currentPrivateModeState.SET_TERMINFO_FUNC_KEY_MODE = false;
                case SET_SUN_KEY_MODE -> terminal.currentPrivateModeState.SET_SUN_KEY_MODE = false;
                case SET_HP_K0EY_MODE -> terminal.currentPrivateModeState.SET_HP_K0EY_MODE = false;
                case SET_SCO_KEY_MODE -> terminal.currentPrivateModeState.SET_SCO_KEY_MODE = false;
                case SET_LEGACY_KEYBOARD -> terminal.currentPrivateModeState.SET_LEGACY_KEYBOARD = false;
                case SET_VT220_KEYBOARD -> terminal.currentPrivateModeState.SET_VT220_KEYBOARD = false;
                case ENABLE_READLINE_MOUSE_1 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_1 = false;
                case ENABLE_READLINE_MOUSE_2 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_2 = false;
                case ENABLE_READLINE_MOUSE_3 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_3 = false;
                case SET_BRACKETED_PASTE -> terminal.currentPrivateModeState.SET_BRACKETED_PASTE = false;
                case ENABLE_READLINE_CHAR_QUOTE -> terminal.currentPrivateModeState.ENABLE_READLINE_CHAR_QUOTE = false;
                case ENABLE_READLINE_NEWLINE_PASTE -> terminal.currentPrivateModeState.ENABLE_READLINE_NEWLINE_PASTE = false;
                case APPLICATION_SYNC -> terminal.currentPrivateModeState.APPLICATION_SYNC = false;
                case APPLICATION_ESC_MODE -> terminal.currentPrivateModeState.APPLICATION_ESC_MODE = false;
                default -> {}
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], false);
        }
    }

    private void handleRM(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case KAM -> terminal.currentModeState.KAM = false;
                case IRM -> terminal.currentModeState.IRM = false;
                case SRM -> terminal.currentModeState.SRM = false;
                case LNM -> terminal.currentModeState.LNM = false;
                default -> {}
            }
        }
    }
}
