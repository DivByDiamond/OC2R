package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;
import li.cil.oc2.common.vm.terminal.modes.impl.ImplementedPrivateModes;

public class CH2 extends CSISequenceHandler {
    private static final Map<ModeTable, Consumer<Terminal>> DECSET_ACTIONS = buildDecSetActions();

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
                final Consumer<Terminal> action = DECSET_ACTIONS.get(mode);
                if (action != null) {
                    action.accept(terminal);
                } else {
                    mode.set(terminal.currentPrivateModeState, true);
                }
            }

            ImplementedPrivateModes.instance.modeUsed(args[i], true);
        }
    }

    private static Map<ModeTable, Consumer<Terminal>> buildDecSetActions() {
        final Map<ModeTable, Consumer<Terminal>> actions = new EnumMap<>(ModeTable.class); // NOPMD immutable after init
        actions.put(ModeTable.DECCOLM, terminal -> {
            terminal.currentPrivateModeState.DECCOLM = true;
            /* DECCOLM spec: clear screen and reset margins */
            terminal.bufferManager.clear();
            terminal.scrollFirst = 0;
            terminal.scrollLast = Terminal.HEIGHT - 1;
            terminal.setRelativeCursorPos(0, 0);
            markScreenDirty(terminal);
        });
        actions.put(ModeTable.DECOM, terminal -> {
            terminal.currentPrivateModeState.DECOM = true;
            terminal.setRelativeCursorPos(0, 0);
        });
        actions.put(ModeTable.X10MM, terminal ->
                setMouseTracking(terminal, true, false, false, false));
        actions.put(ModeTable.START_BLINKING_CURSOR, terminal -> {
            terminal.cursorMode =
                    switch (terminal.cursorMode) {
                        case 2 -> 1;
                        case 4 -> 3;
                        case 6 -> 5;
                        default -> terminal.cursorMode;
                    };
            terminal.currentPrivateModeState.START_BLINKING_CURSOR = true;
        });
        actions.put(ModeTable.ALT_BUFFER, terminal -> {
            terminal.bufferManager.clearAlt();
            terminal.setCursorPos(0, 0);
            terminal.currentPrivateModeState.ALT_BUFFER = true;
            markScreenDirty(terminal);
        });
        actions.put(ModeTable.X11MM, terminal ->
                setMouseTracking(terminal, false, true, false, false));
        actions.put(ModeTable.CELL_MOTION_MOUSE, terminal ->
                setMouseTracking(terminal, false, false, true, false));
        actions.put(ModeTable.ALL_MOTION_MOUSE_TRACKING, terminal ->
                setMouseTracking(terminal, false, false, false, true));
        actions.put(ModeTable.UTF8_MOUSE, terminal ->
                setMouseEncoding(terminal, true, false, false, false));
        actions.put(ModeTable.SGR_MOUSE, terminal ->
                setMouseEncoding(terminal, false, true, false, false));
        actions.put(ModeTable.URXVT_MOUSE, terminal ->
                setMouseEncoding(terminal, false, false, true, false));
        actions.put(ModeTable.SGR_MOUSE_PIXEL, terminal ->
                setMouseEncoding(terminal, false, false, false, true));
        actions.put(ModeTable.SWITCH_ALT_BUFFER, terminal -> {
            terminal.bufferManager.clearAlt();
            terminal.setCursorPos(0, 0);
            terminal.currentPrivateModeState.SWITCH_ALT_BUFFER = true;
            markScreenDirty(terminal);
        });
        actions.put(ModeTable.SAVE_CURSOR, terminal -> {
            saveCursorPosition(terminal);
            terminal.currentPrivateModeState.SAVE_CURSOR = true;
        });
        actions.put(ModeTable.SAVE_CLEAR_AND_SWITCH, terminal -> {
            saveCursorPosition(terminal);
            terminal.bufferManager.clearAlt();
            terminal.setCursorPos(0, 0);
            terminal.currentPrivateModeState.SAVE_CLEAR_AND_SWITCH = true;
            markScreenDirty(terminal);
        });
        return actions;
    }

    private static void setMouseTracking(
            final Terminal terminal,
            final boolean x10,
            final boolean x11,
            final boolean cell,
            final boolean all) {
        terminal.currentPrivateModeState.X10MM = x10;
        terminal.currentPrivateModeState.X11MM = x11;
        terminal.currentPrivateModeState.CELL_MOTION_MOUSE = cell;
        terminal.currentPrivateModeState.ALL_MOTION_MOUSE_TRACKING = all;
    }

    private static void setMouseEncoding(
            final Terminal terminal,
            final boolean utf8,
            final boolean sgr,
            final boolean urxvt,
            final boolean pixel) {
        terminal.currentPrivateModeState.UTF8_MOUSE = utf8;
        terminal.currentPrivateModeState.SGR_MOUSE = sgr;
        terminal.currentPrivateModeState.URXVT_MOUSE = urxvt;
        terminal.currentPrivateModeState.SGR_MOUSE_PIXEL = pixel;
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

    private static void markScreenDirty(final Terminal terminal) {
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

    private static void saveCursorPosition(final Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.altSavedX = terminal.x;
            terminal.altSavedY = terminal.y;
        } else {
            terminal.savedX = terminal.x;
            terminal.savedY = terminal.y;
        }
    }
}