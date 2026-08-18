package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;
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
            final ModeTable mode = ModeTable.forPrivateMode(args[i]);
            if (mode != null) {
                switch (mode) {
                    case DECCOLM -> {
                        terminal.currentPrivateModeState.DECCOLM = true;
                        /* DECCOLM spec: clear screen and reset margins */
                        terminal.bufferManager.clear();
                        terminal.scrollFirst = 0;
                        terminal.scrollLast = Terminal.HEIGHT - 1;
                        terminal.setRelativeCursorPos(0, 0);
                        markScreenDirty();
                    }
                    case DECOM -> {
                        terminal.currentPrivateModeState.DECOM = true;
                        terminal.setRelativeCursorPos(0, 0);
                    }
                    case X10MM -> {
                        terminal.currentPrivateModeState.X11MM = false;
                        terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                        terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                        terminal.currentPrivateModeState.X10MM = true;
                    }
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
                    case ALT_BUFFER -> {
                        terminal.bufferManager.clearAlt();
                        terminal.setCursorPos(0, 0);
                        terminal.currentPrivateModeState.ALT_BUFFER = true;
                        markScreenDirty();
                    }
                    case X11MM -> {
                        terminal.currentPrivateModeState.CELL_MOTION_MOUSE = false;
                        terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = false;
                        terminal.currentPrivateModeState.X10MM = false;
                        terminal.currentPrivateModeState.X11MM = true;
                    }
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
                    default -> mode.set(terminal.currentPrivateModeState, true);
                }
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], true);
        }
    }

    private void handleSM(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            final ModeTable mode = ModeTable.forAnsiMode(args[i]);
            if (mode != null) {
                switch (mode) {
                    case KAM -> terminal.currentModeState.KAM = true;
                    case IRM -> terminal.currentModeState.IRM = true;
                    case SRM -> terminal.currentModeState.SRM = true;
                    case LNM -> terminal.currentModeState.LNM = true;
                    default -> {}
                }
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