package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Nullable;
import li.cil.oc2.common.vm.terminal.Terminal.State;
import li.cil.oc2.common.vm.terminal.escapes.*;

class TerminalIO {
    private final Terminal terminal;
    private final Utf8Decoder decoder = new Utf8Decoder();

    TerminalIO(final Terminal terminal) {
        this.terminal = terminal;
    }

    public int readInput() {
        if (terminal.input.isEmpty()) {
            return -1;
        } else {
            return terminal.input.dequeueByte() & 0xFF;
        }
    }

    @Nullable
    public ByteBuffer getInput() {
        if (terminal.input.isEmpty()) {
            return null;
        } else {
            if (!terminal.currentPrivateModeState.isAltBufferEnabled())
                terminal.lastRowToDisplay = terminal.lastRowToDisplayMax;
            int dirtyLinesMask = 0;
            for (int i = 0; i <= 23; i++) {
                dirtyLinesMask |= 1 << i;
            }
            final int finalDirtyLinesMask = dirtyLinesMask;
            terminal.renderers.forEach(
                    model ->
                            model.getDirtyMask()
                                    .accumulateAndGet(
                                            finalDirtyLinesMask, (left, right) -> left | right));
            final ByteBuffer buffer = ByteBuffer.allocate(terminal.input.size());
            while (!terminal.input.isEmpty()) {
                buffer.put(terminal.input.dequeueByte());
            }
            buffer.flip();
            return buffer;
        }
    }

    public void putInput(final String value) {
        putInput(ByteBuffer.wrap(value.getBytes()));
    }

    public void putInput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            terminal.input.enqueue(values.get());
        }
    }

    public void putOutput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            putOutput(values.get());
        }
    }

    public void putOutput(final byte value) {
        synchronized (terminal.buffer) {
            synchronized (terminal.altBuffer) {
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
                                if (terminal.x < Terminal.WIDTH) {
                                    do {
                                        terminal.x++;
                                    } while (terminal.x < Terminal.WIDTH
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
                                terminal.bufferManager.putChar(decoder.getCodepoint());
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
                                case '=' -> {}
                                case '>' -> {}
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
                        }
                    }
                    case HASH -> {
                        terminal.state = State.NORMAL;
                        switch (ch) {
                            case '3', '4', '5', '6' -> {}
                            case '8' -> {
                                if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                                    Arrays.fill(terminal.altBuffer, 'E');
                                } else {
                                    Arrays.fill(
                                            terminal.buffer,
                                            (terminal.lastRowToDisplayMax - Terminal.HEIGHT)
                                                    * Terminal.WIDTH,
                                            ((Terminal.WIDTH - 1)
                                                            + (Terminal.HEIGHT - 1)
                                                                    * Terminal.WIDTH)
                                                    + 1,
                                            'E');
                                }
                                terminal.renderers.forEach(model -> model.getDirtyMask().set(-1));
                            }
                        }
                    }
                    case DCS -> terminal.dcsManager.handle(ch);
                    case OSC -> terminal.oscManager.handle(ch);
                    case APC -> terminal.apcManager.handle(ch);
                }
            }
        }
    }

    public void putInput(final char value) {
        putInput((byte) value);
    }

    public void putInput(final byte value) {
        terminal.input.enqueue(value);
    }

    public void putResponse(final String value) {
        for (int i = 0; i < value.length(); i++) {
            putResponse((byte) value.charAt(i));
        }
    }

    public void putResponse(final byte value) {
        if (!terminal.displayOnly) {
            putInput(value);
        }
    }
}