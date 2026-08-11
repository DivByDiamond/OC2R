package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.oc2.common.vm.terminal.Terminal.State;
import li.cil.oc2.common.vm.terminal.escapes.*;

class TerminalOutput {

    private final ReentrantLock lock = new ReentrantLock();

    private final Terminal terminal;
    private final Utf8Decoder decoder = new Utf8Decoder();

    TerminalOutput(final Terminal terminal) {
        this.terminal = terminal;
    }

    public void putOutput(final ByteBuffer values) {
        lock.lock();
        try {

            while (values.hasRemaining()) {
                putOutput(values.get());
            }

        } finally {
            lock.unlock();
        }
    }

    public void putOutput(final byte value) {
        lock.lock();
        try {

            lock.lock();
            try {

                if (!decoder.process(value)) {
                    return;
                }
                final char ch = (char) value;
                switch (terminal.state) {
                    case NORMAL -> {
                        switch (value) {
                            case '\007' -> terminal.hasPendingBell = true;
                            case '\033' -> terminal.state = State.ESCAPE;
                            case '\016' -> terminal.useG0 = false;
                            case '\017' -> terminal.useG0 = true;

                            case (byte) '\r' -> terminal.setCursorPos(0, terminal.y);
                            case (byte) '\n', '\013', '\014' -> {
                                if (terminal.currentModeState.LNM) {
                                    NEL.execute(terminal);
                                } else {
                                    IND.execute(terminal);
                                }
                            }
                            case (byte) '\t' -> {
                                if (terminal.x < Terminal.WIDTH - 1) {
                                    do {
                                        terminal.x++;
                                    } while (terminal.x < Terminal.WIDTH - 1
                                            && (terminal.currentPrivateModeState
                                                            .isAltBufferEnabled()
                                                    ? !terminal.altTabs[terminal.x]
                                                    : !terminal.tabs[terminal.x]));
                                }
                            }
                            case (byte) '\b' ->
                                    terminal.setCursorPos(
                                            Math.min(terminal.x, Terminal.WIDTH - 1) - 1,
                                            terminal.y);

                            default -> {
                                terminal.bufferWriter.putChar(decoder.getCodepoint());
                                decoder.sequenceProcessed();
                            }
                        }
                    }
                    case ESCAPE -> {
                        if (ch == '[') {
                            terminal.csiManager.reset();
                            terminal.state = State.CONTROL_SEQUENCE;
                        } else if (ch == '(') {
                            terminal.state = State.SHIFT_IN_CHARACTER_SET;
                        } else if (ch == ')') {
                            terminal.state = State.SHIFT_OUT_CHARACTER_SET;
                        } else if (ch == '#') {
                            terminal.state = State.HASH;
                        } else if (ch == 'P') {
                            terminal.dcsManager.reset();
                            terminal.state = State.DCS;
                        } else if (ch == ']') {
                            terminal.oscManager.reset();
                            terminal.state = State.OSC;
                        } else if (ch == '_') {
                            terminal.apcManager.reset();
                            terminal.state = State.APC;
                        } else {
                            terminal.state = State.NORMAL;
                            switch (ch) {
                                case 'D' -> IND.execute(terminal);
                                case 'E' -> NEL.execute(terminal);
                                case 'M' -> RI.execute(terminal);
                                case '7' -> DECSC.execute(terminal);
                                case '8' -> DECRC.execute(terminal);
                                case 'H' -> HTS.execute(terminal);
                                case 'c' -> RIS.execute(terminal);
                                case '=' -> DECKPAM.execute(terminal);
                                case '>' -> DECKPNM.execute(terminal);
                                default -> System.out.println("Invalid escape: " + ch);
                            }
                        }
                    }
                    case CONTROL_SEQUENCE -> terminal.csiManager.handle(ch);
                    case SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET -> {
                        terminal.state = State.NORMAL;
                        switch (ch) {
                            case 'A' -> {}
                            case 'B' -> terminal.drawingModeG0 = TerminalColors.DrawingMode.ASCII;
                            case '0' ->
                                    terminal.drawingModeG0 =
                                            TerminalColors.DrawingMode.SPECIAL_GRAPHICS;
                            case '1' -> {}
                            case '2' -> {}
                            default -> {}
                        }
                    }
                    case HASH -> {
                        terminal.state = State.NORMAL;
                        if (ch == '8') {
                            if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                                Arrays.fill(terminal.altBuffer, 'E');
                            } else {
                                int startIndex =
                                        (terminal.lastRowToDisplayMax - Terminal.HEIGHT)
                                                * Terminal.WIDTH;
                                Arrays.fill(
                                        terminal.buffer,
                                        startIndex,
                                        startIndex + Terminal.WIDTH * Terminal.HEIGHT,
                                        'E');
                            }
                            terminal.renderers.forEach(model -> model.getDirtyMask().set(-1));
                        }
                    }
                    case DCS -> terminal.dcsManager.handle(ch);
                    case OSC -> terminal.oscManager.handle(ch);
                    case APC -> terminal.apcManager.handle(ch);
                }

            } finally {
                lock.unlock();
            }

        } finally {
            lock.unlock();
        }
    }
}
