package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;
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
            final ModeTable mode = ModeTable.forPrivateMode(args[i]);
            if (mode != null) {
                switch (mode) {
                    case DECCOLM -> {
                        terminal.currentPrivateModeState.DECCOLM = false;
                        /* DECCOLM spec: clear screen and reset margins */
                        terminal.bufferManager.clear();
                        terminal.scrollFirst = 0;
                        terminal.scrollLast = Terminal.HEIGHT - 1;
                        terminal.setRelativeCursorPos(0, 0);
                        markScreenDirty();
                    }
                    case DECOM -> {
                        terminal.currentPrivateModeState.DECOM = false;
                        terminal.setRelativeCursorPos(0, 0);
                    }
                    case ALT_BUFFER -> {
                        terminal.currentPrivateModeState.ALT_BUFFER = false;
                        markScreenDirty();
                    }
                    case SWITCH_ALT_BUFFER -> {
                        terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = false;
                        markScreenDirty();
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
                        markScreenDirty();
                    }
                    default -> mode.set(terminal.currentPrivateModeState, false);
                }
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], false);
        }
    }

    private void handleRM(int[] args, int argCount) {
        for (int i = 0; i < argCount; i++) {
            final ModeTable mode = ModeTable.forAnsiMode(args[i]);
            if (mode != null) {
                switch (mode) {
                    case KAM -> terminal.currentModeState.KAM = false;
                    case IRM -> terminal.currentModeState.IRM = false;
                    case SRM -> terminal.currentModeState.SRM = false;
                    case LNM -> terminal.currentModeState.LNM = false;
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
}