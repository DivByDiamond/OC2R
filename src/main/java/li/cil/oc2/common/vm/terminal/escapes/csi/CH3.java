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
                resetMode(mode);
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], false);
        }
    }

    private void resetMode(final ModeTable mode) {
        switch (mode) {
            case DECCOLM -> resetDECCOLM();
            case DECOM -> {
                terminal.currentPrivateModeState.DECOM = false;
                terminal.setRelativeCursorPos(0, 0);
            }
            case DECSCNM -> {
                terminal.currentPrivateModeState.DECSCNM = false;
                markScreenDirty();
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
                restoreSavedCursor();
            }
            case SAVE_CLEAR_AND_SWITCH -> {
                terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = false;
                restoreSavedCursor();
                markScreenDirty();
            }
            default -> mode.set(terminal.currentPrivateModeState, false);
        }
    }

    private void resetDECCOLM() {
        terminal.currentPrivateModeState.DECCOLM = false;
        /* DECCOLM spec: clear screen and reset margins */
        terminal.bufferManager.clear();
        terminal.scrollFirst = 0;
        terminal.scrollLast = Terminal.HEIGHT - 1;
        terminal.setRelativeCursorPos(0, 0);
        markScreenDirty();
    }

    private void restoreSavedCursor() {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.x = terminal.altSavedX;
            terminal.y = terminal.altSavedY;
        } else {
            terminal.x = terminal.savedX;
            terminal.y = terminal.savedY;
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