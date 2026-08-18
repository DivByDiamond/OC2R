package li.cil.oc2.common.vm.terminal.escapes.csi;

import static li.cil.oc2.common.vm.terminal.modes.Mode.*;
import static li.cil.oc2.common.vm.terminal.modes.PrivateMode.*;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.impl.ImplementedPrivateModes;

public class CH2 extends CSISequenceHandler {
    public CH2(final Terminal terminal) {
        super(terminal);
    }

    @Override
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
                case DECCKM -> terminal.currentPrivateModeState.DECCKM = true;
                case DECANM -> terminal.currentPrivateModeState.DECANM = true;
                case DECCOLM -> {
                    terminal.currentPrivateModeState.DECCOLM = true;
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
                case DECSCLM -> terminal.currentPrivateModeState.DECSCLM = true;
                case DECSCNM -> terminal.currentPrivateModeState.DECSCNM = true;
                case DECOM -> {
                    terminal.currentPrivateModeState.DECOM = true;
                    terminal.setRelativeCursorPos(0, 0);
                }
                case DECAWM -> terminal.currentPrivateModeState.DECAWM = true;
                case DECARM -> terminal.currentPrivateModeState.DECARM = true;
                case X10MM -> {
                    terminal.currentPrivateModeState.X11MM = false;
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                    terminal.currentPrivateModeState.X10MM = true;
                }
                case TOOLBAR -> terminal.currentPrivateModeState.TOOLBAR = true;
                case START_BLINKING_CURSOR -> {
                    terminal.cursorMode =
                            switch (terminal.cursorMode) {
                                case 2 -> 1;
                                case 4 -> 3;
                                case 6 -> 5;
                                default -> terminal.cursorMode;
                            };
                    terminal.currentPrivateModeState.START_BLINKING_CURSOR = true;
                }
                case START_BLINKING_CURSOR2 -> terminal.currentPrivateModeState.START_BLINKING_CURSOR2 = true;
                case XORBLINK -> terminal.currentPrivateModeState.XORBLINK = true;
                case DECPFF -> terminal.currentPrivateModeState.DECPFF = true;
                case DECPEX -> terminal.currentPrivateModeState.DECPEX = true;
                case DECTCEM -> terminal.currentPrivateModeState.DECTCEM = true;
                case SHOW_SCROLL -> terminal.currentPrivateModeState.SHOW_SCROLL = true;
                case FONT_SHIFT -> terminal.currentPrivateModeState.FONT_SHIFT = true;
                case TEKTRONIX -> terminal.currentPrivateModeState.TEKTRONIX = true;
                case ENABLE_80_132 -> terminal.currentPrivateModeState.ENABLE_80_132 = true;
                case MORE_FIX -> terminal.currentPrivateModeState.MORE_FIX = true;
                case DECNRCM -> terminal.currentPrivateModeState.DECNRCM = true;
                case DECGEPM -> terminal.currentPrivateModeState.DECGEPM = true;
                case MARG_BELL -> terminal.currentPrivateModeState.MARG_BELL = true;
                case XTREVWRAP -> terminal.currentPrivateModeState.XTREVWRAP = true;
                case XTLOGGING -> terminal.currentPrivateModeState.XTLOGGING = true;
                case ALT_BUFFER -> {
                    terminal.bufferManager.clearAlt();
                    terminal.setCursorPos(0, 0);
                    terminal.currentPrivateModeState.ALT_BUFFER = true;
                    markScreenDirty();
                }
                case DECNKM -> terminal.currentPrivateModeState.DECNKM = true;
                case DECBKM -> terminal.currentPrivateModeState.DECBKM = true;
                case DECLRMM -> terminal.currentPrivateModeState.DECLRMM = true;
                case DECSDM -> terminal.currentPrivateModeState.DECSDM = true;
                case DECNCSM -> terminal.currentPrivateModeState.DECNCSM = true;
                case X11MM -> {
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                    terminal.currentPrivateModeState.X10MM = false;
                    terminal.currentPrivateModeState.X11MM = true;
                }
                case HILITE_MOUSE -> terminal.currentPrivateModeState.HILITE_MOUSE = true;
                case CELL_MOTION_MOUSE -> {
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                    terminal.currentPrivateModeState.X10MM = false;
                    terminal.currentPrivateModeState.X11MM = false;
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = true;
                }
                case ALL_MOTION_MOUSE_TRACKING -> {
                    terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                    terminal.currentPrivateModeState.X10MM = false;
                    terminal.currentPrivateModeState.X11MM = false;
                    terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = true;
                }
                case FOCUS_IN_FOCUS_OUT -> terminal.currentPrivateModeState.FOCUS_IN_FOCUS_OUT = true;
                case UTF8_MOUSE -> {
                    terminal.currentPrivateModeState.SGR_MOUSE = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                    terminal.currentPrivateModeState.UTF8_MOUSE = true;
                }
                case SGR_MOUSE -> {
                    terminal.currentPrivateModeState.UTF8_MOUSE = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                    terminal.currentPrivateModeState.SGR_MOUSE = true;
                }
                case ALTERNATE_SCROLL_MODE -> terminal.currentPrivateModeState.ALTERNATE_SCROLL_MODE = true;
                case SCROLL_BOTTOM_ON_OUTPUT -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_OUTPUT = true;
                case SCROLL_BOTTOM_ON_KEY_PRESS -> terminal.currentPrivateModeState.SCROLL_BOTTOM_ON_KEY_PRESS = true;
                case FAST_SCROLL -> terminal.currentPrivateModeState.FAST_SCROLL = true;
                case URXVT_MOUSE -> {
                    terminal.currentPrivateModeState.UTF8_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = true;
                }
                case SGR_MOUSE_PIXEL -> {
                    terminal.currentPrivateModeState.UTF8_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE = false;
                    terminal.currentPrivateModeState.URXVT_MOUSE = false;
                    terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = true;
                }
                case META_KEY -> terminal.currentPrivateModeState.META_KEY = true;
                case SPECIAL_MODIFIERS -> terminal.currentPrivateModeState.SPECIAL_MODIFIERS = true;
                case META_SENDS_ESCAPE -> terminal.currentPrivateModeState.META_SENDS_ESCAPE = true;
                case DEL_EDIT_KEYPAD_DEL -> terminal.currentPrivateModeState.DEL_EDIT_KEYPAD_DEL = true;
                case ALT_SENDS_ESC -> terminal.currentPrivateModeState.ALT_SENDS_ESC = true;
                case KEEP_SELECTION -> terminal.currentPrivateModeState.KEEP_SELECTION = true;
                case USE_CLIP -> terminal.currentPrivateModeState.USE_CLIP = true;
                case ENABLE_URGENCY -> terminal.currentPrivateModeState.ENABLE_URGENCY = true;
                case RAISE_ON_CTRL_G -> terminal.currentPrivateModeState.RAISE_ON_CTRL_G = true;
                case KEEP_CLIP -> terminal.currentPrivateModeState.KEEP_CLIP = true;
                case EXT_REV_WRAP -> terminal.currentPrivateModeState.EXT_REV_WRAP = true;
                case ALLOW_ALT_BUFFER -> terminal.currentPrivateModeState.ALLOW_ALT_BUFFER = true;
                case SWITCH_ALT_BUFFER -> {
                    terminal.bufferManager.clearAlt();
                    terminal.setCursorPos(0, 0);
                    terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = true;
                    markScreenDirty();
                }
                case SAVE_CURSOR -> {
                    saveCursorPosition();
                    terminal.currentPrivateModeState.SAVE_CURSOR = true;
                }
                case SAVE_CLEAR_AND_SWITCH -> {
                    saveCursorPosition();
                    terminal.bufferManager.clearAlt();
                    terminal.setCursorPos(0, 0);
                    terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = true;
                    markScreenDirty();
                }
                case SET_TERMINFO_FUNC_KEY_MODE -> terminal.currentPrivateModeState.SET_TERMINFO_FUNC_KEY_MODE = true;
                case SET_SUN_KEY_MODE -> terminal.currentPrivateModeState.SET_SUN_KEY_MODE = true;
                case SET_HP_K0EY_MODE -> terminal.currentPrivateModeState.SET_HP_K0EY_MODE = true;
                case SET_SCO_KEY_MODE -> terminal.currentPrivateModeState.SET_SCO_KEY_MODE = true;
                case SET_LEGACY_KEYBOARD -> terminal.currentPrivateModeState.SET_LEGACY_KEYBOARD = true;
                case SET_VT220_KEYBOARD -> terminal.currentPrivateModeState.SET_VT220_KEYBOARD = true;
                case ENABLE_READLINE_MOUSE_1 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_1 = true;
                case ENABLE_READLINE_MOUSE_2 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_2 = true;
                case ENABLE_READLINE_MOUSE_3 -> terminal.currentPrivateModeState.ENABLE_READLINE_MOUSE_3 = true;
                case SET_BRACKETED_PASTE -> terminal.currentPrivateModeState.SET_BRACKETED_PASTE = true;
                case ENABLE_READLINE_CHAR_QUOTE -> terminal.currentPrivateModeState.ENABLE_READLINE_CHAR_QUOTE = true;
                case ENABLE_READLINE_NEWLINE_PASTE -> terminal.currentPrivateModeState.ENABLE_READLINE_NEWLINE_PASTE = true;
                case APPLICATION_SYNC -> terminal.currentPrivateModeState.APPLICATION_SYNC = true;
                case APPLICATION_ESC_MODE -> terminal.currentPrivateModeState.APPLICATION_ESC_MODE = true;
                default -> {}
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], true);
        }
    }

    private void handleSM(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            switch (args[i]) {
                case KAM -> terminal.currentModeState.KAM = true;
                case IRM -> terminal.currentModeState.IRM = true;
                case SRM -> terminal.currentModeState.SRM = true;
                case LNM -> terminal.currentModeState.LNM = true;
                default -> {}
            }
        }
    }

    private void markScreenDirty() {
        int dirtyLinesMask = 0;
        for (int j = 0; j < Terminal.HEIGHT; j++) {
            dirtyLinesMask |= 1 << j;
        }
        final int finalDirtyLinesMask = dirtyLinesMask;
        terminal.renderers.forEach(
                model ->
                        model.getDirtyMask()
                                .accumulateAndGet(
                                        finalDirtyLinesMask, (left, right) -> left | right));
    }

    private void saveCursorPosition() {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.altSavedX = terminal.x;
            terminal.altSavedY = terminal.y;
        } else {
            terminal.savedX = terminal.x;
            terminal.savedY = terminal.y;
        }
    }
}
